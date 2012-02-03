package net.vtst.ow.closure.compiler.compile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import net.vtst.ow.closure.compiler.util.BidiHashMap;
import net.vtst.ow.closure.compiler.util.Utils;

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
 */
public class CompilationSet implements ICompilationSet {
  
  static final DiagnosticType OW_DUPLICATED_GOOG_PROVIDE = DiagnosticType.warning(
      "OW_DUPLICATED_GOOG_PROVIDE",
      "The package \"{0}\" is already provided by \"{1}\"");

  static final DiagnosticType OW_UNDEFINED_PACKAGE = DiagnosticType.warning(
      "OW_UNDEFINED_PACKAGE",
      "The package \"{0}\" is not provided");
  
  private Collection<CompilationUnit> compilationUnits = new ArrayList<CompilationUnit>();
  private Collection<ICompilationSet> compilationSets = new ArrayList<ICompilationSet>();
  private BidiHashMap<String, CompilationUnit> providedBy = new BidiHashMap<String, CompilationUnit>();
  
  /* (non-Javadoc)
   * @see net.vtst.ow.closure.compiler.compile.ICompilationSet#getProvider(java.lang.String)
   */
  public CompilationUnit getProvider(String name) {
    CompilationUnit compilationUnit = providedBy.get(name);
    if (compilationUnit != null) return compilationUnit;
    for (ICompilationSet compilationSet: compilationSets) {
      compilationUnit = compilationSet.getProvider(name);
      if (compilationUnit != null) return compilationUnit;
    }
    return null;
  }
  
  /**
   * Add a compilation unit to the compilation set.  Does not recompute dependencies.
   * @param compilationUnit  The compilation unit to be added.
   */
  public void addCompilationUnit(CompilationUnit compilationUnit) {
    compilationUnits.add(compilationUnit);
  }
  
  /**
   * Add a delegated compilation set.  Does not recompute dependencies.
   * @param compilationSet  The delegated compilation set.
   */
  public void addCompilationSet(ICompilationSet compilationSet) {
    compilationSets.add(compilationSet);
  }
  
  /* (non-Javadoc)
   * @see net.vtst.ow.closure.compiler.compile.ICompilationSet#updateDependencies(com.google.javascript.jscomp.AbstractCompiler)
   */
  public boolean updateDependencies(AbstractCompiler compiler) {
    boolean hasChanged = false;
    for (ICompilationSet compilationSet: compilationSets) {
      if (compilationSet.updateDependencies(compiler))
        hasChanged = true;
    }
    for (CompilationUnit compilationUnit: compilationUnits) {
      if (compilationUnit.updateDependencies(compiler)) {
        hasChanged = true;
        providedBy.removeAllKeysFor(compilationUnit);
        for (String name: compilationUnit.getProvidedNames()) {
          CompilationUnit previousCompilationUnit = providedBy.put(name, compilationUnit);
          if (previousCompilationUnit != null) {
            Utils.reportError(
                compiler, 
                JSError.make(previousCompilationUnit.getName(), 0, 0, OW_DUPLICATED_GOOG_PROVIDE, name, compilationUnit.getName()));            
          }
        }
      }
    }
    return hasChanged;
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
      Iterable<CompilationUnit> compilationUnits) {
    JSModule module = new JSModule(moduleName);
    LinkedList<CompilationUnit> toBeVisited = Lists.newLinkedList(compilationUnits);
    HashSet<CompilationUnit> visited = Sets.newHashSet(compilationUnits);
    LinkedList<CompilationUnit> requiredCompilationUnits = new LinkedList<CompilationUnit>();
    while (!toBeVisited.isEmpty()) {
      CompilationUnit compilationUnit = toBeVisited.removeFirst();
      requiredCompilationUnits.addFirst(compilationUnit);
      for (String name: compilationUnit.getRequiredNames()) {
        CompilationUnit requiredCompilationUnit = getProvider(name);
        if (requiredCompilationUnit == null) {
          Utils.reportError(
              compiler, 
              JSError.make(compilationUnit.getName(), 0, 0, OW_UNDEFINED_PACKAGE, name));                      
        } else {
          if (visited.add(requiredCompilationUnit)) toBeVisited.add(requiredCompilationUnit);
        }
      }
    }
    for (CompilationUnit compilationUnit: requiredCompilationUnits) {
      module.add(new CompilerInput(compilationUnit.getAst()));
    }
    return module;
  }

}
