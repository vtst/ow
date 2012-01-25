// EasyXtext
// (c) Vincent Simonet, 2011

package net.vtst.eclipse.easyxtext.ui.launching.attributes;

import java.util.HashMap;
import java.util.Map;

import net.vtst.eclipse.easyxtext.ui.launching.tab.IEasyLaunchConfigurationTab;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * Launch attribute whose values are ranging over a list a string options, and which are
 * represented by radio buttons in launch configuration tabs.
 * @author Vincent Simonet
 */
public class StringOptionsLaunchAttribute extends AbstractStringLaunchAttribute {
  
  String[] values;
  Map<String, Integer> valuesMap = new HashMap<String, Integer>();
  
  
  public StringOptionsLaunchAttribute(String defaultValue, String[] values) {
    super(defaultValue);
    this.values = values;
    for (int i = 0; i < values.length; ++i) this.valuesMap.put(values[i], Integer.valueOf(i));
  }

  public StringOptionsLaunchAttribute(String[] values) {
    this(values[0], values);
  }

  
  @Override
  protected String fromLaunchConfiguration(ILaunchConfiguration config) throws CoreException {
    String value = super.fromLaunchConfiguration(config);
    if (valuesMap.containsKey(value)) return value;
    else return getDefaultValue();
  }

  @Override
  public Control createControl(IEasyLaunchConfigurationTab tab, Composite parent, int hspan) {
    return new Control(tab, parent, hspan);
  }
  
  public class Control extends AbstractLaunchAttribute<String>.Control {

    private Button[] buttons;
    
    @SuppressWarnings("restriction")
    public Control(IEasyLaunchConfigurationTab tab, Composite parent, int hspan) {
      super(tab, parent, hspan);
      if (hspan < 2) return;
      String labelKey = getLabelKey();
      buttons = new Button[values.length];
      addWidget(SWTFactory.createLabel(parent, tab.getString(labelKey), 1));
      Composite group = SWTFactory.createComposite(parent, values.length, hspan - 1, GridData.HORIZONTAL_ALIGN_BEGINNING);
      for (int i = 0; i < values.length; ++i) {
        buttons[i] = SWTFactory.createRadioButton(group, tab.getString(labelKey + "_" + values[i]));
        buttons[i].addSelectionListener(tab.getUpdateListener());
        addWidget(buttons[i]);
      }
      tab.registerControl(this);
    }
    
    public String getControlValue() { 
      for (int i = 0; i < values.length; ++i) {
        if (buttons[i].getSelection()) return values[i];
      }
      return null;
    }

    public void setControlValue(String value) {
      Integer i = valuesMap.get(value);
      if (i != null) {
        int ii = i.intValue();
        for (int j = 0; j < buttons.length; ++j) {
          buttons[j].setSelection(ii == j);
        }
      }
    } 
  }

}
