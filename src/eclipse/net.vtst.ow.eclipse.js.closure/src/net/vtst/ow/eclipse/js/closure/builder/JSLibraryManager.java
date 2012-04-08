package net.vtst.ow.eclipse.js.closure.builder;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.CoreException;

import net.vtst.eclipse.easy.ui.properties.stores.IStore;
import net.vtst.eclipse.easy.ui.properties.stores.PluginPreferenceStore;
import net.vtst.ow.closure.compiler.deps.JSLibrary;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;
import net.vtst.ow.eclipse.js.closure.preferences.ClosurePreferenceRecord;

import com.google.javascript.jscomp.AbstractCompiler;

/**
 * Registry of JavaScript libraries, which cache created libraries.  The cached libraries
 * are kept with references, so that they can be garbage collected if no more project uses
 * them.
 * <br>
 * <b>Thread safety:</b> This class is thread safe, as it uses a concurrent hash map.
 * @author Vincent Simonet
 */
public class JSLibraryManager {
  
  /**
   * The key identifying a library in the cache.
   */
  private static class LibraryKey {
    private File libraryPath;
    private File pathOfClosureBase;
    private boolean isClosureBase;
    LibraryKey(File libraryPath, File pathOfClosureBase, boolean isClosureBase) {
      this.libraryPath = libraryPath;
      this.pathOfClosureBase = pathOfClosureBase;
      this.isClosureBase = isClosureBase;
    }
    public boolean equals(Object obj) {
      if (obj instanceof LibraryKey) {
        LibraryKey key = (LibraryKey) obj;
        return (
            key.isClosureBase == isClosureBase &&
            key.libraryPath.equals(libraryPath) && 
            key.pathOfClosureBase.equals(pathOfClosureBase));
      }
      return false;
    }
    public int hashCode() {
      int h1 = libraryPath.hashCode();
      int h2 = pathOfClosureBase.hashCode();
      return h1 * h2 + h1 + h2 + (isClosureBase ? 0 : 1013);
    }
  }
  
  private ConcurrentHashMap<LibraryKey, WeakReference<JSLibrary>> cache = 
      new ConcurrentHashMap<LibraryKey, WeakReference<JSLibrary>>();

  private JSLibrary get(LibraryKey key) {
    WeakReference<JSLibrary> ref = cache.get(key);
    if (ref == null) return null;
    else return ref.get();
  }
  
  /**
   * Get a library from the cache, or create it and add it to the cache if it is not in
   * the cache.
   * @param compiler
   * @param libraryPath
   * @param pathOfClosureBase
   * @param isClosureBase
   * @return
   */
  public JSLibrary get(AbstractCompiler compiler, File libraryPath, File pathOfClosureBase, boolean isClosureBase) {
    LibraryKey key = new LibraryKey(libraryPath, pathOfClosureBase, isClosureBase);
    JSLibrary library = get(key);
    if (library == null) {
      library = new JSLibrary(libraryPath, pathOfClosureBase, isClosureBase, getStripMode());
      library.setUnits(compiler);
      cache.put(key, new WeakReference<JSLibrary>(library));
    }
    return library;
  }
  
  private JSLibrary.StripMode getStripMode() {
    ClosurePreferenceRecord r = ClosurePreferenceRecord.getInstance();
    IStore prefs = new PluginPreferenceStore(OwJsClosurePlugin.getDefault().getPreferenceStore());
    try {
      if (r.readStrippedLibraryFiles.get(prefs)) {
        if (r.writeStrippedLibraryFiles.get(prefs)) {
          return JSLibrary.StripMode.READ_AND_WRITE;
        } else {
          return JSLibrary.StripMode.READ_ONLY;
        }
      } else {
        return JSLibrary.StripMode.DISABLED;
      }
    } catch (CoreException e) {
      return JSLibrary.StripMode.DISABLED;
   }
  }

}
