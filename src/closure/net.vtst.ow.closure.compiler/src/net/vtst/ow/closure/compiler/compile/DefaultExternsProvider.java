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

  public static JSModule get() {
    if (astFactories == null) initialize();
    JSModule module = new JSModule("externs");
    for (AstFactory astFactory: astFactories) module.add(new CompilerInput(astFactory.getClone()));
    return module;
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
    List<SourceFile> externs = CommandLineRunner.getDefaultExterns();
    ArrayList<AstFactory> astFactories = new ArrayList<AstFactory>(externs.size());
    for (SourceFile extern: externs) astFactories.add(new AstFactory(extern));
    return astFactories;
  }
  
}
