package net.vtst.ow.eclipse.js.closure.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.vtst.ow.closure.compiler.compile.CompilableJSUnit;
import net.vtst.ow.closure.compiler.compile.CompilerRun;
import net.vtst.ow.closure.compiler.deps.AbstractJSProject;
import net.vtst.ow.closure.compiler.deps.JSProject;
import net.vtst.ow.closure.compiler.util.CompilerUtils;
import net.vtst.ow.eclipse.js.closure.OwJsClosureMessages;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;
import net.vtst.ow.eclipse.js.closure.compiler.CompilationUnitProviderFromEclipseIFile;
import net.vtst.ow.eclipse.js.closure.compiler.ErrorManagerGeneratingProblemMarkers;
import net.vtst.ow.eclipse.js.closure.compiler.NullErrorManager;
import net.vtst.ow.eclipse.js.closure.properties.ClosureProjectPersistentPropertyHelper;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;

import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.ErrorManager;
import com.google.javascript.jscomp.deps.SortedDependencies.CircularDependencyException;

/**
 * Project builder for the closure compiler.  It is activated on projects having the nature
 * {@code ClosureNature}.
 * @author Vincent Simonet
 */
public class ClosureBuilder extends IncrementalProjectBuilder {
  
  public static final String BUILDER_ID = "net.vtst.ow.eclipse.js.closure.closureBuilder";
  
  private JSLibraryManager jsLibraryManager = OwJsClosurePlugin.getDefault().getJSLibraryManager();
  private OwJsClosureMessages messages = OwJsClosurePlugin.getDefault().getMessages();
  
  public ClosureBuilder() {
    super();
  }

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IncrementalProjectBuilder#build(int, java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IProject[] build(
	    int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
	  IProject project = getProject();
	  monitor.beginTask(messages.format("build_closure", project.getName()), 2);
		if (kind == FULL_BUILD) {
      fullBuild(monitor, project);
		} else {
			IResourceDelta delta = getDelta(project);
			if (delta == null) {
			  fullBuild(monitor, project);
			} else {
        incrementalBuild(monitor, project, delta);
			}
		}
    monitor.done();
		return null;
	}

	// **************************************************************************
	// Full build
  
  /**
   * Gets the list of JavaScript files in a project.
   * @param project  The project to visit.
   * @return  The list of JavaScript file.  May be empty, but never null.
   * @throws CoreException 
   */
  private Set<IFile> getJavaScriptFiles(IProject project) throws CoreException {
    final Set<IFile> files = new HashSet<IFile>();
    IResourceVisitor visitor = new IResourceVisitor() {
      @Override
      public boolean visit(IResource resource) throws CoreException {
        if (resource instanceof IFile) {
          IFile file = (IFile) resource;
          if (isJavaScriptFile(file)) files.add(file);
        }
        return true;
      }
    };
    project.accept(visitor);
    return files;
  }

  /**
   * Perform a full build of a project.
   * @param monitor  The project monitor.  This methods reports two units of work.
   * @param project  The project to build.
   * @throws CoreException
   */
  private void fullBuild(IProgressMonitor monitor, IProject project) throws CoreException {
    monitor.subTask("build_prepare");
    Compiler compiler = CompilerUtils.makeCompiler(new NullErrorManager());  // TODO!
    compiler.initOptions(CompilerUtils.makeOptions());
    ClosureProjectPersistentPropertyHelper helper = new ClosureProjectPersistentPropertyHelper(project);
    File pathOfClosureBase = helper.getClosureBaseDirAfFile();

    // Create or get the project
    JSProject jsProject = ResourceProperties.getJSProject(project);
    if (jsProject == null) {
      jsProject = new JSProject();
      ResourceProperties.setJSProject(project, jsProject);
    }
    
    // Set the referenced projects
    List<AbstractJSProject> referencedJsProjects = new ArrayList<AbstractJSProject>();
    if (pathOfClosureBase != null) {
      referencedJsProjects.add(jsLibraryManager.get(compiler, pathOfClosureBase, pathOfClosureBase, true));
    }
    for (String libraryPath: helper.getOtherLibraries()) {
      checkCancel(monitor, true);
      referencedJsProjects.add(jsLibraryManager.get(compiler, new File(libraryPath), pathOfClosureBase, false));
    }
    for (IProject referencedProject: project.getReferencedProjects()) {
      if (referencedProject.hasNature(ClosureNature.NATURE_ID)) {
        JSProject referencedJsProject = ResourceProperties.getJSProject(referencedProject);
        if (referencedJsProject != null) referencedJsProjects.add(referencedJsProject);
      }
    }
    jsProject.setReferencedProjects(referencedJsProjects);
    
    // Set the compilation units
    Set<IFile> files = getJavaScriptFiles(project);
    ResourceProperties.setJavaScriptFiles(project, files);
    List<CompilableJSUnit> units = new ArrayList<CompilableJSUnit>(files.size());
    for (IFile file: files) {
      checkCancel(monitor, true);
      CompilableJSUnit unit = ResourceProperties.getJSUnit(file);
      if (unit == null) {
        unit = new CompilableJSUnit(
            jsProject, file.getLocation().toFile(), pathOfClosureBase,
            new CompilationUnitProviderFromEclipseIFile(file));
        ResourceProperties.setJSUnit(file, unit);
      }
      units.add(unit);
    }
    try {
      jsProject.setUnits(compiler, units);
    } catch (CircularDependencyException e) {
      throw new CoreException(new Status(IStatus.ERROR, OwJsClosurePlugin.PLUGIN_ID, e.getMessage(), e));
    }
    monitor.worked(1);
    compileJavaScriptFiles(monitor, files, false);
  }

  // **************************************************************************
  // Incremental build
  
  private class ResourceDeltaVisitorForIncrementalBuild implements IResourceDeltaVisitor {
    
    private boolean fullBuildRequired = false;
    private Collection<IFile> currentFiles;
    private List<IFile> changedFiles = new LinkedList<IFile>();
    
    public ResourceDeltaVisitorForIncrementalBuild(Collection<IFile> currentFiles) {
      this.currentFiles = currentFiles;
    }

    @Override
    public boolean visit(IResourceDelta delta) throws CoreException {
      IResource resource = delta.getResource();
      int flags = delta.getFlags();
      if (resource instanceof IProject) {
        /* CONTENT ENCODING DESCRIPTION OPEN TYPE SYNC MARKERS REPLACED LOCAL_CHANGED */
        if ((flags & (IResourceDelta.DESCRIPTION | IResourceDelta.OPEN)) != 0) {
          fullBuildRequired = true;
          return false;
        }
      } else if (resource instanceof IFile) {
        IFile file = (IFile) resource;
        switch (delta.getKind()) {
        case IResourceDelta.ADDED:
          if (isJavaScriptFile(file)) fullBuildRequired = true;
          return false;
        case IResourceDelta.REMOVED:
          if (currentFiles.contains(file)) fullBuildRequired = true;
          return false;
        case IResourceDelta.CHANGED:
          if (currentFiles.contains(file)) changedFiles.add(file);
        }
      }
      return true;
    }
    
    public boolean fullBuildRequired() {
      return fullBuildRequired;
    }

  }

  /**
   * Do an incremental build of a project.  Reverts to a full build if an incremental build
   * cannot be done.
   * @param monitor  This method reports two units of work.
   * @param project  The project to build.
   * @param delta  The delta since the last build.
   * @throws CoreException
   */
  private void incrementalBuild(IProgressMonitor monitor, IProject project, IResourceDelta delta) throws CoreException {
    // If the project is not already known by the builder, a full build is required.
    JSProject jsProject = ResourceProperties.getJSProject(project);
    Collection<IFile> files = ResourceProperties.getJavaScriptFiles(project);
    if (jsProject == null || files == null) {
      fullBuild(monitor, project);
      return;
    }
    // Visit the deltas.
    ResourceDeltaVisitorForIncrementalBuild visitor = new ResourceDeltaVisitorForIncrementalBuild(files);
    delta.accept(visitor);
    if (visitor.fullBuildRequired()) {
      fullBuild(monitor, project);
    } else {
      monitor.worked(1);
      compileJavaScriptFiles(monitor, files, false);
    }

  }

  // **************************************************************************
  // Helper functions
  
  /**
   * Check whether the build has been canceled, and aborts the build if yes.
   * @param monitor  The progress monitor to check.
   * @param forgetLastBuiltState  Set to true if the last built state must be forgotten
   *   if the build has been canceled.
   */
  private void checkCancel(IProgressMonitor monitor, boolean forgetLastBuiltState) {
    if (monitor.isCanceled()) {
      if (forgetLastBuiltState) forgetLastBuiltState();
      throw new OperationCanceledException();  // This is caught by Eclipse Platform.
    }
  }

  /**
   * Compile a collection of JavaScript files.
   * @param monitor  This method reports one unit of work.
   * @param files  The collection of files to compile.
   * @param force  If true, forces the compilation of all files, even those which are 
   * not modified since their last build.
   * @throws CoreException
   */
  private void compileJavaScriptFiles(IProgressMonitor monitor, Collection<IFile> files, boolean force) throws CoreException {
    SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 0);  // TODO: is ticks==0 correct?
    subMonitor.beginTask("build_compile", files.size());
    for (IFile file: files) {
      checkCancel(subMonitor, false);
      monitor.subTask(messages.format("build_compile_file", file.getName()));
      compileJavaScriptFile(file, force);
      subMonitor.worked(1);
    }
    subMonitor.done();
    monitor.worked(1);
  }
  
  /**
   * Compile a JavaScript file.
   * @param file
   * @param force
   * @throws CoreException
   */
  private void compileJavaScriptFile(IFile file, boolean force) throws CoreException {
    CompilableJSUnit unit = ResourceProperties.getJSUnit(file);
    if (unit == null) return;
    CompilerOptions options = CompilerUtils.makeOptions();  // TODO: Clean up the option generation.  Allow customization.
    options.checkTypes = true;
    options.setInferTypes(true);
    options.closurePass = true;
    ErrorManager errorManager = new ErrorManagerGeneratingProblemMarkers(unit, file);
    CompilerRun run = unit.fullCompile(options, errorManager, force);
    run.setErrorManager(new NullErrorManager());
  }

  private static final String JS_CONTENT_TYPE_ID =
      "org.eclipse.wst.jsdt.core.jsSource";

  private final IContentType jsContentType =
      Platform.getContentTypeManager().getContentType(JS_CONTENT_TYPE_ID);

  /**
   * Test whether a file is a JavaScript file (by looking at its content type).
   * @param file  The file to test.
   * @return  true iif the given file is a JavaScript file.
   * @throws CoreException
   */
  private boolean isJavaScriptFile(IFile file) throws CoreException {
    IContentDescription contentDescription = file.getContentDescription();
    if (contentDescription == null) return false;
    IContentType contentType = contentDescription.getContentType();
    return contentType.isKindOf(jsContentType);
  }
}
