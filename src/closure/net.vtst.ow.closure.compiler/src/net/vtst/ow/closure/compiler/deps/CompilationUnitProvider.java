package net.vtst.ow.closure.compiler.deps;


import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.javascript.jscomp.SourceFile;

/**
 * A compilation unit provider provides the source code for a JavaScript compilation unit
 * from an arbitrary source.  This class defines the generic interface, as well as some
 * implementations.
 * @author Vincent Simonet
 */
public class CompilationUnitProvider {

  /**
   * Generic interface for compilation unit providers.
   */
  public static interface Interface extends SourceFile.Generator {
    
    /**
     * Return the timestamp of the last modification of the source.
     * @return The last modification timestamp.
     */
    public long lastModified();
    
    /**
     * To be called before getCode, so that exception can be caught.
     */
    public void prepareToGetCode() throws IOException;

  }
  
  /**
   * Compilation unit provider which gets the source code from a file. 
   */
  public static class FromFile implements Interface {
    
    private File file;
    private Charset charset;
    private String code;
    
    public FromFile(File file) {
      this(file, Charsets.UTF_8);
    }

    public FromFile(File file, Charset charset) {
      this.file = file;
      this.charset = charset;
    }

    @Override
    public void prepareToGetCode() throws IOException {
      try {
        code = Files.toString(file, charset);
      } catch (IOException exn) {
        code = "";
        throw exn;
      }      
    }

    @Override
    public String getCode() {
      return code;
    }

    @Override
    public long lastModified() {
      return file.lastModified();
    }
    
  }

  /**
   * Compilation unit provider which gets the source code from a string. 
   */
  public static class FromCode implements Interface {
    
    private String code;
    
    public FromCode(String code) {
      this.code = code;
    }

    @Override
    public void prepareToGetCode() throws IOException {}

    @Override
    public String getCode() {
      return code;
    }

    @Override
    public long lastModified() {
      return 0;
    }
    
  }
  
}
