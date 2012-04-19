package net.vtst.ow.closure.compiler.deps;

import java.io.File;

import net.vtst.ow.closure.compiler.deps.JSLibrary.CacheMode;

import com.google.javascript.jscomp.JSSourceFile;

public class JSExtern extends AstFactory {
  private static final long serialVersionUID = 1L;

  public JSExtern(String name, JSUnitProvider.IProvider provider) {
    super(JSSourceFile.fromGenerator(name, provider));
  }
  
  public JSExtern(File file) {
    this(file.getAbsolutePath(), new JSUnitProvider.FromLibraryFile(file, CacheMode.DISABLED));
  }

}
