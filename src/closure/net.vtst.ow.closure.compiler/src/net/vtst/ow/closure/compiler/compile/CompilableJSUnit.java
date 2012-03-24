package net.vtst.ow.closure.compiler.compile;

import java.io.File;
import java.util.List;

import net.vtst.ow.closure.compiler.deps.JSProject;
import net.vtst.ow.closure.compiler.deps.JSUnit;
import net.vtst.ow.closure.compiler.deps.JSUnitProvider.IProvider;

import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.ErrorManager;

/**
 * This class extends {@code JSUnit} with methods to perform complete and incremental
 * compilations.
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
  
  public CompilerRun fullCompile(CompilerOptions options, ErrorManager errorManager) {
    return fullCompile(options, errorManager, true);
  }
  
  public CompilerRun fullCompile(CompilerOptions options, ErrorManager errorManager, boolean force) {
    List<JSUnit> units = project.getSortedDependenciesOf(this);
    units.add(this);
    if (force || run == null || run.hasChanged(units)) {
      CompilerRun newRun = new CompilerRun(this.getName(), options, errorManager, units);
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
