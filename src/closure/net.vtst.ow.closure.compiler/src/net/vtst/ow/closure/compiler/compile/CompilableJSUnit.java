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
  
  public CompilerRun fullCompile(CompilerOptions options, ErrorManager errorManager, boolean stripIncludedFiles) {
      return fullCompile(options, errorManager, stripIncludedFiles, true, false, false);
	  }
	  
  public CompilerRun fullCompile(CompilerOptions options, ErrorManager errorManager, boolean stripIncludedFiles, boolean force, boolean singleCompile, boolean compile) {
    if (singleCompile) {
      run = null;
      if (!compile)
    	  return null;
    }
    List<JSUnit> orderedUnits = updateAndGetOrderedUnits();
    if (compile || force || run == null || run.hasChanged(orderedUnits)) {
      CompilerRun newRun = new CompilerRun(
          this.getName(), options, errorManager, project.getExterns(),
          orderedUnits, Collections.<JSUnit>singleton(this), stripIncludedFiles);
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
