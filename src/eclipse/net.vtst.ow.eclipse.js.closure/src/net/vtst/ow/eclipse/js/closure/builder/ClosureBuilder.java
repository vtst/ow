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
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
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
  
  // TODO There might be one instance per thread.  Is JSSet enough thread safe?

  public static final String BUILDER_ID = "net.vtst.ow.eclipse.js.closure.closureBuilder";
  
  private JSLibraryManager jsLibraryManager = OwJsClosurePlugin.getDefault().getJSLibraryManager();
  
  public ClosureBuilder() {
    super();
    System.out.println("Creating builder: " + this.toString());
    System.out.println("  in:" + Thread.currentThread().toString());
  }

	protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map args, IProgressMonitor monitor) throws CoreException {
	  IProject project = getProject();
	  System.out.println("Building project: " + project.getName() + " with: " + this.toString());
    System.out.println("  in:" + Thread.currentThread().toString());
		if (kind == FULL_BUILD) {
      fullBuild(project);
		} else {
			IResourceDelta delta = getDelta(project);
			if (delta == null) {
			  fullBuild(project);
			} else {
        incrementalBuild(project, delta);
			}
		}
		return null;
	}

  public static void clearProject(IProject project) throws CoreException {
    ResourceProperties.setJavaScriptFiles(project, null);
    ResourceProperties.setJSProject(project, null);
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

  public void fullBuild(IProject project) throws CoreException {
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
    compileJavaScriptFiles(files, false);
  }
  
  private void compileJavaScriptFiles(Iterable<IFile> files, boolean force) throws CoreException {
    for (IFile file: files) {
      compileJavaScriptFile(file, force);
    }
  }
  
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
    
    public Iterable<IFile> changedFiles() {
      return changedFiles;
    }

  }


  private void incrementalBuild(IProject project, IResourceDelta delta) throws CoreException {
    // If the project is not already known by the builder, a full build is required.
    JSProject jsProject = ResourceProperties.getJSProject(project);
    Collection<IFile> files = ResourceProperties.getJavaScriptFiles(project);
    if (jsProject == null || files == null) {
      fullBuild(project);
      return;
    }
    // Visit the deltas.
    ResourceDeltaVisitorForIncrementalBuild visitor = new ResourceDeltaVisitorForIncrementalBuild(files);
    delta.accept(visitor);
    if (visitor.fullBuildRequired()) {
      fullBuild(project);
    } else {
      compileJavaScriptFiles(files, false);
    }

  }

  // **************************************************************************
  // Helper functions

  private static final String JS_CONTENT_TYPE_ID =
      "org.eclipse.wst.jsdt.core.jsSource";

  private final IContentType jsContentType =
      Platform.getContentTypeManager().getContentType(JS_CONTENT_TYPE_ID);

  private boolean isJavaScriptFile(IFile file) throws CoreException {
    IContentDescription contentDescription = file.getContentDescription();
    if (contentDescription == null) return false;
    IContentType contentType = contentDescription.getContentType();
    return contentType.isKindOf(jsContentType);
  }

}
