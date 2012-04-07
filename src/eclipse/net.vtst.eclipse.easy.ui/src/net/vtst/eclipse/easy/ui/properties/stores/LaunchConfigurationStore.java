package net.vtst.eclipse.easy.ui.properties.stores;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

/**
 * Wrapping of {@code ILaunchConfigurationWorkingCopy} into {@code IStore}.
 * @author Vincent Simonet
 */
public class LaunchConfigurationStore extends LaunchConfigurationReadOnlyStore implements IStore {

  private ILaunchConfigurationWorkingCopy config;

  public LaunchConfigurationStore(ILaunchConfigurationWorkingCopy config) {
    super(config);
    this.config = config;
  }

  @Override
  public void set(String name, boolean value) throws CoreException {
    config.setAttribute(name, value);
  }

  @Override
  public void set(String name, int value) throws CoreException {
    config.setAttribute(name, value);
  }

  @Override
  public void set(String name, double value) throws CoreException {
    config.setAttribute(name, Double.toString(value));    
  }

  @Override
  public void set(String name, String value) throws CoreException {
    config.setAttribute(name, value);    
  }

  @Override
  public void set(String name, List<String> defaultValue) throws CoreException {
    config.setAttribute(name, defaultValue);
  }

}
