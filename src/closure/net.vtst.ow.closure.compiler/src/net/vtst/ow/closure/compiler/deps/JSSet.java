package net.vtst.ow.closure.compiler.deps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.vtst.ow.closure.compiler.util.BidiHashMap;
import net.vtst.ow.closure.compiler.util.CompilerUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.CompilerInput;
import com.google.javascript.jscomp.DiagnosticType;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.JSModule;

/**
 * Concrete implementation of a compilation set, which may own some compilation unit, and contain
 * some delegated compilation sets.
 * @author Vincent Simonet
 *
 * @param <K>  Type of keys used to retrieve compilation units.
 */
public class JSSet<K> implements IJSSet {
  
  static final DiagnosticType OW_DUPLICATED_GOOG_PROVIDE = DiagnosticType.warning(
      "OW_DUPLICATED_GOOG_PROVIDE",
      "The package \"{0}\" is already provided by \"{1}\"");

  static final DiagnosticType OW_UNDEFINED_PACKAGE = DiagnosticType.warning(
      "OW_UNDEFINED_PACKAGE",
      "The package \"{0}\" is not provided");
  
  private Collection<JSUnit> compilationUnits = new ArrayList<JSUnit>();
  private Collection<IJSSet> compilationSets = new ArrayList<IJSSet>();
  private BidiHashMap<String, JSUnit> providedBy = new BidiHashMap<String, JSUnit>();
  private Map<K, JSUnit> keyToCompilationUnit = new HashMap<K, JSUnit>();
  
  /* (non-Javadoc)
   * @see net.vtst.ow.closure.compiler.compile.ICompilationSet#getProvider(java.lang.String)
   */
  public JSUnit getProvider(String name) {
    JSUnit compilationUnit = providedBy.get(name);
    if (compilationUnit != null) return compilationUnit;
    for (IJSSet compilationSet: compilationSets) {
      compilationUnit = compilationSet.getProvider(name);
      if (compilationUnit != null) return compilationUnit;
    }
    return null;
  }
  
  /**
   * Add a compilation unit to the compilation set.  Does not recompute dependencies.
   * @param compilationUnit  The compilation unit to be added.
   */
  public void addCompilationUnit(JSUnit compilationUnit) {
    compilationUnits.add(compilationUnit);
  }

  /**
   * Add a compilation unit to the compilation set.  Does not recompute dependencies.
   * @param key  The key to retrieve the compilation unit.
   * @param compilationUnit  The compilation unit to be added.
   */
  public void addCompilationUnit(K key, JSUnit compilationUnit) {
    keyToCompilationUnit.put(key, compilationUnit);
    addCompilationUnit(compilationUnit);
  }
  
  /**
   * Retrieve a compilation unit by key.
   * @param key
   * @return
   */
  public JSUnit getCompilationUnit(K key) {
    return keyToCompilationUnit.get(key);
  }
  
  public Set<K> keySet() {
    return keyToCompilationUnit.keySet();
  }

  /**
   * Add a delegated compilation set.  Does not recompute dependencies.
   * @param compilationSet  The delegated compilation set.
   */
  public void addCompilationSet(IJSSet compilationSet) {
    compilationSets.add(compilationSet);
  }

  public void clear() {
    keyToCompilationUnit.clear();
    compilationUnits.clear();
    compilationSets.clear();
    providedBy.clear();
  }
  
  /* (non-Javadoc)
   * @see net.vtst.ow.closure.compiler.compile.ICompilationSet#updateDependencies(com.google.javascript.jscomp.AbstractCompiler)
   */
  public boolean updateDependencies(AbstractCompiler compiler) {
    boolean hasChanged = false;
    for (IJSSet compilationSet: compilationSets) {
      if (compilationSet.updateDependencies(compiler))
        hasChanged = true;
    }
    for (JSUnit compilationUnit: compilationUnits) {
      if (compilationUnit.updateDependencies(compiler)) {
        hasChanged = true;
        providedBy.removeAllKeysFor(compilationUnit);
        for (String name: compilationUnit.getProvides()) {
          JSUnit previousCompilationUnit = providedBy.put(name, compilationUnit);
          if (previousCompilationUnit != null) {
            CompilerUtils.reportError(
                compiler, 
                JSError.make(previousCompilationUnit.getName(), 0, 0, OW_DUPLICATED_GOOG_PROVIDE, name, compilationUnit.getName()));            
          }
        }
      }
    }
    return hasChanged;
  }
  
  /**
   * Compute the list of compilation units which are required to compile a set of compilation units.
   * All the compilation units are taken from the compilation set (and its delegates).
   * The returned list is ordered according to the dependencies.
   * @param compiler  The compiler used to report errors.
   * @param compilationUnits  The compilation units to compile.
   * @return  The list of required compilation units (including those of {@code compilationUnits}.
   */
  public List<JSUnit> getRequiredCompilationUnits(
      AbstractCompiler compiler,
      Iterable<JSUnit> compilationUnits) {
    LinkedList<JSUnit> toBeVisited = Lists.newLinkedList(compilationUnits);
    HashSet<JSUnit> visited = Sets.newHashSet(compilationUnits);
    LinkedList<JSUnit> result = new LinkedList<JSUnit>();
    while (!toBeVisited.isEmpty()) {
      JSUnit compilationUnit = toBeVisited.removeFirst();
      result.addFirst(compilationUnit);
      for (String name: compilationUnit.getRequires()) {
        JSUnit requiredCompilationUnit = getProvider(name);
        if (requiredCompilationUnit == null) {
          CompilerUtils.reportError(
              compiler, 
              JSError.make(compilationUnit.getName(), 0, 0, OW_UNDEFINED_PACKAGE, name));                      
        } else {
          if (visited.add(requiredCompilationUnit)) toBeVisited.add(requiredCompilationUnit);
        }
      }
    }
    return result;
  }
  
  /**
   * Make a JSModule object which contains all compilation units necessary to compile a given
   * set of compilation units.
   * @param compiler  The compiler used to report errors.
   * @param moduleName  The name of the created module.
   * @param compilationUnits  The set of compilation units to compute (shall belong to the current
   *     compilation set).
   * @return  The created JSModule.
   */
  public JSModule makeJSModule(
      AbstractCompiler compiler,
      String moduleName, 
      Iterable<JSUnit> compilationUnits) {
    JSModule module = new JSModule(moduleName);
    for (JSUnit compilationUnit: getRequiredCompilationUnits(compiler, compilationUnits)) {
      module.add(new CompilerInput(compilationUnit.getAst()));
    }
    return module;
  }

}
