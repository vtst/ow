package net.vtst.ow.closure.compiler.compile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.vtst.ow.closure.compiler.deps.AstFactory;

import com.google.javascript.jscomp.CommandLineRunner;
import com.google.javascript.jscomp.CompilerInput;
import com.google.javascript.jscomp.JSModule;
import com.google.javascript.jscomp.SourceFile;

public class DefaultExternsProvider {
  
  private static List<AstFactory> astFactories;

  /**
   * Get the externs as a {@code JSModule}.  Note that the included compiler inputs are <b>not</b> marked
   * as externs.
   * @return  The module containing the externs.
   */
  public static JSModule getAsModule() {
    if (astFactories == null) initialize();
    JSModule module = new JSModule("externs");
    for (AstFactory astFactory: astFactories) module.add(new CompilerInput(astFactory.getClone(false)));
    return module;
  }
  
  /**
   * @return  the externs as a list of {@code CompilerInput}.
   */
  public static List<CompilerInput> getAsCompilerInputs() {
    if (astFactories == null) initialize();
    List<CompilerInput> result = new ArrayList<CompilerInput>(astFactories.size());
    for (AstFactory astFactory: astFactories) result.add(new CompilerInput(astFactory.getClone(false), true));
    return result;    
  }
  
  public static List<SourceFile> getAsSourceFiles() throws IOException {
    return CommandLineRunner.getDefaultExterns();
  }
  
  private static synchronized void initialize() {
    if (astFactories != null) return;
    try {
      astFactories = loadExterns();
    } catch (IOException e) {
      astFactories = Collections.emptyList();
    }
  }
  
  private static List<AstFactory> loadExterns() throws IOException {
    List<SourceFile> externs = getAsSourceFiles();
    ArrayList<AstFactory> astFactories = new ArrayList<AstFactory>(externs.size());
    for (SourceFile extern: externs) astFactories.add(new AstFactory(extern));
    return astFactories;
  }
  
}
