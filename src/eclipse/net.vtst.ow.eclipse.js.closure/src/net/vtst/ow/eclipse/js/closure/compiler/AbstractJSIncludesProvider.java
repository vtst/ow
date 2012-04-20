package net.vtst.ow.eclipse.js.closure.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.vtst.eclipse.easy.ui.properties.stores.IReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.ProjectPropertyStore;
import net.vtst.ow.closure.compiler.compile.DefaultExternsProvider;
import net.vtst.ow.closure.compiler.deps.AbstractJSProject;
import net.vtst.ow.closure.compiler.deps.JSExtern;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;
import net.vtst.ow.eclipse.js.closure.properties.ClosureProjectPropertyRecord;
import net.vtst.ow.eclipse.js.closure.util.Utils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.google.javascript.jscomp.AbstractCompiler;

public abstract class AbstractJSIncludesProvider implements IJSIncludesProvider {
  
  private ClosureProjectPropertyRecord record = ClosureProjectPropertyRecord.getInstance();
  
  /**
   * Keys identifying libraries.
   */
  protected static class JSLibraryKey {
    private File libraryPath;
    private File pathOfClosureBase;
    public JSLibraryKey(File libraryPath, File pathOfClosureBase) {
      this.libraryPath = libraryPath;
      this.pathOfClosureBase = pathOfClosureBase;
    }
    public boolean equals(Object obj) {
      if (obj instanceof JSLibraryKey) {
        JSLibraryKey key = (JSLibraryKey) obj;
        return (
            key.libraryPath.equals(libraryPath) && 
            key.pathOfClosureBase.equals(pathOfClosureBase));
      }
      return false;
    }
    public int hashCode() {
      int h1 = libraryPath.hashCode();
      int h2 = pathOfClosureBase.hashCode();
      return h1 * h2 + h1 + h2;
    }
  }
  
  // Getting libraries
  
  private void addLibraries(
      AbstractCompiler compiler, IProgressMonitor monitor, IReadOnlyStore store, 
      Set<JSLibraryKey> alreadyAdded, List<AbstractJSProject> result) throws CoreException {
    File pathOfClosureBase = ClosureCompiler.getPathOfClosureBase(store);
    if (pathOfClosureBase != null) {
      if (alreadyAdded == null || alreadyAdded.add(new JSLibraryKey(pathOfClosureBase, pathOfClosureBase)));
        result.add(this.getLibrary(compiler, pathOfClosureBase, pathOfClosureBase));
    }
    for (File libraryPath: record.includes.otherLibraries.get(store)) {
      if (monitor != null) Utils.checkCancel(monitor);
      if (alreadyAdded == null || alreadyAdded.add(new JSLibraryKey(libraryPath, pathOfClosureBase)));
        result.add(this.getLibrary(compiler, libraryPath, pathOfClosureBase));
    }
    
  }
  
  public List<AbstractJSProject> getLibraries(AbstractCompiler compiler, IProgressMonitor monitor, ArrayList<IProject> projects) throws CoreException {
    List<AbstractJSProject> result = new ArrayList<AbstractJSProject>();
    Set<JSLibraryKey> keys = new HashSet<JSLibraryKey>();
    for (int i = projects.size() - 1; i >= 0; --i) {
      addLibraries(
          compiler, monitor, 
          new ProjectPropertyStore(projects.get(i), OwJsClosurePlugin.PLUGIN_ID), keys, result);
    }
    return result;
  }
  
  public List<AbstractJSProject> getLibraries(AbstractCompiler compiler, IProgressMonitor monitor, IReadOnlyStore store) throws CoreException {
    List<AbstractJSProject> result = new ArrayList<AbstractJSProject>();
    addLibraries(compiler, monitor, store, null, result);
    return result;    
  }

  // Getting externs
  
  private static File defaultExterns = new File("///default-externs///");
  
  private void addExterns(
      AbstractCompiler compiler, IProgressMonitor monitor, IReadOnlyStore store, 
      Set<File> alreadyAdded, List<JSExtern> result) throws CoreException {
    if (!record.includes.useOnlyCustomExterns.get(store)) {
      if (alreadyAdded == null || alreadyAdded.add(defaultExterns)) {
        result.addAll(DefaultExternsProvider.getAsJSExterns());
      }
    }
    for (File externPath: record.includes.externs.get(store)) {
      if (monitor != null) Utils.checkCancel(monitor);
      if (alreadyAdded == null || alreadyAdded.add(externPath));
        result.add(this.getExtern(compiler, externPath));
    }
    
  }
  
  public List<JSExtern> getExterns(AbstractCompiler compiler, IProgressMonitor monitor, ArrayList<IProject> projects) throws CoreException {
    List<JSExtern> result = new ArrayList<JSExtern>();
    Set<File> keys = new HashSet<File>();
    for (int i = projects.size() - 1; i >= 0; --i) {
      addExterns(
          compiler, monitor, 
          new ProjectPropertyStore(projects.get(i), OwJsClosurePlugin.PLUGIN_ID), keys, result);
    }
    return result;
  }
  
  public List<JSExtern> getExterns(AbstractCompiler compiler, IProgressMonitor monitor, IReadOnlyStore store) throws CoreException {
    List<JSExtern> result = new ArrayList<JSExtern>();
    addExterns(compiler, monitor, store, null, result);
    return result;    
  }

}
