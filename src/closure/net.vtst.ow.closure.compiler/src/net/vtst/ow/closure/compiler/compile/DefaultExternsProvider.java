package net.vtst.ow.closure.compiler.compile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.vtst.ow.closure.compiler.deps.AstFactory;
import net.vtst.ow.closure.compiler.deps.JSExtern;

import com.google.javascript.jscomp.CommandLineRunner;
import com.google.javascript.jscomp.CompilerInput;
import com.google.javascript.jscomp.JSModule;
import com.google.javascript.jscomp.SourceFile;

/**
 * Provides the default externs (which are embeded in the compiler's JAR) under various form.
 * The default externs' AST are cached, so that they are read only once.
 * @author Vincent Simonet
 */
public class DefaultExternsProvider {
  
  private static List<JSExtern> externs;

  /**
   * Get the externs as a {@code JSModule}.  Note that the included compiler inputs are <b>not</b> marked
   * as externs.
   * @return  The module containing the externs.
   */
  public static JSModule getAsModule() {
    if (externs == null) initialize();
    JSModule module = new JSModule("externs");
    for (AstFactory astFactory: externs) module.add(new CompilerInput(astFactory.getClone(false)));
    return module;
  }


  /**
   * Get the default externs as a list of {@code JSExtern}.
   */
  public static List<JSExtern> getAsJSExterns() {
    if (externs == null) initialize();
    return externs;
  }

  /**
   * @return  the externs as a list of {@code CompilerInput}.
   */
  public static List<CompilerInput> getAsCompilerInputs() {
    if (externs == null) initialize();
    List<CompilerInput> result = new ArrayList<CompilerInput>(externs.size());
    for (JSExtern extern: externs) result.add(new CompilerInput(extern.getClone(false), true));
    return result;    
  }
  
  /**
   * @return The default externs, as a list of source files.
   * @throws IOException
   */
  public static List<SourceFile> getAsSourceFiles() throws IOException {
    return CommandLineRunner.getDefaultExterns();
  }
  
  /**
   * Initialized the cached AST factories if they are not yet initialized.
   */
  private static synchronized void initialize() {
    if (externs != null) return;
    try {
      externs = loadExterns();
    } catch (IOException e) {
      externs = Collections.emptyList();
    }
  }
  
  /**
   * @return The default externs, as a list of AST factories.
   * @throws IOException
   */
  private static List<JSExtern> loadExterns() throws IOException {
    List<SourceFile> sourceFiles = getAsSourceFiles();
    ArrayList<JSExtern> externs = new ArrayList<JSExtern>(sourceFiles.size());
    for (SourceFile sourceFile: sourceFiles) externs.add(new JSExtern(sourceFile));
    return externs;
  }
  
}
