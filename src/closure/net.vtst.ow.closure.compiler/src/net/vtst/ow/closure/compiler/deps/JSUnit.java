package net.vtst.ow.closure.compiler.deps;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.vtst.ow.closure.compiler.util.FileUtils;
import net.vtst.ow.closure.compiler.util.TimestampKeeper;

import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.JsAst;
import com.google.javascript.jscomp.deps.DependencyInfo;
import com.google.javascript.rhino.Node;

/**
 * A compilation unit stands for a JavaScript file which can be passed to the compiler.
 * Note that the current implementation relies on physical identity: do not create two CompilationUnit
 * objects for the same file!
 * <br>
 * <b>Thread safety:</b>  This class maintain the AST and the dependencies for the unit as an internal
 * state.  In order to ensure safe concurrent accesses, the public methods manipulating them are 
 * synchronized.  The provides/requires are thread safe as they are immutable collections.
 * @author Vincent Simonet
 */
public class JSUnit implements DependencyInfo {
    
  private File path;
  private File pathOfClosureBase;
  private JSUnitProvider.IProvider provider;
  private File pathRelativeToClosureBase;
  private TimestampKeeper timestampKeeperForDependencies;  // Timestamp for the source modification
  private long dependenciesModificationStamp = -1;  // Timestamp for the last modification of the dependencies
  private AstFactoryFromModifiable astFactory;
  private Collection<String> providedNames;
  private Collection<String> requiredNames;

  /**
   * Create a compilation unit from a provider.
   * @param fileName  The name for the compilation unit.
   * @param pathOfClosureBase  The path of the closure base directory.
   * @param provider  The provider for the compilation unit.
   */
  public JSUnit(File path, File pathOfClosureBase, JSUnitProvider.IProvider provider) {
    this.path = path;
    this.pathOfClosureBase = pathOfClosureBase;
    this.provider = provider;
    this.timestampKeeperForDependencies = new TimestampKeeper(provider);
    this.astFactory = new AstFactoryFromModifiable(getName(), provider);
  }
  
  /**
   * Constructor which is useful when the dependencies are known at creation time,
   * e.g. when the unit is created from a library which has a deps.js file.
   */
  public JSUnit(
      File path, File pathOfClosureBase, JSUnitProvider.IProvider provider,
      Set<String> providedNames, Set<String> requiredNames) {
    this(path, pathOfClosureBase, provider);
    setDependencies(providedNames, requiredNames, -1);
  }

  /**
   * Get the name of the compilation unit.
   * @return
   */
  public String getName() {
    return path.getPath();
  }
  
  /** Gets the path of this file relative to Closure's base.js file. */
  public String getPathRelativeToClosureBase() {
    if (pathRelativeToClosureBase == null) {
      pathRelativeToClosureBase = FileUtils.makeRelative(pathOfClosureBase, path);
    }
    return pathRelativeToClosureBase.getPath();
  }
  
  public long lastModified() {
    return provider.lastModified();
  }  
  
  // **************************************************************************
  // Dependencies
  
  /**
   * Get the names (namespaces and classes) provided by the compilation unit.
   * @return  The collection of the names.
   */
  public Collection<String> getProvides() {
    return Collections.unmodifiableCollection(providedNames);
  }
  
  /**
   * Add a provided name.  This method is <b>not</b> thread safe.  It should not be called if the
   * object can be accessed from several threads.
   * @param name
   */
  void addProvide(String name) {
    providedNames.add(name);
  }
  
  /**
   * Get the names (namespaces and classes) required by the compilation unit.
   * @return  The collection of the names.
   */
  public Collection<String> getRequires() {
    return Collections.unmodifiableCollection(requiredNames);
  }

  /**
   * Add a required name.  This method is <b>not</b> thread safe.  It should not be called if the
   * object can be accessed from several threads.
   * @param name
   */
  void addRequire(String name) {
    requiredNames.add(name);
  }

  /**
   * Update the sets of provided and required names from the current code.
   * @param compiler  The compiler used to report errors.
   * @return  true if the dependencies have changed since the last update.
   */
  public synchronized boolean updateDependencies(AbstractCompiler compiler) {
    if (!timestampKeeperForDependencies.hasChanged()) return false;
    long modificationStamp = timestampKeeperForDependencies.getModificationStamp();
    // There is no need to make a clone, as this pass does not modify the AST.
    Node root = astFactory.getAstRoot(compiler);
    Set<String> newProvidedNames = new HashSet<String>();
    Set<String> newRequiredNames = new HashSet<String>();
    GetDependenciesNodeTraversal.run(compiler, root, newProvidedNames, newRequiredNames);
    if (newProvidedNames.equals(providedNames) &&
        newRequiredNames.equals(requiredNames)) {
      return false;
    }
    setDependencies(newProvidedNames, newRequiredNames, modificationStamp);
    return true;
  }

  /**
   * Internal version of {@code setDependencies}, which does not update the timestamp keeper.
   * @param providedNames
   * @param requiredNames
   * @param modificationStamp  The modification stamp for the dependencies, i.e. the last modification
   *   date of the file at the time the dependencies were computed.
   */
  private void setDependencies(Set<String> providedNames, Set<String> requiredNames, long modificationStamp) {
    this.dependenciesModificationStamp = modificationStamp;
    this.providedNames = providedNames;  // Atomic set
    this.requiredNames = requiredNames;  // Atomic set
  }
  
  public long getDependenciesModificationStamp() {
    return dependenciesModificationStamp;
  }

  // **************************************************************************
  // Source / AST

  /**
   * Get a clone of the AST for the file.
   */
  // TODO: Is this thread safe (when the AST is parsed?)
  public synchronized JsAst getAst(boolean stripped) {
    return astFactory.getClone(stripped);
  }
  
  // **************************************************************************
  // Dependency order

  /**
   * This is a placeholder for the containing project.  It should not be used 
   * elsewhere.
   */
  public int dependencyIndex;
}
