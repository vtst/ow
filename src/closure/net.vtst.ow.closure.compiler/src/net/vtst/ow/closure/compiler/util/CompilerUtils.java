package net.vtst.ow.closure.compiler.util;

import java.io.File;
import java.io.PrintStream;

import com.google.common.collect.ArrayListMultimap;
import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.CompilerPass;
import com.google.javascript.jscomp.CustomPassExecutionTime;
import com.google.javascript.jscomp.ErrorManager;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.PrintStreamErrorManager;

/**
 * Helper functions to manage JavaScript compilers.
 * @author Vincent Simonet
 */
public class CompilerUtils {

  // TODO: This should be moved somewhere else
  public static boolean isJavaScriptFile(File file) {
    return file.getPath().endsWith(".js");
  }

  /**
   * Create a new error manager that prints error on a stream.
   * @param printStream  The output stream where errors are printed.
   * @return  The new error manager.
   */
  public static ErrorManager makePrintingErrorManager(PrintStream printStream) {
    return new PrintStreamErrorManager(printStream);
  }

  /**
   * Create a new compiler object, using a given error manager.
   * @param errorManager  The error manager the compiler will be connected to.
   * @return  The new compiler.
   */
  public static Compiler makeCompiler(ErrorManager errorManager) {
    Compiler compiler = new Compiler(errorManager);
    compiler.disableThreads();  // TODO If this useful or needed?
    return compiler;
  }  

  private static void setupOptions(CompilerOptions options) {
    options.ideMode = true;
    options.setRewriteNewDateGoogNow(false);
    options.setRemoveAbstractMethods(false);    
  }
  
  /**
   * Create a new compiler options object, and set the default options we need.
   * @return  The new compiler options.
   */
  public static CompilerOptions makeOptions() {
    // These options should remain minimal, because they are used by the stripper.
    CompilerOptions options = new CompilerOptions();
    setupOptions(options);
    return options;
  }

  /**
   * Add a custom compiler pass to a compiler options.
   * @param options  The compiler options to which the custom pass will be added.
   * @param pass  The compiler pass to add.
   * @param executionTime  The execution time for the compiler pass.
   */
  public static void addCustomCompilerPass(
      CompilerOptions options, CompilerPass pass, CustomPassExecutionTime executionTime) {
    if (options.customPasses == null) options.customPasses = ArrayListMultimap.create();
    options.customPasses.put(executionTime, pass);
  }
  
  /**
   * Report an error via an error manager.
   * @param manager  The error manager to use for reporting the error.
   * @param error  The error to report.
   */
  public static void reportError(ErrorManager manager, JSError error) {
    manager.report(error.level, error);
  }
  
  /**
   * Report an error via the error manager of a compiler.
   * @param compiler  The compiler whose error manager will be used for reporting the error.
   * @param error  The error to report.
   */
  public static void reportError(AbstractCompiler compiler, JSError error) {
    reportError(compiler.getErrorManager(), error);
  }
}
