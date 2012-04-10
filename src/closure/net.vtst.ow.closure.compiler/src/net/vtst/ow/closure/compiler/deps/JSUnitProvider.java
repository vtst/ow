package net.vtst.ow.closure.compiler.deps;


import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import net.vtst.ow.closure.compiler.deps.JSLibrary.CacheMode;
import net.vtst.ow.closure.compiler.strip.JSFileStripper;
import net.vtst.ow.closure.compiler.util.NullErrorManager;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.javascript.jscomp.SourceFile;

/**
 * A compilation unit provider provides the source code for a JavaScript compilation unit
 * from an arbitrary source.  This class defines the generic interface, as well as some
 * implementations.
 * @author Vincent Simonet
 */
public class JSUnitProvider {
  
  /**
   * Generic interface for compilation unit providers.
   */
  public static interface IProvider extends SourceFile.Generator {
    
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
  public static class FromFile implements IProvider {
    
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
    
    protected File getFile() {
      return file;
    }

    @Override
    public void prepareToGetCode() throws IOException {
      try {
        code = Files.toString(getFile(), charset);
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
  
  public static class FromLibraryFile extends FromFile {
    
    private static final String EXTENSION_FOR_STRIPPED_FILES = ".ow";
    
    private File strippedFile;
    private JSLibrary.CacheMode stripMode;

    public FromLibraryFile(File file, JSLibrary.CacheMode stripMode) {
      super(file);
      this.stripMode = stripMode;
      strippedFile = new File(file.getPath() + EXTENSION_FOR_STRIPPED_FILES);
    }
    
    public FromLibraryFile(File file, Charset charset) {
      super(file, charset);
    }
    
    private boolean strip() {
      JSFileStripper stripper = new JSFileStripper(new NullErrorManager());
      try {
        stripper.strip(super.getFile(), strippedFile);
        return true;
      } catch (IOException e) {
        return false;
      }
    }
    
    protected File getFile() {
      File originalFile = super.getFile();
      if (stripMode == CacheMode.DISABLED) return originalFile;
      if (strippedFile.exists()) {
        if (strippedFile.lastModified() >= originalFile.lastModified()) {
          return strippedFile;
        }
        else if (stripMode == CacheMode.READ_AND_WRITE && strippedFile.canWrite() && strip()) {
          return strippedFile;
        } else {
          return originalFile;
        }
      } else {
        try {
          if (stripMode == CacheMode.READ_AND_WRITE && strippedFile.createNewFile() && strip()) {
            return strippedFile;
          } else {
            return originalFile;
          }
        } catch (IOException e) {
          return originalFile;
        }
      }
    }
    
    @Override
    public long lastModified() {
      return 1;  // not 0, otherwise it will never load.
    }
    
  }

  /**
   * Compilation unit provider which gets the source code from a string. 
   */
  public static class FromCode implements IProvider {
    
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
