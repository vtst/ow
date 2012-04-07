package net.vtst.eclipse.easy.ui.properties.stores;

import java.util.List;

import net.vtst.eclipse.easy.ui.util.Utils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

/**
 * Wrapping of {@code IProject} into {@code IStore}.
 * @author Vincent Simonet
 */
public class ProjectPropertyStore implements IStore {
  
  private IProject project;
  private String qualifier;

  public ProjectPropertyStore(IProject project, String qualifier) {
    this.project = project;
    this.qualifier = qualifier;
  }
  
  private QualifiedName getName(String localName) {
    return new QualifiedName(qualifier, localName);
  }
  
  private String getPersistentProperty(String localName) throws CoreException {
    return project.getPersistentProperty(getName(localName));
  }
  
  private void setPersistentProperty(String localName, String value) throws CoreException {
    project.setPersistentProperty(getName(localName), value);
  }

  @Override
  public boolean get(String name, boolean defaultValue)
      throws CoreException {
    String value = getPersistentProperty(name);
    if (value == null) return defaultValue;
    return Boolean.parseBoolean(value);  // TODO: How are parse errors handled?
  }

  @Override
  public int get(String name, int defaultValue) throws CoreException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double get(String name, double defaultValue)
      throws CoreException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String get(String name, String defaultValue) throws CoreException {
    String value = getPersistentProperty(name);
    if (value == null) return defaultValue;
    else return value;
  }
  
  @Override
  public List<String> get(String name, List<String> defaultValue) throws CoreException {
    return Utils.stringToStringList(getPersistentProperty(name), defaultValue);
  }

  @Override
  public void set(String name, boolean value) throws CoreException {
    setPersistentProperty(name, Boolean.toString(value));
  }

  @Override
  public void set(String name, int value) throws CoreException {
    setPersistentProperty(name, Integer.toString(value));
  }

  @Override
  public void set(String name, double value) throws CoreException {
    setPersistentProperty(name, Double.toString(value));
  }

  @Override
  public void set(String name, String value) throws CoreException {
    setPersistentProperty(name, value);
  }

  @Override
  public boolean has(String name) throws CoreException {
    return getPersistentProperty(name) != null;
  }

  @Override
  public void set(String name, List<String> value) throws CoreException {
    setPersistentProperty(name, Utils.stringListToString(value));
  }

}
