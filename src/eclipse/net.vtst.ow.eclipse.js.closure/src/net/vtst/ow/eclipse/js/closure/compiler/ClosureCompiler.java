package net.vtst.ow.eclipse.js.closure.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.vtst.eclipse.easy.ui.properties.stores.IReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.IStore;
import net.vtst.eclipse.easy.ui.properties.stores.PluginPreferenceStore;
import net.vtst.eclipse.easy.ui.properties.stores.ProjectPropertyStore;
import net.vtst.ow.closure.compiler.deps.AbstractJSProject;
import net.vtst.ow.closure.compiler.util.ListWithoutDuplicates;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;
import net.vtst.ow.eclipse.js.closure.builder.ClosureNature;
import net.vtst.ow.eclipse.js.closure.preferences.ClosurePreferenceRecord;
import net.vtst.ow.eclipse.js.closure.properties.ClosureProjectPropertyRecord;
import net.vtst.ow.eclipse.js.closure.util.Utils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;

import com.google.javascript.jscomp.AbstractCompiler;

/**
 * This class implements static methods which are useful for using the Closure Compiler.
 * @author Vincent Simonet
 */
public class ClosureCompiler {

  private static final String JS_CONTENT_TYPE_ID =
      "org.eclipse.wst.jsdt.core.jsSource";

  private static final IContentType jsContentType =
      Platform.getContentTypeManager().getContentType(JS_CONTENT_TYPE_ID);

  /**
   * Test whether a file is a JavaScript file (by looking at its content type).
   * @param file  The file to test.
   * @return  true iif the given file is a JavaScript file.
   * @throws CoreException
   */
  public static boolean isJavaScriptFile(IFile file) throws CoreException {
    IContentDescription contentDescription = file.getContentDescription();
    if (contentDescription == null) return false;
    IContentType contentType = contentDescription.getContentType();
    return contentType.isKindOf(jsContentType);
  }
  
  /**
   * Gets the list of JavaScript files of a resource (including itself).
   * @param resource  The resource to visit.
   * @return  The list of JavaScript files.  May be empty, but never null.
   * @throws CoreException 
   */
  public static Set<IFile> getJavaScriptFiles(IResource resource) throws CoreException {
    final Set<IFile> files = new HashSet<IFile>();
    IResourceVisitor visitor = new IResourceVisitor() {
      @Override
      public boolean visit(IResource resource) throws CoreException {
        if (resource instanceof IFile) {
          IFile file = (IFile) resource;
          if (ClosureCompiler.isJavaScriptFile(file)) files.add(file);
        }
        return true;
      }
    };
    resource.accept(visitor);
    return files;
  }

  /**
   * Gets the list of JavaScript files of a set of resources (including themselves).
   * @param resources  The resources to visit.
   * @return  The list of JavaScript files.  May be empty, but never null.
   * @throws CoreException 
   */
  public static Set<IFile> getJavaScriptFiles(Iterable<? extends IResource> resources) throws CoreException {
    final Set<IFile> files = new HashSet<IFile>();
    IResourceVisitor visitor = new IResourceVisitor() {
      @Override
      public boolean visit(IResource resource) throws CoreException {
        if (resource instanceof IFile) {
          IFile file = (IFile) resource;
          if (ClosureCompiler.isJavaScriptFile(file)) files.add(file);
        }
        return true;
      }
    };
    for (IResource resource: resources) resource.accept(visitor);
    return files;
  }

  /**
   * Get the closure base path for a project (from its properties and the global preferences).
   * @param project
   * @return  The closure base path for the project.
   * @throws CoreException
   */
  public static File getPathOfClosureBase(IProject project) throws CoreException {
    return getPathOfClosureBase(new ProjectPropertyStore(project, OwJsClosurePlugin.PLUGIN_ID));
  }

  /**
   * Get the closure base path for a store.
   * @throws CoreException
   */
  public static File getPathOfClosureBase(IReadOnlyStore store) throws CoreException {
    ClosureProjectPropertyRecord pr = ClosureProjectPropertyRecord.getInstance();
    if (pr.includes.useDefaultClosureBasePath.get(store)) {
      IStore prefs = new PluginPreferenceStore(OwJsClosurePlugin.getDefault().getPreferenceStore());
      return ClosurePreferenceRecord.getInstance().closureBasePath.get(prefs);
    } else {
      return pr.includes.closureBasePath.get(store);
    }
  }

  /**
   * Get the list of projects which are transitively referenced from a collection of root projects.
   * @param projects  The root projects.
   * @return  The list of projects, including the root projects and their recursive references.
   * @throws CoreException
   */
  public static ArrayList<IProject> getReferencedJavaScriptProjectsRecursively(
      Collection<IProject> projects) throws CoreException {
    // Compute the transitive set of referenced projects.
    ListWithoutDuplicates<IProject> result = new ListWithoutDuplicates<IProject>();
    LinkedList<IProject> projectsToVisit = new LinkedList<IProject>();
    projectsToVisit.addAll(projects);
    result.addAll(projects);
    while (!projectsToVisit.isEmpty()) {
      IProject visitedProject = projectsToVisit.remove();
      for (IProject referencedProject: visitedProject.getReferencedProjects()) {
        if (referencedProject.hasNature(ClosureNature.NATURE_ID)) {
          if (result.add(referencedProject)) projectsToVisit.add(referencedProject);
        }
      }
    }
    // Sort the set of referenced projects by dependency order.
    return result.asList();
  }

  /**
   * Get the list of projects which are transitively referenced from a collection of root projects.
   * @param projects  The root projects.
   * @param comparator  A comparator to order projects, or {@code null}.
   * @return  The list of projects, including the root projects and their recursive references,
   *   ordered according to the comparator.
   * @throws CoreException
   */
  public static ArrayList<IProject> getReferencedJavaScriptProjectsRecursively(
      Collection<IProject> projects, Comparator<IProject> comparator) throws CoreException {
    ArrayList<IProject> referencedProjects = getReferencedJavaScriptProjectsRecursively(projects);
    Collections.sort(referencedProjects, comparator);
    return referencedProjects;
  }
  
  
  private static void addJSLibraries(
      IJSLibraryProvider provider, AbstractCompiler compiler, IProgressMonitor monitor,
      IReadOnlyStore store, ListWithoutDuplicates<AbstractJSProject> result) throws CoreException {
    File pathOfClosureBase = getPathOfClosureBase(store);
    if (pathOfClosureBase != null) {
      result.add(provider.get(compiler, pathOfClosureBase, pathOfClosureBase));
    }
    for (File libraryPath: ClosureProjectPropertyRecord.getInstance().includes.otherLibraries.get(store)) {
      if (monitor != null) Utils.checkCancel(monitor);
      result.add(provider.get(compiler, libraryPath, pathOfClosureBase));
    }
  }

  /**
   * Returns the list of libraries which are imported from a given list of projects.
   * @param provider  The library provider.
   * @param compiler  The compiler to which errors are reported.
   * @param monitor  A progress monitor, which is checked for cancellation (may be {@code null}).
   * @param projects  The projects to scan.
   * @return The list of libraries, in the order of the projects which require them.  If the
   *   same library is required by several projects, the last wins.
   * @throws CoreException
   */
  public static List<AbstractJSProject> getJSLibraries(
      IJSLibraryProvider provider, AbstractCompiler compiler, IProgressMonitor monitor,
      ArrayList<IProject> projects) throws CoreException {
    ListWithoutDuplicates<AbstractJSProject> result = new ListWithoutDuplicates<AbstractJSProject>();
    for (int i = projects.size() - 1; i >= 0; --i) {
      addJSLibraries(
          provider, compiler, monitor, 
          new ProjectPropertyStore(projects.get(i), OwJsClosurePlugin.PLUGIN_ID), result);
    }
    return result.asList();
  }
  
  public static List<AbstractJSProject> getJSLibraries(IJSLibraryProvider provider, AbstractCompiler compiler, IProgressMonitor monitor,
      IReadOnlyStore store) throws CoreException {
    ListWithoutDuplicates<AbstractJSProject> result = new ListWithoutDuplicates<AbstractJSProject>();
    addJSLibraries(provider, compiler, monitor, store, result);
    return result.asList();
  }

  
  /**
   * Returns the common project of a set of resources.
   * @param resources  The resources.
   * @return  The project to which these resources belong to, or {@code null} if all resources do not belong
   *   to the same project.
   */
  public static IProject getCommonProject(Iterable<IResource> resources) {
    IProject project = null;
    for (IResource resource: resources) {
      if (project == null) {
        project = resource.getProject();
      } else {
        if (!project.equals(resource.getProject())) return null;
      }
    }
    return project;
  }
}
