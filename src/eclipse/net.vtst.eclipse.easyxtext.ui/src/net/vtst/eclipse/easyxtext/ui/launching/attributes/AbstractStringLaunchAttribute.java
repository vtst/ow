// EasyXtext
// (c) Vincent Simonet, 2011

package net.vtst.eclipse.easyxtext.ui.launching.attributes;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

/**
 * Base class of launch attributes which are internally represented as strings.
 * @author Vincent Simonet
 */
public abstract class AbstractStringLaunchAttribute extends AbstractLaunchAttribute<String> {
  
  protected String defaultValue;
  
  public AbstractStringLaunchAttribute(String defaultValue) {
    this.defaultValue = defaultValue;
  }
  
  @Override
  protected String fromLaunchConfiguration(ILaunchConfiguration config) throws CoreException {
    return config.getAttribute(name, defaultValue);
  }

  @Override
  protected void toLaunchConfiguration(ILaunchConfigurationWorkingCopy config, String value) {
    config.setAttribute(name, value);
  }

  @Override
  protected String getDefaultValue() {
    return defaultValue;
  }
  
}
