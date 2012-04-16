package net.vtst.ow.eclipse.js.closure.launching;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.vtst.eclipse.easy.ui.properties.stores.IReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.IStore;
import net.vtst.eclipse.easy.ui.properties.stores.LaunchConfigurationReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.LaunchConfigurationStore;
import net.vtst.eclipse.easy.ui.properties.stores.PluginPreferenceStore;
import net.vtst.eclipse.easy.ui.properties.stores.ProjectPropertyStore;
import net.vtst.ow.closure.compiler.compile.DefaultExternsProvider;
import net.vtst.ow.closure.compiler.deps.AbstractJSProject;
import net.vtst.ow.closure.compiler.deps.JSLibrary;
import net.vtst.ow.closure.compiler.deps.JSLibrary.CacheSettings;
import net.vtst.ow.closure.compiler.deps.JSProject;
import net.vtst.ow.closure.compiler.deps.JSUnit;
import net.vtst.ow.closure.compiler.deps.JSUnitProvider;
import net.vtst.ow.closure.compiler.util.CompilerUtils;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;
import net.vtst.ow.eclipse.js.closure.builder.ClosureNature;
import net.vtst.ow.eclipse.js.closure.compiler.ClosureCompiler;
import net.vtst.ow.eclipse.js.closure.compiler.ClosureCompilerOptions;
import net.vtst.ow.eclipse.js.closure.preferences.ClosurePreferenceRecord;
import net.vtst.ow.eclipse.js.closure.properties.ClosureProjectPropertyRecord;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;

import com.google.common.collect.Lists;
import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerInput;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSModule;
import com.google.javascript.jscomp.deps.SortedDependencies.CircularDependencyException;

public class ClosureCompilerLaunchConfigurationDelegate extends LaunchConfigurationDelegate {
  
  // TODO: Manage output
  // TODO: Check order of libraries
  // TODO: Run compiler in thread
  
  private ClosureCompilerLaunchConfigurationRecord launchRecord = ClosureCompilerLaunchConfigurationRecord.getInstance();
  private ClosureProjectPropertyRecord projectRecord = ClosureProjectPropertyRecord.getInstance();

  @Override
  public void launch(ILaunchConfiguration config, String mode, ILaunch launch, IProgressMonitor monitor)
      throws CoreException {
    IReadOnlyStore store = new LaunchConfigurationReadOnlyStore(config);
    List<IResource> resources = launchRecord.inputResources.get(store);
    if (resources.isEmpty()) return;
    
    // We arbitrarily take the first project as the master one for getting options.
    IProject project = resources.get(0).getProject();
    IReadOnlyStore projectStore = new ProjectPropertyStore(project, OwJsClosurePlugin.PLUGIN_ID);
    File closureBasePath = projectRecord.closureBasePath.get(projectStore);
    
    Compiler compiler = CompilerUtils.makeCompiler(CompilerUtils.makePrintingErrorManager(System.out));
    CompilerOptions options = ClosureCompilerOptions.makeForFullCompilation(project, config);
    compiler.initOptions(options);
    Iterable<IProject> projects = getClosureProjects(resources);
    List<AbstractJSProject> libraries = getLibraries(compiler, projects);
    Collection<IFile> selectedJsFiles = getJavaScriptFiles(resources);
    Collection<IFile> allJsFiles = getJavaScriptFiles(projects);
    Map<IFile, JSUnit> units = makeJSUnits(closureBasePath, allJsFiles);
    List<JSUnit> selectedJsUnits = new ArrayList<JSUnit>(selectedJsFiles.size());
    for (IFile selectedJsFile: selectedJsFiles) selectedJsUnits.add(units.get(selectedJsFile));
    try {
      JSProject jsProject = makeJSProject(compiler, Lists.newArrayList(units.values()), libraries, closureBasePath);
      List<JSUnit> unitsToBeCompiled = jsProject.getSortedDependenciesOf(selectedJsUnits);
      JSModule module = new JSModule("main");
      for (JSUnit unit: unitsToBeCompiled) {
        module.add(new CompilerInput(unit.getAst(false)));
      }
      compiler.compileModules(DefaultExternsProvider.getAsSourceFiles(), Collections.singletonList(module), options);
      System.out.println(compiler.toSource());
      compiler.getErrorManager().generateReport();
    } catch (CircularDependencyException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
    
  /**
   * Get the projects the resources in {@code resources} belong to.  Include recursively
   * their referenced projects. 
   */
  private Iterable<IProject> getClosureProjects(Iterable<IResource> resources) throws CoreException {
    Set<IProject> projects = new HashSet<IProject>();
    LinkedList<IProject> projectsToVisit = new LinkedList<IProject>();
    for (IResource resource: resources) {
      IProject project = resource.getProject();
      if (project.hasNature(ClosureNature.NATURE_ID) && projects.add(project)) 
        projectsToVisit.add(project);
    }
    while (!projectsToVisit.isEmpty()) {
      for (IProject referencedProject: projectsToVisit.removeLast().getReferencedProjects()) {
        if (referencedProject.hasNature(ClosureNature.NATURE_ID) && projects.add(referencedProject)) 
          projectsToVisit.add(referencedProject);
      }
    }
    return projects;
  }
  
  /**
   * Get the JavaScript files which are in a set of resources.
   * @param resources  The set of resources to scan.
   * @return  The list of JavaScript files.
   * @throws CoreException
   */
  private Collection<IFile> getJavaScriptFiles(Iterable<? extends IResource> resources) throws CoreException {
    final Set<IFile> files = new HashSet<IFile>();
    IResourceVisitor visitor = new IResourceVisitor(){
      public boolean visit(IResource resource) throws CoreException {
        if (resource instanceof IFile) {
          IFile file = (IFile) resource;
          if (ClosureCompiler.isJavaScriptFile(file)) files.add(file);
        }
        return true;
      }};
    for (IResource resource: resources) resource.accept(visitor);
    return files;
  }

  private List<AbstractJSProject> getLibraries(AbstractCompiler compiler, Iterable<IProject> projects) throws CoreException {
    CacheSettings cacheSettings = getCacheSettings();
    List<AbstractJSProject> libraries = new ArrayList<AbstractJSProject>();
    Set<File> addedLibraries = new HashSet<File>();
    for (IProject project: projects) {
      IReadOnlyStore store = new ProjectPropertyStore(project, OwJsClosurePlugin.PLUGIN_ID);
      File closureBasePath = projectRecord.closureBasePath.get(store);
      if (addedLibraries.add(closureBasePath)) {
        libraries.add(getLibrary(compiler, closureBasePath, closureBasePath, true, cacheSettings));
      }
      for (File libraryPath: projectRecord.otherLibraries.get(store)) {
        if (addedLibraries.add(libraryPath)) {
          libraries.add(getLibrary(compiler, libraryPath, closureBasePath, false, cacheSettings));
        }
      }
    }
    return libraries;
  }
  
  private JSLibrary getLibrary(AbstractCompiler compiler, File path, File pathOfClosureBase, boolean isClosureBase, CacheSettings cacheSettings) {
    JSLibrary library = new JSLibrary(path, pathOfClosureBase, cacheSettings);
    library.setUnits(compiler);
    return library;
  }
  
  private JSLibrary.CacheSettings getCacheSettings() {
    ClosurePreferenceRecord r = ClosurePreferenceRecord.getInstance();
    JSLibrary.CacheSettings result = new JSLibrary.CacheSettings();
    IStore prefs = new PluginPreferenceStore(OwJsClosurePlugin.getDefault().getPreferenceStore());
    try {
      result.cacheDepsFiles = r.cacheLibraryDepsFiles.get(prefs);
    } catch (CoreException e) {
      result.cacheDepsFiles = r.cacheLibraryDepsFiles.getDefault();
    }
    result.cacheStrippedFiles = JSLibrary.CacheMode.DISABLED;
    return result;
  }
  
  private Map<IFile, JSUnit> makeJSUnits(File closureBasePath, Collection<IFile> files) {
    final Map<IFile, JSUnit> map = new HashMap<IFile, JSUnit>(files.size());
    for (IFile file: files) {
      File path = file.getLocation().toFile();
      map.put(file, new JSUnit(path, closureBasePath, new JSUnitProvider.FromFile(path)));
    }
    return map;
  }
  
  private JSProject makeJSProject(AbstractCompiler compiler, List<JSUnit> units, List<AbstractJSProject> libraries, File closureBasePath) throws CircularDependencyException {
    JSProject jsProject = new JSProject();
    jsProject.setUnits(compiler, units);
    jsProject.setReferencedProjects(libraries);
    return jsProject;
  }

}
