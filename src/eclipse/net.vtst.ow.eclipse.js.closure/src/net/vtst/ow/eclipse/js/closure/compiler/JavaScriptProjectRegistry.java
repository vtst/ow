package net.vtst.ow.eclipse.js.closure.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import net.vtst.ow.closure.compiler.compile.CompilableJSUnit;
import net.vtst.ow.closure.compiler.compile.CompilerRun;
import net.vtst.ow.closure.compiler.deps.JSLibrary;
import net.vtst.ow.closure.compiler.deps.JSSet;
import net.vtst.ow.closure.compiler.deps.JSUnit;
import net.vtst.ow.closure.compiler.util.CompilerUtils;
import net.vtst.ow.eclipse.js.closure.builder.ClosureNature;
import net.vtst.ow.eclipse.js.closure.properties.ClosureProjectPersistentPropertyHelper;
import net.vtst.ow.eclipse.js.closure.util.Pair;

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

import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.ErrorManager;

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
  // should be collected.  Libraries are identified by pairs (closureBasePath, libraryBasePath).
  private WeakHashMap<Pair<File, File>, JSLibrary> pathToLibrary = new WeakHashMap<Pair<File, File>, JSLibrary>();

  public JavaScriptProjectRegistry(IWorkbench workbench) {
    ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener);
  }

  public void dispose() {
    projectToCompilationSet.clear();
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
  }

  
  // **************************************************************************
  // Libraries
  
  private synchronized JSLibrary getLibrary(File libraryPath, File pathOfClosureBase, boolean isClosureBase) {
    Pair<File, File> key = new Pair<File, File>(libraryPath, pathOfClosureBase);
    JSLibrary library = pathToLibrary.get(key);
    if (library == null) {
      library = new JSLibrary(libraryPath, pathOfClosureBase, isClosureBase);
      pathToLibrary.put(key, library);
    }
    return library;
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
    File pathOfClosureBase = new File(helper.getClosureBaseDir());
    compilationSet.addCompilationSet(getLibrary(pathOfClosureBase, pathOfClosureBase, true));
    for (String libraryPath: helper.getOtherLibraries()) {
      compilationSet.addCompilationSet(getLibrary(new File(libraryPath), pathOfClosureBase, false));
    }
    // Add the compilation units for the referenced projects
    // TODO: Be careful to avoid loops!
    for (IProject referencedProject: project.getReferencedProjects()) {
      if (referencedProject.hasNature(ClosureNature.NATURE_ID)) {
        System.out.println("Referenced project: " + project.getName());
      }
    }
    // Add the files of the current project.
    for (IFile file: jsFiles) {
      CompilableJSUnit unit = new CompilableJSUnit(
          compilationSet, file.getLocation().toFile(), pathOfClosureBase,
          new CompilationUnitProviderFromEclipseIFile(file));
      compilationSet.addCompilationUnit(file, unit);
    }
    // Compile
    for (Entry<IFile, JSUnit> entry: compilationSet.entries()) {
      JSUnit unit = entry.getValue();
      if (unit instanceof CompilableJSUnit) {
        compileUnit((CompilableJSUnit) unit, entry.getKey());
      }
    }
    projectToCompilationSet.put(project, compilationSet);
  }
  
  /**
   * Update incrementally the compilation set for a project.
   * @param project
   * @param delta
   */
  public void incrementalUpdate(IProject project, IResourceDelta delta) throws CoreException {
    JSSet<IFile> compilationSet = projectToCompilationSet.get(project);
    if (compilationSet == null) {
      fullUpdate(project);
      return;
    }
    ResourceDeltaVisitorForIncrementalUpdate visitor = 
        new ResourceDeltaVisitorForIncrementalUpdate(getFilesOfProject(project));
    delta.accept(visitor);
    if (visitor.fullUpdateRequired()) {
      fullUpdate(project);
    } else {
      for (IFile file: visitor.changedFiles()) {
        JSUnit unit = compilationSet.getCompilationUnit(file);
        if (unit instanceof CompilableJSUnit) {
          compileUnit((CompilableJSUnit) unit, file);
        }
      }
    }
  }
  
  private void compileUnit(CompilableJSUnit cunit, IFile file) {
    CompilerOptions options = CompilerUtils.makeOptions();  // TODO: Clean up the option generation.  Allow customization.
    options.checkTypes = true;
    options.setInferTypes(true);
    options.closurePass = true;
    ErrorManager errorManager = new ErrorManagerGeneratingProblemMarkers(cunit, file);
    CompilerRun run = cunit.compile(options, errorManager);
    run.setErrorManager(new NullErrorManager());
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
    
    private boolean fullUpdateRequired = false;
    private Collection<IFile> currentFiles;
    private List<IFile> changedFiles = new LinkedList<IFile>();
    
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
          fullUpdateRequired = true;
      } else if (resource instanceof IFile) {
        IFile file = (IFile) resource;
        switch (delta.getKind()) {
        case IResourceDelta.ADDED:
          if (isJavaScriptFile(file)) fullUpdateRequired = true;
          break;
        case IResourceDelta.REMOVED:
          if (currentFiles.contains(file)) fullUpdateRequired = true;
          break;
        case IResourceDelta.CHANGED:
          if (currentFiles.contains(file)) changedFiles.add(file);
        }
      }
      return true;
    }
    
    public boolean fullUpdateRequired() {
      return fullUpdateRequired;
    }
    
    public Iterable<IFile> changedFiles() {
      return changedFiles;
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
