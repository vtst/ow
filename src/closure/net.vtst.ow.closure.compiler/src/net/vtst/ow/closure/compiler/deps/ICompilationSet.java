package net.vtst.ow.closure.compiler.deps;

import com.google.javascript.jscomp.AbstractCompiler;

/**
 * A compilation set represents a set of compilation units, which can be passed to the compiler.
 * @author Vincent Simonet
 */
public interface ICompilationSet {
  
  /**
   * Get the compilation unit which provides a given name (namespace or class).
   * @param name  The namespace or class.
   * @return  The compilation unit (or null if not found).
   */
  public CompilationUnit getProvider(String name);
  
  /**
   * Update the dependency map with the current state of the compilation units.
   * @param compiler  The compiler used to report errors.
   * @return  true if some dependencies have changed, false otherwise.
   */
  public boolean updateDependencies(AbstractCompiler compiler);

}
