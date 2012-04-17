package net.vtst.ow.eclipse.js.closure.compiler;

import java.io.File;

import org.eclipse.core.runtime.CoreException;

import net.vtst.eclipse.easy.ui.properties.stores.IStore;
import net.vtst.eclipse.easy.ui.properties.stores.PluginPreferenceStore;
import net.vtst.ow.closure.compiler.deps.JSLibrary;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;
import net.vtst.ow.eclipse.js.closure.preferences.ClosurePreferenceRecord;

import com.google.javascript.jscomp.AbstractCompiler;

public class JSLibraryProviderForLaunch implements IJSLibraryProvider {

  public JSLibrary get(AbstractCompiler compiler, File libraryPath, File pathOfClosureBase) {
    JSLibrary library = new JSLibrary(libraryPath, pathOfClosureBase, getCacheSettingsFromPreferences());
    library.setUnits(compiler);
    return library;
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
