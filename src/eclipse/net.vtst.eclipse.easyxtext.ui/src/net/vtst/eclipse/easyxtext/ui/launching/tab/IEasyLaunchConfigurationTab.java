// EasyXtext
// (c) Vincent Simonet, 2011

package net.vtst.eclipse.easyxtext.ui.launching.tab;


import net.vtst.eclipse.easyxtext.ui.launching.attributes.AbstractLaunchAttribute;

import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;

/**
 * Interface for {@link EasyLaunchConfigurationTab}, which is used for references in
 * launch attributes (see {@link AbstractLaunchAttribute}).
 * @author Vincent Simonet
 */
public interface IEasyLaunchConfigurationTab {

  /**
   * Return the string message for the given key.
   * @param key  The key of the message.
   * @return  the string message.
   */
  public String getString(String key);
  
  /**
   * Get the update listener for the option tab, which shall be notified by the controls of the
   * tab for any change of their value, in order to refresh the UI.
   * @return  the update listener.
   */
  public UpdateListener getUpdateListener();
  
  /**
   * Register a new control for the tab.  This method is called by the controls created
   * from launch attributes.
   * @param control
   */
  public void registerControl(IEasyLaunchAttributeControl control);
  
  /**
   * An update listener is the combination of a selection listener and a modify listener.
   * @author Vincent Simonet
   */
  public static interface UpdateListener extends SelectionListener, ModifyListener {}
  
}
