package net.vtst.eclipse.easy.ui.properties.stores;

import java.util.List;

import net.vtst.eclipse.easy.ui.util.Utils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Wrapping of {@code IPreferenceStore} into {@code IStore}.
 * @author Vincent Simonet
 */
public class PluginPreferenceStore implements IStore {

  private IPreferenceStore store;

  public PluginPreferenceStore(IPreferenceStore store) {
    this.store = store;
  }

  @Override
  public boolean get(String name, boolean defaultValue)
      throws CoreException {
    return store.getBoolean(name);
  }

  @Override
  public int get(String name, int defaultValue) throws CoreException {
    return store.getInt(name);
  }

  @Override
  public double get(String name, double defaultValue)
      throws CoreException {
    return store.getDouble(name);
  }

  @Override
  public String get(String name, String defaultValue)
      throws CoreException {
    return store.getString(name);
  }
  
  @Override
  public List<String> get(String name, List<String> defaultValue) throws CoreException {
    return Utils.stringToStringList(store.getString(name), defaultValue);
  }

  @Override
  public void set(String name, boolean value) throws CoreException {
    store.setValue(name, value);
  }

  @Override
  public void set(String name, int value) throws CoreException {
    store.setValue(name, value);
  }

  @Override
  public void set(String name, double value) throws CoreException {
    store.setValue(name, value);
  }

  @Override
  public void set(String name, String value) throws CoreException {
    store.setValue(name, value);
  }

  @Override
  public void set(String name, List<String> value) throws CoreException {
    store.setValue(name, Utils.stringListToString(value));
  }

  @Override
  public boolean has(String name) throws CoreException {
    return store.contains(name);
  }
}
