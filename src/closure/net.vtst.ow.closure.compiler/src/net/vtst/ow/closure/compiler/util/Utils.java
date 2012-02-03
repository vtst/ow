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

public class Utils {

  public static boolean isJavaScriptFile(File file) {
    return file.getPath().endsWith(".js");
  }
    
  public static void addCustomCompilerPass(
      CompilerOptions options, CompilerPass pass, CustomPassExecutionTime executionTime) {
    if (options.customPasses == null) options.customPasses = ArrayListMultimap.create();
    options.customPasses.put(executionTime, pass);
  }

  public static CompilerOptions makeOptions() {
    CompilerOptions options = new CompilerOptions();
    options.ideMode = true;
    options.setRewriteNewDateGoogNow(false);
    options.setRemoveAbstractMethods(false);
    return options;
  }
  
  public static ErrorManager makeErrorManager(PrintStream printStream) {
    return new PrintStreamErrorManager(printStream);
  }
  
  public static void reportError(ErrorManager manager, JSError error) {
    manager.report(error.level, error);
  }
  
  public static void reportError(AbstractCompiler compiler, JSError error) {
    reportError(compiler.getErrorManager(), error);
  }
  
  public static Compiler makeCompiler(ErrorManager errorManager) {
    Compiler compiler = new Compiler(errorManager);
    compiler.disableThreads();  // TODO If this useful or needed?
    return compiler;
  }
  
}
