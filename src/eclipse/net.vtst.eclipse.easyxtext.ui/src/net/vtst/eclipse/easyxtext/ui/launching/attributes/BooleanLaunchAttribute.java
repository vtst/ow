// EasyXtext
// (c) Vincent Simonet, 2011

package net.vtst.eclipse.easyxtext.ui.launching.attributes;

import net.vtst.eclipse.easyxtext.ui.launching.tab.IEasyLaunchConfigurationTab;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * Launch attribute whose values are booleans, and which is represented by a check-box in
 * launch configuration tabs.
 * @author Vincent Simonet
 *
 */
public class BooleanLaunchAttribute extends AbstractLaunchAttribute<Boolean> {

  protected boolean defaultValue;
  
  public BooleanLaunchAttribute(boolean defaultValue) {
    this.defaultValue = defaultValue;
  }
  
  protected Boolean fromLaunchConfiguration(ILaunchConfiguration config) throws CoreException {
    return Boolean.valueOf(config.getAttribute(name, Boolean.valueOf(defaultValue)));
  }

  protected void toLaunchConfiguration(ILaunchConfigurationWorkingCopy config, Boolean value) {
    config.setAttribute(name, value.booleanValue());
  }

  protected Boolean getDefaultValue() {
    return Boolean.valueOf(defaultValue);
  }
  
  public boolean getBooleanValue(ILaunchConfiguration config) {
    return this.getValue(config).booleanValue();
  }
  
  public void setValue(boolean value) {
    setValue(Boolean.valueOf(value));
  }

  public Control createControl(IEasyLaunchConfigurationTab tab, Composite parent, int hspan) {
    return new Control(tab, parent, hspan);
  }
  
  public class Control extends AbstractLaunchAttribute<Boolean>.Control {

    private Button checkbox;
    
    @SuppressWarnings("restriction")
    public Control(IEasyLaunchConfigurationTab tab, Composite parent, int hspan) {
      super(tab, parent, hspan);
      checkbox = SWTFactory.createCheckButton(parent, tab.getString(getLabelKey()), null, false, hspan);
      checkbox.addSelectionListener(tab.getUpdateListener());
      tab.registerControl(this);
    }
    
    public Boolean getControlValue() {
      return Boolean.valueOf(checkbox.getSelection());
    }

    public void setControlValue(Boolean value) {
      checkbox.setSelection(value.booleanValue());
    } 
    
    public void addSelectionListener(SelectionListener listener) {
      checkbox.addSelectionListener(listener);
    }
    
  }


}
