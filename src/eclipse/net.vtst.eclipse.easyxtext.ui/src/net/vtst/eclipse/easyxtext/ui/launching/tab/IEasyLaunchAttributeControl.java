package net.vtst.eclipse.easyxtext.ui.launching.tab;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

/**
 * Interface for the controls created by the launch attributes.
 * @author Vincent Simonet
 */
public interface IEasyLaunchAttributeControl {
  
  /**
   * Set the current value of the control from a launch configuration.
   * @param config  The launch configuration to read.
   */
  public void initializeFrom(ILaunchConfiguration config);
 
  /**
   * Store the current value of the control in a launch configuration.
   * @param config  The launch configuration to write.
   */
  public void performApply(ILaunchConfigurationWorkingCopy config);
  
  /**
   * Test whether the current value of the control is valid.
   * @return  true if it is value, false if it is not valid.
   */
  public boolean isValid();
  
  /**
   * Set the value of the control to the default value.
   * @param config  The launch configuration to write.
   */
  public void setDefaults(ILaunchConfigurationWorkingCopy config);
}
