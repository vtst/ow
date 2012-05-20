package net.vtst.ow.eclipse.js.closure.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import net.vtst.eclipse.easy.ui.properties.stores.IReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.IStore;
import net.vtst.eclipse.easy.ui.properties.stores.PluginPreferenceStore;
import net.vtst.eclipse.easy.ui.properties.stores.ResourcePropertyStore;
import net.vtst.ow.closure.compiler.util.ListWithoutDuplicates;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;
import net.vtst.ow.eclipse.js.closure.builder.ClosureNature;
import net.vtst.ow.eclipse.js.closure.preferences.ClosurePreferenceRecord;
import net.vtst.ow.eclipse.js.closure.properties.file.ClosureFilePropertyRecord;
import net.vtst.ow.eclipse.js.closure.properties.project.ClosureProjectPropertyRecord;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.core.ClasspathEntry;
import org.eclipse.wst.jsdt.internal.core.JavaProject;
import org.eclipse.wst.jsdt.internal.core.util.Util;

/**
 * This class implements static methods which are useful for using the Closure Compiler.
 * @author Vincent Simonet
 */
@SuppressWarnings("restriction")
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
    if (!contentType.isKindOf(jsContentType)) return false;
    if (ClosureFilePropertyRecord.getInstance().generatedByCompiler.get(new ResourcePropertyStore(file, OwJsClosurePlugin.PLUGIN_ID)))
      return false;
    return true;
  }
  
  // Inspired from org.eclipse.wst.jsdt.internal.core.builder.AbstractImageBuilder.isExcludedFromProject
  private static boolean isExcludedFromProject(JavaProject javaProject, IIncludePathEntry[] expandedClassPath, IPath containerPath) throws JavaScriptModelException {
    // answer whether the folder should be ignored when walking the project as a source folder
    if (containerPath.segmentCount() > 2) return false; // is a subfolder of a package
    for (IIncludePathEntry includePathEntry: expandedClassPath) {
      if (!(includePathEntry instanceof ClasspathEntry)) continue;
      final ClasspathEntry entry = (ClasspathEntry) includePathEntry;
      if (containerPath.equals(entry.getOutputLocation())) return true;
    }
    // skip default output folder which may not be used by any source folder
    return containerPath.equals(javaProject.getOutputLocation());
  }
  
  /**
   * Get the JavaScript files from a project, according to the class path entries of the project.
   * @param project
   * @return  The set of JavaScript files, may be empty but not null.
   * @throws CoreException
   */
  // Inspired from org.eclipse.wst.jsdt.internal.core.builder.AbstractImageBuilder.addAllSourceFiles
  public static Set<IFile> getJavaScriptFilesOfProject(IProject project) throws CoreException {
    final JavaProject javaProject = (JavaProject) JavaScriptCore.create(project);
    final Set<IFile> result = new HashSet<IFile>();
    try {
      final IIncludePathEntry[] expandedClassPath = javaProject.getExpandedClasspath();
      for (IIncludePathEntry includePathEntry: expandedClassPath) {
        if (!(includePathEntry instanceof ClasspathEntry)) continue;
        final ClasspathEntry entry = (ClasspathEntry) includePathEntry;
        int entryKind = entry.getEntryKind();
        if (entryKind != IIncludePathEntry.CPE_SOURCE && entryKind != IIncludePathEntry.CPE_LIBRARY) continue;
        IResource includeResource = ResourcesPlugin.getWorkspace().getRoot().findMember(entry.getPath());
        if (includeResource == null) continue;
        switch (entryKind) {
        case IIncludePathEntry.CPE_SOURCE:
          includeResource.accept(new IResourceVisitor() {
            public boolean visit(IResource resource) throws CoreException {
              if (resource instanceof IFile) {
                IFile file = (IFile) resource;
                if (ClosureCompiler.isJavaScriptFile(file) &&
                    !Util.isExcluded(file.getFullPath(), entry.fullInclusionPatternChars(), entry.fullExclusionPatternChars(), false)) {
                  result.add(file);
                }
                return false;
              } else if (resource instanceof IContainer) {
                IContainer container = (IContainer) resource;
                if (isExcludedFromProject(javaProject, expandedClassPath, container.getFullPath())) return false;
                return (
                    !Util.isExcluded(container.getFullPath(), entry.fullInclusionPatternChars(), entry.fullExclusionPatternChars(), false) ||
                    entry.fullInclusionPatternChars() != null);
              } else return false;
            }});
          break;
        case IIncludePathEntry.CPE_LIBRARY:
          if (includeResource instanceof IFile && project.equals(includeResource.getProject())) {
            result.add((IFile) includeResource);
          }
          break;
        }
      }
      return result;
    } catch (JavaScriptModelException e) {
      e.printStackTrace();
      return Collections.emptySet();
    }
  }
  
  /**
   * Same as <code>getJavaScriptFilesOfProject</code>, for a series of projects.
   * @param projects
   * @return The JavaScript files included in the series of projects (as defined in the class path).
   * @throws CoreException
   */
  public static Set<IFile> getJavaScriptFilesOfProjects(Iterable<IProject> projects) throws CoreException {
    Set<IFile> result = new HashSet<IFile>();
    for (IProject project: projects) {
      result.addAll(getJavaScriptFilesOfProject(project));
    }
    return result;
  }
  
  /**
   * Fallback function to get the JavaScript files of a resource which is not a project (i.e. a folder
   * or a single file).
   * @param resource  The resource to visit.
   * @return  The set of JavaScript files included in the resource.
   * @throws CoreException
   */
  private static Set<IFile> getJavaScriptFilesOfOtherResource(IResource resource) throws CoreException {
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
   * Gets the list of JavaScript files of a resource (including itself).
   * @param resource  The resource to visit.
   * @return  The list of JavaScript files.  May be empty, but never null.
   * @throws CoreException 
   */
  public static Set<IFile> getJavaScriptFiles(IResource resource) throws CoreException {
    if (resource instanceof IProject) {
      return getJavaScriptFilesOfProject((IProject) resource);
    } else {
      return getJavaScriptFilesOfOtherResource(resource);
    }
  }

  /**
   * Gets the list of JavaScript files of a set of resources (including themselves).
   * @param resources  The resources to visit.
   * @return  The list of JavaScript files.  May be empty, but never null.
   * @throws CoreException 
   */
  public static Set<IFile> getJavaScriptFiles(Iterable<? extends IResource> resources) throws CoreException {
    final Set<IFile> result = new HashSet<IFile>();
    for (IResource resource: resources) {
      result.addAll(getJavaScriptFiles(resource));
    }
    return result;
  }

  /**
   * Get the closure base path for a project (from its properties and the global preferences).
   * @param project
   * @return  The closure base path for the project.
   * @throws CoreException
   */
  public static File getPathOfClosureBase(IProject project) throws CoreException {
    return getPathOfClosureBase(new ResourcePropertyStore(project, OwJsClosurePlugin.PLUGIN_ID));
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
        if (referencedProject.isOpen() && referencedProject.hasNature(ClosureNature.NATURE_ID)) {
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
  
  /**
   * Convert a source name (as returned by the closure compiler in an Error) into an Eclipse IFile.
   * @param sourceName  The source name to convert.
   * @return  The corresponding file, or null if not found in the workspace.
   */
  public static IFile getFileFromSourceName(String sourceName) {
    if (sourceName == null) return null;
    IFile[] errorFiles = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI((new File(sourceName)).toURI());
    if (errorFiles.length > 0) {
      return errorFiles[0];
    } else {
      return null;
    }
  }
}
