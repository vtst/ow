package net.vtst.ow.eclipse.soy.ui.launching;

import net.vtst.eclipse.easyxtext.ui.launching.attributes.BooleanLaunchAttribute;
import net.vtst.eclipse.easyxtext.ui.launching.attributes.StringLaunchAttribute;
import net.vtst.eclipse.easyxtext.ui.launching.tab.EasyLaunchConfigurationTab;
import net.vtst.ow.eclipse.soy.ui.SoyUiMessages;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.google.inject.Inject;

@SuppressWarnings("restriction")
public class SoyCompilerInputsOutputsTab extends EasyLaunchConfigurationTab {

  @Inject 
  private SoyCompilerLaunchConfigurationHelper configHelper;
  
  @Inject
  private SoyUiMessages messages;
  
  @Override
  public String getName() {
    return messages.getString("lconf_inputs_outputs");
  }

  private BooleanLaunchAttribute.Control localize;
  private StringLaunchAttribute.Control outputPathFormat;
  private StringLaunchAttribute.Control locales;
  private StringLaunchAttribute.Control messageFilePathFormat;
  private StringLaunchAttribute.Control outputPathFormatLocalized;

  @Override
  public void createControls(Composite parent) {
    Group inputs = SWTFactory.createGroup(parent, messages.getString("lconf_inputs"), 3, 2, GridData.FILL_HORIZONTAL);
    Group outputs = SWTFactory.createGroup(parent, messages.getString("lconf_outputs"), 2, 2, GridData.FILL_HORIZONTAL);
    configHelper.inputFile.createControl(this, inputs, 3);
    configHelper.compileTimeGlobalsFile.createControl(this, inputs, 3);
    localize = configHelper.localize.createControl(this, outputs, 2);
    outputPathFormat = configHelper.outputPathFormat.createControl(this, outputs, 2);
    locales = configHelper.locales.createControl(this, outputs, 2);
    messageFilePathFormat = configHelper.messageFilePathFormat.createControl(this, outputs, 2);
    outputPathFormatLocalized = configHelper.outputPathFormatLocalized.createControl(this, outputs, 2);
    
    Group others = SWTFactory.createGroup(parent, messages.getString("lconf_others"), 2, 2, GridData.FILL_HORIZONTAL);
    configHelper.useAsDefault.createControl(this, others, 2);

    localize.addSelectionListener(new SelectionListener(){
      public void widgetDefaultSelected(SelectionEvent arg0) {}
      public void widgetSelected(SelectionEvent arg0) {
        refreshControls();
      }
    });
  }

  @Override
  protected void refreshControls() {
    boolean status = localize.getControlValue().booleanValue();
    outputPathFormat.setEnabled(!status);
    locales.setEnabled(status);
    messageFilePathFormat.setEnabled(status);
    outputPathFormatLocalized.setEnabled(status);
  }
  
  @Override
  public boolean isValid(ILaunchConfiguration config) {
    if (!super.isValid(config)) return false;
    if (configHelper.localize.getBooleanValue(config) &&
        configHelper.locales.getValue(config).isEmpty()) {
      setErrorMessage(messages.getString("empty_locales"));
      return false;
    }
    setErrorMessage(null);
    return true;
  }
  
}
