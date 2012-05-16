// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.ui.launching;

import net.vtst.eclipse.easyxtext.ui.launching.attributes.BooleanLaunchAttribute;
import net.vtst.eclipse.easyxtext.ui.launching.attributes.StringLaunchAttribute;
import net.vtst.eclipse.easyxtext.ui.launching.attributes.WorkspaceFileLaunchAttribute;
import net.vtst.eclipse.easyxtext.ui.launching.tab.EasyLaunchConfigurationTab;
import net.vtst.ow.eclipse.less.ui.LessImageHelper;
import net.vtst.ow.eclipse.less.ui.LessUiMessages;

import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.google.inject.Inject;

@SuppressWarnings("restriction")
public class LessCompilerOptionsTab extends EasyLaunchConfigurationTab {

  @Inject 
  private LessCompilerLaunchConfigurationHelper configHelper;
  
  @Inject
  private LessUiMessages messages;

  @Inject
  private LessImageHelper imageHelper;
  
  @Override
  public String getName() {
    return messages.getString("lconf_options");
  }
  
  @Override
  public Image getImage() {
    return imageHelper.getImage(LessImageHelper.STYLESHEET);
  }

  private WorkspaceFileLaunchAttribute.Control inputFile;
  private StringLaunchAttribute.Control outputFile;
  private BooleanLaunchAttribute.Control autoOutputFile;

  @Override
  public void createControls(Composite parent) {
    Group compilationOptions = SWTFactory.createGroup(parent, messages.getString("lconf_compilation_options"), 2, 2, GridData.FILL_HORIZONTAL);
    configHelper.command.createControl(this, compilationOptions, 2);
    configHelper.verbose.createControl(this, compilationOptions, 2);
    configHelper.strictImports.createControl(this, compilationOptions, 2);
    configHelper.compress.createControl(this, compilationOptions, 2);
    configHelper.yuiCompress.createControl(this, compilationOptions, 2);
    configHelper.optimization.createControl(this, compilationOptions, 2);

    Group inputsOutputs = SWTFactory.createGroup(parent, messages.getString("lconf_inputs_outputs"), 3, 2, GridData.FILL_HORIZONTAL);
    inputFile = configHelper.inputFile.createControl(this, inputsOutputs, 3);
    outputFile = configHelper.outputFile.createControl(this, inputsOutputs, 2);
    autoOutputFile = configHelper.autoOutputFile.createControl(this, inputsOutputs, 1);
    autoOutputFile.addSelectionListener(new SelectionListener(){
      public void widgetDefaultSelected(SelectionEvent arg0) {}
      public void widgetSelected(SelectionEvent arg0) { refreshControls(); }
    });
    inputFile.addModifyListener(new ModifyListener(){
      public void modifyText(ModifyEvent arg0) { refreshControls(); }
    });
    
    Group others = SWTFactory.createGroup(parent, messages.getString("lconf_others"), 2, 2, GridData.FILL_HORIZONTAL);
    configHelper.useAsDefault.createControl(this, others, 2);
  }
  
  protected void refreshControls() {
    if (autoOutputFile.getControlValue().booleanValue()) {
      outputFile.setEnabled(false);
      outputFile.setControlValue(configHelper.getAutoOutputFile(inputFile.getControlValue()));
    } else {
      outputFile.setEnabled(true);
    }
  }
  
}
