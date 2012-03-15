package net.vtst.ow.eclipse.js.closure.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.vtst.ow.closure.compiler.compile.CompilableJSUnit;
import net.vtst.ow.closure.compiler.deps.JSSet;
import net.vtst.ow.closure.compiler.deps.JSUnit;
import net.vtst.ow.closure.compiler.util.CompilerUtils;
import net.vtst.ow.eclipse.js.closure.builder.ClosureNature;
import net.vtst.ow.eclipse.js.closure.properties.ClosureProjectPersistentPropertyHelper;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.ui.IWorkbench;

import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.DefaultPassConfig;
import com.google.javascript.jscomp.ErrorManager;
import com.google.javascript.jscomp.JSModule;
import com.google.javascript.jscomp.JSSourceFile;
import com.google.javascript.jscomp.PassConfig;

/**
 * A registry of JavaScript editors, which is updated thanks to listener.
 * Thread safe implementation.
 * @author Vincent Simonet
 */
public class JavaScriptProjectRegistry {
  
  // Note: the builder is automatically invoked at startup and when the project nature
  // is put on a project.
  
  // TODO: Manage project references.
  // TODO: Add project libraries.
  // TODO: How to manage errors in the update?
  
  private static final String JS_CONTENT_TYPE_ID =
      "org.eclipse.wst.jsdt.core.jsSource";

  // **************************************************************************
  // Constructor and dispose

  private final IContentType jsContentType =
      Platform.getContentTypeManager().getContentType(JS_CONTENT_TYPE_ID);

  // Take a concurrent hash map, as it may be accessed in parallel from several threads.
  private Map<IProject, JSSet<IFile>> projectToCompilationSet = 
      new ConcurrentHashMap<IProject, JSSet<IFile>>();
  
  // This is a weak hash map, because libraries which are no longer used by any project
  // should be collected.
  // TODO: Should the libraries be identified by a pair of files?
  // private WeakHashMap<File, Library> pathToLibrary = new WeakHashMap<File, Library>();

  public JavaScriptProjectRegistry(IWorkbench workbench) {
    ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener);
  }


  public void dispose() {
    projectToCompilationSet.clear();
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
  }

  // **************************************************************************
  // Update

  /**
   * Update from scratch the compilation set for a project.
   * @param project
   */
  public void fullUpdate(IProject project) throws CoreException {
    // Get the list of files
    ResourceVisitorForFullUpdate visitor = new ResourceVisitorForFullUpdate();
    project.accept(visitor);
    List<IFile> jsFiles = visitor.getFiles();
    // Build the compilation set
    JSSet<IFile> compilationSet = new JSSet<IFile>();
    // Add the compilation units for the libraries
    ClosureProjectPersistentPropertyHelper helper = new ClosureProjectPersistentPropertyHelper(project);
    for (String libraryPath: helper.getOtherLibraries()) {
      System.out.println("Library: " + libraryPath);
    }
    File temporary = new File("/home/vtst/test/in/closure/goog");
    ErrorManager errorManager = CompilerUtils.makePrintingErrorManager(System.out);  // TODO
    // Add the compilation units for the referenced projects
    // TODO: Be careful to avoid loops!
    for (IProject referencedProject: project.getReferencedProjects()) {
      if (referencedProject.hasNature(ClosureNature.NATURE_ID)) {
        System.out.println("Referenced project: " + project.getName());
      }
    }
    // Add the files of the current project.
    for (IFile file: jsFiles) {
      compilationSet.addCompilationUnit(file,
          new CompilableJSUnit(
              errorManager,
              compilationSet, file.getFullPath().toFile(), temporary,
              new CompilationUnitProviderFromEclipseIFile(file)));
    }
    projectToCompilationSet.put(project, compilationSet);
  }
  
  /**
   * Update incrementally the compilation set for a project.
   * @param project
   * @param delta
   */
  public void incrementalUpdate(IProject project, IResourceDelta delta) throws CoreException {
    if (!projectToCompilationSet.containsKey(project) ||
        shallRebuild(project, delta)) fullUpdate(project);
  }
  
  /**
   * Determine whether an update of a project requires to re-generate the compilation set.
   * @param project
   * @param delta
   * @return  true if the compilation set has to be re-generated.
   * @throws CoreException
   */
  private boolean shallRebuild(IProject project, IResourceDelta delta) throws CoreException {
    ResourceDeltaVisitorForIncrementalUpdate visitor = 
        new ResourceDeltaVisitorForIncrementalUpdate(getFilesOfProject(project));
    delta.accept(visitor);
    return visitor.shallRebuild();
  }

  /**
   * Get the files which were associated with a project.  This is useful to detemined
   * if removed files are JavaScript ones.
   * @param project
   * @return the list of JavaScript files associated with the project.
   */
  private Collection<IFile> getFilesOfProject(IProject project) {
    JSSet<IFile> compilationSet = projectToCompilationSet.get(project);
    if (compilationSet == null) return Collections.emptySet();
    return compilationSet.keySet();
  }
  
  public void remove(IProject project) {
    projectToCompilationSet.remove(project);
  }
  
  // **************************************************************************
  // Access to the registry
  
  /**
   * Get the compilation set for a project.
   * @param project  The project to look for.
   * @return  The compilation set for that project, or null if the project is not in the registry.
   */
  public JSSet<IFile> getCompilationSet(IProject project) {
    return projectToCompilationSet.get(project);
  }
  
  public void compile(IFile file) {
    JSSet<IFile> compilationSet = projectToCompilationSet.get(file.getProject());
    if (compilationSet == null) return;
    JSUnit compilationUnit = compilationSet.getCompilationUnit(file);
    if (compilationUnit == null) return;
    Compiler compiler = CompilerUtils.makeCompiler(CompilerUtils.makePrintingErrorManager(System.out));
    CompilerOptions options = CompilerUtils.makeOptions();
    options.checkTypes = true;
    compiler.initOptions(options);
    PassConfig passes = new DefaultPassConfig(options);
    compiler.setPassConfig(passes);
    compilationSet.updateDependencies(compiler);
    JSModule module = compilationSet.makeJSModule(compiler, "test-module", Collections.singleton(compilationUnit));
    compiler.compile(new JSSourceFile[]{}, new JSModule[]{module}, options);
    System.out.println(compiler.toSource());
    //System.out.println((System.nanoTime() - t0) * 1e-9);
  }
  
  // **************************************************************************
  // ResourceVisitorForFullUpdate

  class ResourceVisitorForFullUpdate implements IResourceVisitor {
    
    private List<IFile> files = new ArrayList<IFile>();
    
    public boolean visit(IResource resource) throws CoreException {
      if (resource instanceof IFile) {
        IFile file = (IFile) resource;
        if (isJavaScriptFile(file)) files.add(file);
      }
      return true;
    }
    
    public List<IFile> getFiles() {
      return files;
    }
    
  }
  
  // **************************************************************************
  // ResourceDeltaVisitorForIncrementalUpdate

  class ResourceDeltaVisitorForIncrementalUpdate implements IResourceDeltaVisitor {
    
    private boolean shallRebuild = false;
    private Collection<IFile> currentFiles;
    
    public ResourceDeltaVisitorForIncrementalUpdate(Collection<IFile> currentFiles) {
      this.currentFiles = currentFiles;
    }

    @Override
    public boolean visit(IResourceDelta delta) throws CoreException {
      IResource resource = delta.getResource();
      int flags = delta.getFlags();
      if (resource instanceof IProject) {
        // TODO: Test that the project has the required nature?
        /* CONTENT ENCODING DESCRIPTION OPEN TYPE SYNC MARKERS REPLACED LOCAL_CHANGED */
        if ((flags & (IResourceDelta.DESCRIPTION | IResourceDelta.OPEN)) != 0)
          shallRebuild = true;
      } else if (resource instanceof IFile) {
        IFile file = (IFile) resource;
        int kind = delta.getKind();
        if ((kind == IResourceDelta.ADDED && isJavaScriptFile(file) || 
            (kind == IResourceDelta.REMOVED && currentFiles.contains(file))))
          shallRebuild = true;
      }
      return true;
    }
    
    public boolean shallRebuild() {
      return shallRebuild;
    }

  }
  
  private boolean isJavaScriptFile(IFile file) throws CoreException {
    IContentDescription contentDescription = file.getContentDescription();
    if (contentDescription == null) return false;
    IContentType contentType = contentDescription.getContentType();
    return contentType.isKindOf(jsContentType);
  }

  // **************************************************************************
  // ResourceListener
  
  ResourceChangeListener resourceChangeListener = new ResourceChangeListener();
  
  /**
   * Listen for closure and deletion of projects.
   * @author Vincent Simonet
   *
   */
  private class ResourceChangeListener implements IResourceChangeListener {

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
      IResource resource = event.getResource();
      if (resource instanceof IProject) {
        IProject project = (IProject) resource;
        int type = event.getType();
        if ((type & (IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE)) != 0) {
          projectToCompilationSet.remove(project);
        }
      }
    }
    
  }

}
