package net.vtst.ow.eclipse.js.closure.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import net.vtst.eclipse.easy.ui.properties.stores.IReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.ProjectPropertyStore;
import net.vtst.ow.closure.compiler.deps.AbstractJSProject;
import net.vtst.ow.closure.compiler.deps.JSExtern;
import net.vtst.ow.closure.compiler.deps.JSLibrary;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;
import net.vtst.ow.eclipse.js.closure.compiler.AbstractJSIncludesProvider.JSLibraryKey;

import com.google.javascript.jscomp.AbstractCompiler;

/**
 * Interface for providing JavaScript includes, which may be libraries or externs.
 * @author Vincent Simonet
 */
public interface IJSIncludesProvider {

  /**
   * Get a library from the provider.
   * @param compiler  The compiler used for parsing and to report potential errors.
   * @param libraryPath  The base path of the library.
   * @param pathOfClosureBase  The Closure base path for this library.
   * @param isClosureBase  Whether this library is the closure base library.
   * @return  The library.
   */
  public JSLibrary getLibrary(AbstractCompiler compiler, File libraryPath, File pathOfClosureBase);

  public List<AbstractJSProject> getLibraries(AbstractCompiler compiler, IProgressMonitor monitor, ArrayList<IProject> projects) throws CoreException;
  public List<AbstractJSProject> getLibraries(AbstractCompiler compiler, IProgressMonitor monitor, IReadOnlyStore store) throws CoreException;

  /**
   * Get the {@code JSUnit} for an extern file.
   * @param compiler  The compiler used for parsing and to report potential errors.
   * @param path  The path of the extern file.
   * @return  The unit for the extern file.
   */
  public JSExtern getExtern(AbstractCompiler compiler, File path);
  
  public List<JSExtern> getExterns(AbstractCompiler compiler, IProgressMonitor monitor, ArrayList<IProject> projects) throws CoreException;  
  public List<JSExtern> getExterns(AbstractCompiler compiler, IProgressMonitor monitor, IReadOnlyStore store) throws CoreException;

  /**
   * Clear any cached data of the provider.
   */
  public void clear();
}
