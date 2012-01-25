// EasyXtext
// (c) Vincent Simonet, 2011

package net.vtst.eclipse.easyxtext.ui.launching.attributes;

import net.vtst.eclipse.easyxtext.ui.launching.tab.IEasyLaunchConfigurationTab;

import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

/**
 * Launch attribute whose values are strings, and which is represented by a simple text
 * box in launch configuration tabs.
 * @author Vincent Simonet
 */
public class StringLaunchAttribute extends AbstractStringLaunchAttribute {
    
  public StringLaunchAttribute(String defaultValue) {
    super(defaultValue);
  }

  public Control createControl(IEasyLaunchConfigurationTab tab, Composite parent, int hspan) {
    return new Control(tab, parent, hspan);
  }
  
  public class Control extends AbstractLaunchAttribute<String>.Control {

    private Text text;
    
    @SuppressWarnings("restriction")
    public Control(IEasyLaunchConfigurationTab tab, Composite parent, int hspan) {
      super(tab, parent, hspan);
      if (hspan < 2) return;
      addWidget(SWTFactory.createLabel(parent, tab.getString(getLabelKey()), 1));
      text = SWTFactory.createSingleText(parent, hspan - 1);
      text.addModifyListener(tab.getUpdateListener());
      addWidget(text);
      tab.registerControl(this);
    }
    
    public String getControlValue() {
      return text.getText();
    }

    public void setControlValue(String value) {
      text.setText(value);
    } 
    
    public void addModifyListener(ModifyListener listener) {
      text.addModifyListener(listener);
    }
  }

}
