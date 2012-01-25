// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.ui.launching;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LessCompilerTabGroup extends AbstractLaunchConfigurationTabGroup {
  
  @Inject
  Provider<LessCompilerOptionsTab> lessCompilerOptionsTabProvider;

  public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
    ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
        lessCompilerOptionsTabProvider.get(),
        new EnvironmentTab(),
        new CommonTab()
    };
    setTabs(tabs);
  }

}
