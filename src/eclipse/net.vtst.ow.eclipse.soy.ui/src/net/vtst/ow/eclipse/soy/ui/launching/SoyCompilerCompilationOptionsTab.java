package net.vtst.ow.eclipse.soy.ui.launching;

import net.vtst.eclipse.easyxtext.ui.launching.tab.EasyLaunchConfigurationTab;
import net.vtst.ow.eclipse.soy.ui.SoyUiMessages;

import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.google.inject.Inject;

@SuppressWarnings("restriction")
public class SoyCompilerCompilationOptionsTab extends EasyLaunchConfigurationTab {

  @Inject 
  private SoyCompilerLaunchConfigurationHelper launchConfiguration;
  
  @Inject
  private SoyUiMessages messages;
  
  @Override
  public String getName() {
    return messages.getString("lconf_compilation_options");
  }

  @Override
  public void createControls(Composite parent) {
    Group soyCode = SWTFactory.createGroup(parent, messages.getString("lconf_source_code"), 2, 2, GridData.FILL_HORIZONTAL);
    Group generatedCode = SWTFactory.createGroup(parent, messages.getString("lconf_generated_code"), 2, 2, GridData.FILL_HORIZONTAL);
    launchConfiguration.bidiGlobalDir.createControl(this, soyCode, 2);
    launchConfiguration.codeStyle.createControl(this, generatedCode, 2);
    launchConfiguration.cssHandlingScheme.createControl(this, generatedCode, 2);
    launchConfiguration.isUsingIjData.createControl(this, soyCode, 2);
    launchConfiguration.shouldDeclareTopLevelNamespaces.createControl(this, generatedCode, 2);
    launchConfiguration.shouldGenerateJsdoc.createControl(this, generatedCode, 2);
    launchConfiguration.shouldProvideRequireSoyNamespaces.createControl(this, generatedCode, 2);
  }

  @Override
  protected void refreshControls() {}
  
}
