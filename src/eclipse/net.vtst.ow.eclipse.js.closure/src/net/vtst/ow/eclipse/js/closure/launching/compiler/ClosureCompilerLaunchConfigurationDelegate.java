package net.vtst.ow.eclipse.js.closure.launching.compiler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.vtst.eclipse.easy.ui.properties.stores.IReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.LaunchConfigurationReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.ResourcePropertyStore;
import net.vtst.ow.closure.compiler.deps.AbstractJSProject;
import net.vtst.ow.closure.compiler.deps.JSExtern;
import net.vtst.ow.closure.compiler.deps.JSProject;
import net.vtst.ow.closure.compiler.deps.JSUnit;
import net.vtst.ow.closure.compiler.deps.JSUnitProvider;
import net.vtst.ow.closure.compiler.util.CompilerUtils;
import net.vtst.ow.eclipse.js.closure.OwJsClosureMessages;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;
import net.vtst.ow.eclipse.js.closure.compiler.ClosureCompiler;
import net.vtst.ow.eclipse.js.closure.compiler.ClosureCompilerOptions;
import net.vtst.ow.eclipse.js.closure.compiler.IJSIncludesProvider;
import net.vtst.ow.eclipse.js.closure.properties.file.ClosureFilePropertyRecord;
import net.vtst.ow.eclipse.js.closure.util.Utils;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;

import com.google.common.collect.Lists;
import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerInput;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSModule;
import com.google.javascript.jscomp.SourceFile;
import com.google.javascript.jscomp.deps.SortedDependencies.CircularDependencyException;

public class ClosureCompilerLaunchConfigurationDelegate extends LaunchConfigurationDelegate {
  
  public static final String TYPE_ID = "net.vtst.ow.eclipse.js.closure.launching.compiler";
  
  private OwJsClosureMessages messages = OwJsClosurePlugin.getDefault().getMessages();
  private ClosureCompilerLaunchConfigurationRecord record = ClosureCompilerLaunchConfigurationRecord.getInstance();
  private IJSIncludesProvider includesProvider = new JSIncludesProviderForLaunch();

  @Override
  public void launch(ILaunchConfiguration config, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
    monitor.beginTask(config.getName(), 1);
    monitor.subTask(messages.getString("ClosureCompilerLaunchConfigurationDelegate_prepareCompiler"));
    IReadOnlyStore store = new LaunchConfigurationReadOnlyStore(config);
    List<IResource> resources = record.inputResources.get(store);
    if (resources.isEmpty()) return;
    
    // Getting the stores for project configurations
    IProject project = null;
    if (record.useProjectPropertiesForChecks.get(store) ||
        record.useProjectPropertiesForIncludes.get(store)) {
      project = ClosureCompiler.getCommonProject(resources);
      if (project == null) throw new CoreException(new Status(Status.ERROR, OwJsClosurePlugin.PLUGIN_ID, messages.getString("ClosureCompilerLaunchConfigurationDelegate_differentProjects")));
    }
    IReadOnlyStore storeForChecks = record.useProjectPropertiesForChecks.get(store) ? new ResourcePropertyStore(project, OwJsClosurePlugin.PLUGIN_ID) : store; 
    IReadOnlyStore storeForIncludes = record.useProjectPropertiesForIncludes.get(store) ? new ResourcePropertyStore(project, OwJsClosurePlugin.PLUGIN_ID) : store; 

    // Get the output file
    IFile outputFile = getOutputFile(store, resources);
    
    // Create and configure the compiler
    ClosureCompilerProcess process = new ClosureCompilerProcess(launch);
    Compiler compiler = CompilerUtils.makeCompiler(process.getErrorManager());
    CompilerOptions options = ClosureCompilerOptions.makeForLaunch(storeForChecks, store);
    compiler.initOptions(options);

    // Get the files to compile
    Set<IFile> allFiles, rootFiles;
    List<AbstractJSProject> libraries;
    if (record.manageClosureDependencies.get(store)) {
      // If dependencies are managed, we take all projects containing selected resources,
      // then all their referenced projects.
      // TODO: It should not be allowed to customize the includes in this case, we should always
      // use the project ones.
      Collection<IProject> projects = getProjects(resources);
      Comparator<IProject> comparator = OwJsClosurePlugin.getDefault().getProjectOrderManager().get().reverseOrderComparator();
      ArrayList<IProject> allProjects = ClosureCompiler.getReferencedJavaScriptProjectsRecursively(projects, comparator);
      monitor.subTask(messages.getString("ClosureCompilerLaunchConfigurationDelegate_loadLibraries"));
      libraries = includesProvider.getLibraries(compiler, monitor, allProjects);
      monitor.subTask(messages.getString("ClosureCompilerLaunchConfigurationDelegate_prepareCompiler"));
      allFiles = ClosureCompiler.getJavaScriptFilesOfProjects(allProjects);
      for (IResource resource: resources) {
        if (!(resource instanceof IProject)) allFiles.addAll(ClosureCompiler.getJavaScriptFiles(resource));
      }
      rootFiles = Utils.getAllContainedFilesWhichAreInSet(resources, allFiles);
    } else {
      // If dependencies are not managed, we take only what has been selected.
      monitor.subTask(messages.getString("ClosureCompilerLaunchConfigurationDelegate_loadLibraries"));
      libraries = includesProvider.getLibraries(compiler, monitor, storeForIncludes);
      monitor.subTask(messages.getString("ClosureCompilerLaunchConfigurationDelegate_prepareCompiler"));
      allFiles = ClosureCompiler.getJavaScriptFiles(resources);
      rootFiles = allFiles;
    }

    // Build the project to compile
    File closureBasePath = ClosureCompiler.getPathOfClosureBase(storeForIncludes);
    Map<IFile, JSUnit> units = makeJSUnits(closureBasePath, allFiles);
    List<JSUnit> rootUnits = new ArrayList<JSUnit>(rootFiles.size());
    for (IFile selectedJsFile: rootFiles) rootUnits.add(units.get(selectedJsFile));
    try {
      JSProject jsProject = makeJSProject(compiler, Lists.newArrayList(units.values()), libraries, closureBasePath);
      List<JSUnit> rootUnitsWithTheirDependencies = jsProject.getSortedDependenciesOf(rootUnits);
      JSModule module = new JSModule("main");
      for (JSUnit unit: rootUnitsWithTheirDependencies) module.add(new CompilerInput(unit.getAst(false)));
      monitor.subTask(messages.getString("ClosureCompilerLaunchConfigurationDelegate_runCompiler"));
      compiler.compileModules(getExterns(compiler, monitor, storeForIncludes), Collections.singletonList(module), options);
      if (outputFile.exists()) {
        outputFile.setContents(new ByteArrayInputStream(compiler.toSource().getBytes("UTF-8")), false, false, monitor);
      } else {
        outputFile.create(new ByteArrayInputStream(compiler.toSource().getBytes("UTF-8")), false, monitor);
      }
      outputFile.setCharset("UTF-8", monitor);
      ClosureFilePropertyRecord.getInstance().generatedByCompiler.set(new ResourcePropertyStore(outputFile, OwJsClosurePlugin.PLUGIN_ID), true);
      process.setTerminated();
      monitor.done();
    } catch (CircularDependencyException e) {
      throw new CoreException(new Status(Status.ERROR, OwJsClosurePlugin.PLUGIN_ID, e.getLocalizedMessage(), e));
    } catch (IOException e) {
      throw new CoreException(new Status(Status.ERROR, OwJsClosurePlugin.PLUGIN_ID, e.getLocalizedMessage(), e));
    }
  }
  
  private List<SourceFile> getExterns(AbstractCompiler compiler, IProgressMonitor monitor, IReadOnlyStore store) throws CoreException {
    List<JSExtern> externs = includesProvider.getExterns(compiler, monitor, store);
    List<SourceFile> sourceFiles = new ArrayList<SourceFile>(externs.size());
    for (JSExtern extern: externs) sourceFiles.add(extern.getSourceFile());
    return sourceFiles;
  }
  
  private IFile getOutputFile(IReadOnlyStore store, List<IResource> resources) throws CoreException {
    if (record.useDefaultOutputFile.get(store)) {
      if (resources.size() != 1) {
        throw new CoreException(new Status(Status.ERROR, OwJsClosurePlugin.PLUGIN_ID, messages.getString("ClosureCompilerLaunchConfigurationDelegate_missingOutputFile")));
      }
      IResource resource = resources.get(0);
      if (resource instanceof IContainer) {
        return ((IContainer) resource).getFile(new Path("out.jsc"));
      } else if (resource instanceof IFile) {
        return ResourcesPlugin.getWorkspace().getRoot().getFile(resource.getFullPath().removeFileExtension().addFileExtension("jsc"));
      } else {
        assert false;
        return null;
      }
    } else {
      return ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(record.outputFile.get(store)));
    }
  }

  /**
   * Get the projects the resources in {@code resources} belong to.  Include recursively
   * their referenced projects. 
   */
  private Collection<IProject> getProjects(Iterable<IResource> resources) throws CoreException {
    Set<IProject> projects = new HashSet<IProject>();
    for (IResource resource: resources) {
      projects.add(resource.getProject());
    }
    return projects;
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
