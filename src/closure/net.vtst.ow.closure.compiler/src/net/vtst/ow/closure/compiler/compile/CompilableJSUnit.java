package net.vtst.ow.closure.compiler.compile;

import java.io.File;
import java.util.Collections;
import java.util.List;

import net.vtst.ow.closure.compiler.deps.JSExtern;
import net.vtst.ow.closure.compiler.deps.JSProject;
import net.vtst.ow.closure.compiler.deps.JSUnit;
import net.vtst.ow.closure.compiler.deps.JSUnitProvider.IProvider;

import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.ErrorManager;

/**
 * This class extends {@code JSUnit} with methods to perform complete and incremental
 * compilations.
 * <br>
 * <b>Thread safety:</b>  {@code fullCompile} is not thread safe, it shall be called from
 * a single thread.  {@code getLastAvailableCompilerRun} is thread safe because it returns
 * a member which is atomically updated.
 * @author Vincent Simonet
 */
public class CompilableJSUnit extends JSUnit {

  private JSProject project;

  public CompilableJSUnit(JSProject project, File path, File pathOfClosureBase, IProvider provider) {
    super(path, pathOfClosureBase, provider);
    this.project = project;
  }
  
  // **************************************************************************
  // Compilation
  
  private CompilerRun run = null;
  private List<JSUnit> orderedUnits = Collections.emptyList();
  private long allDependenciesModificationStamp = -2;
  private List<JSExtern> externs = Collections.emptyList();
  
  /**
   * Clear the cached dependencies and externs. 
   */
  public void clear() {
    allDependenciesModificationStamp = -2;
  }
  
  private long getMaxDependenciesModificationStamp() {
    long result = -1;
    for (JSUnit unit: orderedUnits) {
      if (unit.getDependenciesModificationStamp() > result)
        result = unit.getDependenciesModificationStamp();
    }
    return result;
  }
  
  private List<JSUnit> updateAndGetOrderedUnits() {
    long maxDependenciesModificationStamp = getMaxDependenciesModificationStamp();
    if (allDependenciesModificationStamp < maxDependenciesModificationStamp) {
      allDependenciesModificationStamp = maxDependenciesModificationStamp;
      orderedUnits = project.getSortedDependenciesOf(this);
    }
    return orderedUnits;
  }
  
  public CompilerRun fullCompile(
      CompilerOptions options, ErrorManager errorManager, 
      boolean keepCompilationResultsInMemory, boolean stripIncludedFiles) {
    return fullCompile(options, errorManager, keepCompilationResultsInMemory, stripIncludedFiles, true);
  }
  
  public CompilerRun fullCompile(
      CompilerOptions options, ErrorManager errorManager, 
      boolean keepCompilationResultsInMemory, boolean stripIncludedFiles, boolean force) {
    List<JSUnit> orderedUnits = updateAndGetOrderedUnits();
    // A full run is required if one of the following conditions happen:
    // - force,
    // - there is no previous run,
    // - the unit has changed since the last compilation,
    // - the previous run did not keep compilation results in memory, and one now requires to keep
    //   them.
    if (force || run == null || run.hasChanged(orderedUnits) ||
        (keepCompilationResultsInMemory && !run.getKeepCompilationResultsInMemory())) {
      CompilerRun newRun = new CompilerRun(
          this.getName(), options, errorManager, project.getExterns(),
          orderedUnits, Collections.<JSUnit>singleton(this), keepCompilationResultsInMemory, stripIncludedFiles);
      run = newRun;  // This is atomic
      return newRun;    
    } else {
      return run;
    }
  }
  
  public CompilerRun getLastAvailableCompilerRun() {
    return run;
  }

}
