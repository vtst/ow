package net.vtst.ow.closure.compiler.compile;

import java.io.File;

import net.vtst.ow.closure.compiler.deps.JSSet;
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

  private JSSet<?> compilationSet;

  public CompilableJSUnit(JSSet<?> compilationSet, File path, File pathOfClosureBase, IProvider provider) {
    super(path, pathOfClosureBase, provider);
    this.compilationSet = compilationSet;
  }
  
  public JSSet<?> getJSSet() {
    return compilationSet;
  }

  // **************************************************************************
  // Compilation
  
  private CompilerRun run = null;
  
  public CompilerRun compile(CompilerOptions options, ErrorManager errorManager) {
    CompilerRun newRun = new CompilerRun(options, errorManager, this);
    run = newRun;  // This is atomic
    return newRun;
  }
  
  public CompilerRun getLastAvailableCompilerRun() {
    return run;
  }

}
