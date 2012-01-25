// EasyXtext
// (c) Vincent Simonet, 2011

package net.vtst.eclipse.easyxtext.ui.launching.attributes;

import java.util.ArrayList;

import net.vtst.eclipse.easyxtext.ui.launching.tab.IEasyLaunchAttributeControl;
import net.vtst.eclipse.easyxtext.ui.launching.tab.IEasyLaunchConfigurationTab;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Widget;

/**
 * Base class to represent a typed attribute in a launch configuration.  This abstract class defines
 * some functions to be defined by sub-classes, and provides two "interfaces" for the attribute:
 * <ul>
 *   <li>The class <code>Widgets</code>, which provides methods to edit the attribute on an
 *     option tab.  These methods are used by <code>EasyOptionTab</code>.</li>
 *   <li>The methods <code>getValue</code> and <code>setValue</code> which allow accessing the
 *     attribute in a launch configuration.</li>
 * </ul>
 * @author Vincent Simonet
 *
 * @param <T>  The type for the values of the attribute.
 */
public abstract class AbstractLaunchAttribute<T> {
  
  protected String name = "noname";
  
  /**
   * @return  The name of the attribute (unique identifier).
   */
  public String getName() {
    return name;
  }
  
  /**
   * Set the name of the attribute.
   * @param name
   */
  public void setName(String name) {
    this.name = name;
  }
  
  /**
   * Get the value of the attribute in a launch configuration.  This method shall be implemented
   * by sub-classes.
   * @param config  The launch configuration to read.
   * @return  The value of the attribute in the launch configuration, or null if it cannot be read.
   * @throws CoreException
   * @throws EasyLaunchConfigurationException
   */
  protected abstract T fromLaunchConfiguration(ILaunchConfiguration config) 
      throws CoreException;
  
  /**
   * Set the value of the attribute in a launch configuration.  This method shall be implemented
   * by sub-classes.
   * @param config  The launch configuration to update.
   * @param value  The value to set to the attribute.
   */
  protected abstract void toLaunchConfiguration(ILaunchConfigurationWorkingCopy config, T value);
  
  /**
   * This method shall be implemented by sub-classes.
   * @return  The default value for the attribute.
   */
  protected abstract T getDefaultValue();

  /**
   * Create the controls needed to edit the value of the attribute on an option tab.
   * @param parent  The parent control in which the widgets will be inserted.
   * @param hspan  The number of columns to span on.
   * @param tab  The launch configuration tab in which the control is inserted.
   * @return  The created control.
   */
  public abstract Control createControl(IEasyLaunchConfigurationTab tab, Composite parent, int hspan);

  public abstract class Control implements IEasyLaunchAttributeControl {
   
    private ArrayList<Widget> widgets = new ArrayList<Widget>();
    protected IEasyLaunchConfigurationTab tab;
    
    public Control(IEasyLaunchConfigurationTab tab, Composite parent, int hspan) {
      this.tab = tab;
    }
    
    public void addWidget(Widget widget) {
      widgets.add(widget);
    }
    
    /**
     * This method shall be implemented by sub-classes.
     * @return  The value of the attribute currently stored by the widgets, or null if the
     *   current state of the widgets do not represent a legal value.
     */
    public abstract T getControlValue();
  
    /**
     * Set the current state of the widgets so that they represent the given value.
     * This method shall be implemented by sub-classes.
     * @param value  The value to set.
     */
    public abstract void setControlValue(T value);
        
    /* (non-Javadoc)
     * @see net.vtst.ow.easyxtext.ui.launching.configuration.IEasyLaunchAttributeControl#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
     */
    public void initializeFrom(ILaunchConfiguration config) {
      setControlValue(getValue(config));
    }

    /* (non-Javadoc)
     * @see net.vtst.ow.easyxtext.ui.launching.configuration.IEasyLaunchAttributeControl#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void performApply(ILaunchConfigurationWorkingCopy config) {
      T value = getControlValue();
      toLaunchConfiguration(config, value == null ? getDefaultValue() : value);
    }
    
    /* (non-Javadoc)
     * @see net.vtst.ow.easyxtext.ui.launching.configuration.IEasyLaunchAttributeControl#isValid()
     */
    public boolean isValid() {
      return (getControlValue() != null);
    }

    /* (non-Javadoc)
     * @see net.vtst.ow.easyxtext.ui.launching.configuration.IEasyLaunchAttributeControl#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void setDefaults(ILaunchConfigurationWorkingCopy config) {
      toLaunchConfiguration(config, getDefaultValue());
    }
    
    public void setEnabled(boolean enabled) {
      for (Widget widget: widgets) {
        if (widget instanceof org.eclipse.swt.widgets.Control) {
          ((org.eclipse.swt.widgets.Control) widget).setEnabled(enabled);
        }
      }
    }
  }

  /**
   * Get the value of the given attribute in the launch configuration.  Returns the default
   * value if it is not found or if it cannot be read.  This method should not be overridden.
   * @param config  The launch configuration to read from.
   * @return  The value of the attribute.
   */
  public T getValue(ILaunchConfiguration config) {
    if (config == null) return getDefaultValue();
    try {
      T value = fromLaunchConfiguration(config);
      return (value == null ? getDefaultValue() : value);
    } catch (CoreException e) {
      return getDefaultValue();
    }    
  }
  
  /**
   * Set the value of the given attribute in the launch configuration.  This method should not
   * be overridden.
   * @param config  The launch configuration to update.
   * @param value  The value of the attribute.
   */
  public void setValue(ILaunchConfigurationWorkingCopy config, T value) {
    toLaunchConfiguration(config, value);
  }
  
  /**
   * @return  The key for the label for the option in the resource bundle.
   */
  protected String getLabelKey() {
    return "lattr_" + name;
  }
  
}
