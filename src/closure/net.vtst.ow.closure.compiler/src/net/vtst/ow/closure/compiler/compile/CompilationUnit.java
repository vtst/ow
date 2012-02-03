package net.vtst.ow.closure.compiler.compile;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.SourceAst;
import com.google.javascript.rhino.Node;

/**
 * A compilation unit stands for a JavaScript file which can be passed to the compiler.
 * Note that the current implementation relies on physical identity: do not create two CompilationUnit
 * objects for the same file!
 * @author Vincent Simonet
 */
public class CompilationUnit {
    
  private String fileName;
  private TimestampKeeper timestampKeeper;
  private AstFactory astFactory;
  private Set<String> providedNames = new HashSet<String>();
  private Set<String> requiredNames = new HashSet<String>();

  /**
   * Create a compilation unit from a provider.
   * @param fileName  The name for the compilation unit.
   * @param provider  The provider for the compilation unit.
   */
  public CompilationUnit(String fileName, CompilationUnitProvider.Interface provider) {
    this.fileName = fileName;
    this.timestampKeeper = new TimestampKeeper(provider);
    this.astFactory = new AstFactory(fileName, provider);
  }
  
  /**
   * Get the names (namespaces and classes) provided by the compilation unit.
   * @return  The collection of the names.
   */
  public Collection<String> getProvidedNames() {
    return providedNames;
  }
  
  /**
   * Get the names (namespaces and classes) required by the compilation unit.
   * @return  The collection of the names.
   */
  public Collection<String> getRequiredNames() {
    return requiredNames;
  }
  
  /**
   * Update the sets of provided and required names from the current code.
   * @param compiler  The compiler used to report errors.
   * @return  true if the dependencies have changed since the last update.
   */
  public boolean updateDependencies(AbstractCompiler compiler) {
    if (!timestampKeeper.hasChanged()) return false;
    // There is no need to make a clone, as this pass does not modify the AST.
    Node root = astFactory.getAstRoot(compiler);
    Set<String> newProvidedNames = new HashSet<String>();
    Set<String> newRequiredNames = new HashSet<String>();
    GetDependenciesNodeTraversal.run(compiler, root, newProvidedNames, newRequiredNames);
    if (newProvidedNames.equals(providedNames) &&
        newRequiredNames.equals(requiredNames)) return false;
    providedNames = newProvidedNames;
    requiredNames = newRequiredNames;
    return true;
  }
    
  /**
   * Set the dependencies for the compilation unit.  This is useful if they are loaded from a deps.js
   * file, instead of being computed from the source file.
   * @param providedNames
   * @param requiredNames
   */
  public void setDependencies(Collection<String> providedNames, Collection<String> requiredNames) {
    this.timestampKeeper.sync();
    this.providedNames.clear();
    this.providedNames.addAll(providedNames);
    this.requiredNames.clear();
    this.requiredNames.addAll(requiredNames);
  }
  
  /**
   * Get a clone of the AST for the file.
   * @return
   */
  public SourceAst getAst() {
    return astFactory.getClone();
  }

  /**
   * Get the name of the compilation unit.
   * @return
   */
  public String getName() {
    return fileName;
  }

}
