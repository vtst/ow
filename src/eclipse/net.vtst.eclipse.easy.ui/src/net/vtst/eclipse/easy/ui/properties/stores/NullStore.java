package net.vtst.eclipse.easy.ui.properties.stores;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

public class NullStore implements IStore {

  public boolean get(String name, boolean defaultValue) throws CoreException {
    return defaultValue;
  }

  public int get(String name, int defaultValue) throws CoreException {
    return defaultValue;
  }

  public double get(String name, double defaultValue) throws CoreException {
    return defaultValue;
  }

  public String get(String name, String defaultValue) throws CoreException {
    return defaultValue;
  }

  public List<String> get(String name, List<String> defaultValue)
      throws CoreException {
    return defaultValue;
  }

  public boolean has(String name) throws CoreException {
    return false;
  }

  public void set(String name, boolean value) throws CoreException {
  }

  public void set(String name, int value) throws CoreException {
  }

  public void set(String name, double value) throws CoreException {
  }

  public void set(String name, String value) throws CoreException {
  }

  public void set(String name, List<String> defaultValue) throws CoreException {
  }

}
