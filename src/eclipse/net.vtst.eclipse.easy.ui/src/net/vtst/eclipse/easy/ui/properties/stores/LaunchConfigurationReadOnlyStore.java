package net.vtst.eclipse.easy.ui.properties.stores;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * Wrapping of {@code ILaunchConfiguration} into {@code IReadOnlyStore}.
 * @author Vincent Simonet
 */
public class LaunchConfigurationReadOnlyStore implements IReadOnlyStore {
  
  private ILaunchConfiguration config;

  public LaunchConfigurationReadOnlyStore(ILaunchConfiguration config) {
    this.config = config;
  }

  @Override
  public boolean get(String name, boolean defaultValue)
      throws CoreException {
    return config.getAttribute(name, defaultValue ? -1 : 0) == -1;
  }

  @Override
  public int get(String name, int defaultValue) throws CoreException {
    return config.getAttribute(name, defaultValue);
  }

  @Override
  public double get(String name, double defaultValue)
      throws CoreException {
    String value = config.getAttribute(name, (String) null);
    if (value == null) return defaultValue;
    return Double.parseDouble(value);  // TODO Parse errors?
  }

  @Override
  public String get(String name, String defaultValue)
      throws CoreException {
    return config.getAttribute(name, defaultValue);
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<String> get(String name, List<String> defaultValue) throws CoreException {
    return config.getAttribute(name, defaultValue);
  }

  @Override
  public boolean has(String name) throws CoreException {
    return config.hasAttribute(name);
  }

}
