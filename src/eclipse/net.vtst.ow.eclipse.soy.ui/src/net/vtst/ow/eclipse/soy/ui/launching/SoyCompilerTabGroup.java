package net.vtst.ow.eclipse.soy.ui.launching;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class SoyCompilerTabGroup extends AbstractLaunchConfigurationTabGroup {

  @Inject 
  Provider<SoyCompilerCompilationOptionsTab> soyCompilerOptionsTabProvider;
  @Inject 
  Provider<SoyCompilerInputsOutputsTab> soyCompilerInputsOutputsTabProvider;
  
  public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
    ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
        soyCompilerInputsOutputsTabProvider.get(),
        soyCompilerOptionsTabProvider.get(),
        new JavaJRETab(),
        new JavaClasspathTab(), 
        new EnvironmentTab(),
        new CommonTab()
    };
    setTabs(tabs);
  }

}
