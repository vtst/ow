package net.vtst.ow.closure.compiler.strip;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;

import net.vtst.ow.closure.compiler.util.CompilerUtils;

import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.CustomPassExecutionTime;
import com.google.javascript.jscomp.ErrorManager;
import com.google.javascript.jscomp.JSSourceFile;

/**
 * JavaScript processor which removes private definitions and contents of functions, while
 * preserving JSDoc comments.
 * 
 * @author Vincent Simonet
 */
public class JSFileStripper {
  
  private Compiler compiler;
  private CompilerOptions options;
  private StripCompilerPass stripCompilerPass;
  private ErrorManager errorManager;
  
  public JSFileStripper(ErrorManager errorManager) {
    this.errorManager = errorManager;
  }
  
  public void setupCompiler(Writer writer) {
    compiler = CompilerUtils.makeCompiler(errorManager);
    options = CompilerUtils.makeOptions();
    stripCompilerPass = new StripCompilerPass(compiler, writer);
    CompilerUtils.addCustomCompilerPass(options, stripCompilerPass, CustomPassExecutionTime.BEFORE_OPTIMIZATIONS);
  }
  
  /**
   * Run the processor on a JavaScript file.
   * @param input  The input JavaScript file.
   * @param output  The output JavaScript file.
   * @return  The array of errors reported by the JavaScript compiler.
   * @throws IOException
   */
  public void strip(File input, File output) throws IOException {
    JSSourceFile jsInput = JSSourceFile.fromFile(input.getAbsolutePath());
    Writer writer = new BufferedWriter(new FileWriter(output));
    setupCompiler(writer);
    compiler.compile(
        Collections.<JSSourceFile> emptyList(),
        Collections.singletonList(jsInput), 
        options);
    if (stripCompilerPass.getException() != null) throw stripCompilerPass.getException();
    writer.close();
  }
  
}
