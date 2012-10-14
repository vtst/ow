package net.vtst.ow.eclipse.js.closure.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.vtst.eclipse.easy.ui.properties.stores.IStore;
import net.vtst.eclipse.easy.ui.properties.stores.PluginPreferenceStore;
import net.vtst.ow.closure.compiler.compile.CompilableJSUnit;
import net.vtst.ow.closure.compiler.compile.CompilerRun;
import net.vtst.ow.closure.compiler.deps.AbstractJSProject;
import net.vtst.ow.closure.compiler.deps.JSProject;
import net.vtst.ow.closure.compiler.util.CompilerUtils;
import net.vtst.ow.closure.compiler.util.NullErrorManager;
import net.vtst.ow.eclipse.js.closure.OwJsClosureMessages;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;
import net.vtst.ow.eclipse.js.closure.compiler.ClosureCompiler;
import net.vtst.ow.eclipse.js.closure.compiler.ClosureCompilerOptions;
import net.vtst.ow.eclipse.js.closure.compiler.IJSIncludesProvider;
import net.vtst.ow.eclipse.js.closure.dev.OwJsDev;
import net.vtst.ow.eclipse.js.closure.preferences.ClosurePreferenceRecord;
import net.vtst.ow.eclipse.js.closure.util.Utils;

import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.PlatformUI;

import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.DiagnosticType;
import com.google.javascript.jscomp.ErrorManager;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.deps.SortedDependencies.CircularDependencyException;

/**
 * Project builder for the closure compiler.  It is activated on projects having the nature
 * {@code ClosureNature}.
 * @author Vincent Simonet
 */
public class ClosureBuilder extends IncrementalProjectBuilder {
  
  public static final String BUILDER_ID = "net.vtst.ow.eclipse.js.closure.closureBuilder";

  private static final DiagnosticType CIRCULAR_DEPENDENCY_ERROR =
      DiagnosticType.error("JSC_CIRCULAR_DEP",
          "Circular dependency detected: {0}");

  private IJSIncludesProvider jsLibraryProvider = OwJsClosurePlugin.getDefault().getJSLibraryProviderForClosureBuilder();
  private OwJsClosureMessages messages = OwJsClosurePlugin.getDefault().getMessages();
  private ProjectOrderManager projectOrderManager = OwJsClosurePlugin.getDefault().getProjectOrderManager();
  private JavaScriptEditorRegistry editorRegistry = OwJsClosurePlugin.getDefault().getEditorRegistry();
  
  public ClosureBuilder() {
    super();
  }
  
  protected void startupOnInitialize() {
    super.startupOnInitialize();
    OwJsDev.log("Initializing builder for: " + getProject().getName());
    // This is to force re-build on startup, because the persistent cache does not keep
    // enough information.
    this.forgetLastBuiltState();
  }

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IncrementalProjectBuilder#build(int, java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IProject[] build(
	    int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
	  IProject project = getProject();
    OwJsDev.log("Start build: %s", project.getName());

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
    OwJsDev.log("End build: %s", project.getName());
		return ResourceProperties.getTransitivelyReferencedProjects(project);
	}

	// **************************************************************************
	// Full build
  
  /**
   * Perform a full build of a project.
   * @param monitor  The project monitor.  This methods reports two units of work.
   * @param project  The project to build.
   * @throws CoreException, OperationCanceledException
   */
  private void fullBuild(IProgressMonitor monitor, IProject project) throws CoreException {
    monitor.subTask(messages.format("build_prepare"));
    boolean forgetIfCanceled = true;
    Compiler compiler = CompilerUtils.makeCompiler(new ErrorManagerForProjectBuild(project));
    compiler.initOptions(CompilerUtils.makeOptionsForParsingAndErrorReporting());
    try {
      File pathOfClosureBase = ClosureCompiler.getPathOfClosureBase(project);
      if (pathOfClosureBase == null) {
        monitor.worked(1);
        return;
      }
  
      // Create or get the project
      JSProject jsProject = ResourceProperties.getOrCreateJSProject(project);
      boolean jsProjectUpdated = updateJSProjectIfNeeded(monitor, compiler, project, jsProject);
      
      // Set the compilation units
      Set<IFile> files = ClosureCompiler.getJavaScriptFilesOfProject(project);
      ResourceProperties.setJavaScriptFiles(project, files);
      List<CompilableJSUnit> units = new ArrayList<CompilableJSUnit>(files.size());
      for (IFile file: files) {
        Utils.checkCancel(monitor);
        CompilableJSUnit unit = ResourceProperties.getJSUnit(file);
        if (unit == null) {
          unit = new CompilableJSUnit(
              jsProject, file.getLocation().toFile(), pathOfClosureBase,
              new CompilationUnitProviderFromEclipseIFile(file));
          ResourceProperties.setJSUnit(file, unit);
        } else {
          if (jsProjectUpdated) unit.clear();
        }
        units.add(unit);
      }
  
      try {
        jsProject.setUnits(compiler, units);
        monitor.worked(1);
        forgetIfCanceled = false;
        compileJavaScriptFiles(monitor, project, files, false);
      } catch (CircularDependencyException e) {
        CompilerUtils.reportError(compiler, JSError.make(CIRCULAR_DEPENDENCY_ERROR, e.getMessage()));
        forgetLastBuiltState();
      }
    } catch (OperationCanceledException e) {
      if (forgetIfCanceled) forgetLastBuiltState();
      throw e;
    } finally {
      compiler.getErrorManager().generateReport();      
    }
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
          if (ClosureCompiler.isJavaScriptFile(file)) fullBuildRequired = true;
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
      compileJavaScriptFiles(monitor, project, files, false);
    }

  }

  // **************************************************************************
  // Compilation of JavaScript files

  /**
   * Compile a collection of JavaScript files.
   * @param monitor  This method reports one unit of work.
   * @param project  The project the files belong to.
   * @param files  The collection of files to compile.
   * @param force  If true, forces the compilation of all files, even those which are 
   * not modified since their last build.
   * @throws CoreException
   */
  private void compileJavaScriptFiles(
      IProgressMonitor monitor, IProject project, Collection<IFile> files, boolean force) throws CoreException {
    SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1);
    int n = files.size(), i = 0;
    subMonitor.beginTask("build_compile", n);
    CompilerOptions options = ClosureCompilerOptions.makeForBuilder(project);
    boolean stripIncludedFiles = getStripIncludedFiles();
    boolean doNotKeepCompilationResultsOfClosedFilesInMemory = getDoNotKeepCompilationResultsOfClosedFilesInMemory();
    boolean doNotCompileClosedFiles = getDoNotCompileClosedFiles();
    for (IFile file: files) {
      ++i;
      boolean fileIsOpen = (editorRegistry.getTextEditor(file) != null);
      Utils.checkCancel(subMonitor);
      monitor.subTask(messages.format("build_compile_file", file.getName()));
      if (fileIsOpen || !doNotCompileClosedFiles) {
        CompilerOptions clonedOptions = i < n ? cloneCompilerOptions(project, options) : options;
        compileJavaScriptFile(file, clonedOptions, fileIsOpen, stripIncludedFiles, doNotKeepCompilationResultsOfClosedFilesInMemory, force);
      }
      subMonitor.worked(1);
    }
    subMonitor.done();
  }
  
  /**
   * @return  The status of the workspace preference stripProjectFiles.
   */
  private boolean getStripIncludedFiles() {
    IStore store = new PluginPreferenceStore(OwJsClosurePlugin.getDefault().getPreferenceStore());
    try {
      return ClosurePreferenceRecord.getInstance().stripProjectFiles.get(store);
    } catch (CoreException e) {
      return ClosurePreferenceRecord.getInstance().stripProjectFiles.getDefault();
    }
  }

  /**
   * @return  The status of the workspace preference doNotCompileClosedFiles.
   */
  private boolean getDoNotKeepCompilationResultsOfClosedFilesInMemory() {
    IStore store = new PluginPreferenceStore(OwJsClosurePlugin.getDefault().getPreferenceStore());
    try {
      return ClosurePreferenceRecord.getInstance().doNotKeepCompilationResultsOfClosedFilesInMemory.get(store);
    } catch (CoreException e) {
      return ClosurePreferenceRecord.getInstance().doNotKeepCompilationResultsOfClosedFilesInMemory.getDefault();
    }
  }

  /**
   * @return  The status of the workspace preference doNotCompileClosedFiles.
   */
  private boolean getDoNotCompileClosedFiles() {
    IStore store = new PluginPreferenceStore(OwJsClosurePlugin.getDefault().getPreferenceStore());
    try {
      return ClosurePreferenceRecord.getInstance().doNotCompileClosedFiles.get(store);
    } catch (CoreException e) {
      return ClosurePreferenceRecord.getInstance().doNotCompileClosedFiles.getDefault();
    }
  }

  /**
   * Clone a {@code CompilerOptions} object.  If the cloning fails, generate a fresh one.
   */
  private CompilerOptions cloneCompilerOptions(IProject project, CompilerOptions options) throws CoreException {
    try {
      return (CompilerOptions) options.clone();
    } catch (CloneNotSupportedException e) {
      // This should never happen, but anyway, let's do it.
      OwJsDev.log("ClosureBuilder.cloneCompilerOptions: cannot clone options, generate fresh ones.");
      return ClosureCompilerOptions.makeForBuilder(project);
    }
  }
  
  /**
   * Compile a JavaScript file.
   */
  private void compileJavaScriptFile(
      IFile file, CompilerOptions options,
      boolean fileIsOpen,
      boolean stripIncludedFiles, 
      boolean doNotKeepCompilationResultsOfClosedFilesInMemory,
      boolean force) throws CoreException {
    OwJsDev.log("Compiling %s", file.getFullPath().toOSString());
    CompilableJSUnit unit = ResourceProperties.getJSUnit(file);
    if (unit == null) return;
    ErrorManager errorManager = new ErrorManagerForFileBuild(unit, file);
    boolean keepCompilationResultsInMemory = !doNotKeepCompilationResultsOfClosedFilesInMemory || fileIsOpen;
    boolean force2 = force || editorRegistry.isNewlyOpenedFile(file);
    CompilerRun run = unit.fullCompile(options, errorManager, keepCompilationResultsInMemory, stripIncludedFiles, force2);
    run.setErrorManager(new NullErrorManager());
  }

  // **************************************************************************
  // Referenced projects
  
  /**
   * @param monitor  Progress monitor checked for cancellation.
   * @param compiler  Compiler used to parse libraries.
   * @param project  Project for which references shall be updated.
   * @param jsProject  Project for which references shall be updated.
   * @return true iif the project has been updated
   * @throws CoreException
   */
  private boolean updateJSProjectIfNeeded(
      IProgressMonitor monitor, Compiler compiler, 
      IProject project, JSProject jsProject) throws CoreException {
    // Get the referenced projects and libraries
    ProjectOrderManager.State projectOrderState = projectOrderManager.get();
    if (projectOrderState.getModificationStamp() <= jsProject.getReferencedProjectsModificationStamp()) return false;
    OwJsDev.log("Updating referenced projects of: %s", project.getName());
    ArrayList<IProject> projects = ClosureCompiler.getReferencedJavaScriptProjectsRecursively(
        Collections.singleton(project), projectOrderState.reverseOrderComparator());
    Collection<AbstractJSProject> libraries = jsLibraryProvider.getLibraries(compiler, monitor, projects);

    // Build the list of referenced projects and libraries
    ArrayList<AbstractJSProject> referencedProjectsAndLibraries = new ArrayList<AbstractJSProject>(projects.size() + libraries.size());
    for (IProject referencedProject: projects) referencedProjectsAndLibraries.add(ResourceProperties.getOrCreateJSProject(referencedProject));
    referencedProjectsAndLibraries.addAll(libraries);
    
    // Store the results in the jsProject.
    ResourceProperties.setTransitivelyReferencedProjects(project, projects.toArray(new IProject[0]));
    jsProject.setReferencedProjects(referencedProjectsAndLibraries, projectOrderState.getModificationStamp());
    jsProject.setExterns(jsLibraryProvider.getExterns(compiler, monitor, projects));
    return true;
  }
    
  /**
   * Force a build of all JavaScript projects in the workspace.
   */
  public static void buildAll() {
    // Get the list of projects having the Closure nature
    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    final ArrayList<IBuildConfiguration> configs = new ArrayList<IBuildConfiguration>(projects.length);
    for (IProject project: projects) {
      try {
        if (project.hasNature(ClosureNature.NATURE_ID)) {
          configs.add(project.getActiveBuildConfig());
        }
      } catch (CoreException e) {
        // This happens if the project is not open
      }
    }
    // Build
    Job buildAll = new Job(OwJsClosurePlugin.getDefault().getMessages().getString("build_all")) {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          ResourcesPlugin.getWorkspace().build(configs.toArray(new IBuildConfiguration[0]), IncrementalProjectBuilder.FULL_BUILD, false, monitor);
          return Status.OK_STATUS;
        } catch (CoreException e) {
          return e.getStatus();
        }
      }
    };
    buildAll.schedule();
  }
}
