package net.vtst.ow.closure.compiler.deps;

import java.io.File;

import net.vtst.ow.closure.compiler.deps.JSLibrary.CacheMode;

import com.google.javascript.jscomp.JSSourceFile;
import com.google.javascript.jscomp.SourceFile;

public class JSExtern extends AstFactory {
  private static final long serialVersionUID = 1L;

  public JSExtern(SourceFile sourceFile) {
    super(sourceFile);
  }
  
  public JSExtern(String name, JSUnitProvider.IProvider provider) {
    super(JSSourceFile.fromGenerator(name, provider));
  }
  
  public JSExtern(File file) {
    this(file.getAbsolutePath(), new JSUnitProvider.FromLibraryFile(file, CacheMode.DISABLED));
  }

}
