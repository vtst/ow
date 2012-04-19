package net.vtst.ow.eclipse.js.closure.launching.compiler;

import java.io.File;

import net.vtst.eclipse.easy.ui.properties.stores.IStore;
import net.vtst.eclipse.easy.ui.properties.stores.PluginPreferenceStore;
import net.vtst.ow.closure.compiler.deps.JSExtern;
import net.vtst.ow.closure.compiler.deps.JSLibrary;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;
import net.vtst.ow.eclipse.js.closure.compiler.IJSIncludesProvider;
import net.vtst.ow.eclipse.js.closure.preferences.ClosurePreferenceRecord;

import org.eclipse.core.runtime.CoreException;

import com.google.javascript.jscomp.AbstractCompiler;

public class JSIncludesProviderForLaunch implements IJSIncludesProvider {

  public JSLibrary getLibrary(AbstractCompiler compiler, File libraryPath, File pathOfClosureBase) {
    JSLibrary library = new JSLibrary(libraryPath, pathOfClosureBase, getCacheSettingsFromPreferences());
    library.setUnits(compiler);
    return library;
  }
  
  public JSExtern getExtern(AbstractCompiler compiler, File path) {
    return new JSExtern(path);
  }

  public void clear() {
  }

  private JSLibrary.CacheSettings getCacheSettingsFromPreferences() {
    ClosurePreferenceRecord record = ClosurePreferenceRecord.getInstance();
    JSLibrary.CacheSettings result = new JSLibrary.CacheSettings();
    IStore store = new PluginPreferenceStore(OwJsClosurePlugin.getDefault().getPreferenceStore());
    try {
      result.cacheDepsFiles = record.cacheLibraryDepsFiles.get(store);
    } catch (CoreException e) {
      result.cacheDepsFiles = record.cacheLibraryDepsFiles.getDefault();
    }
    result.cacheStrippedFiles = JSLibrary.CacheMode.DISABLED;
    return result;
  }

}
