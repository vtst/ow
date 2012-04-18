package net.vtst.ow.eclipse.js.closure.launching.linter;

import net.vtst.ow.eclipse.js.closure.OwJsClosureMessages;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;

public class ClosureLinterLaunchConfigurationDelegate extends LaunchConfigurationDelegate {
  
  public static final String TYPE_ID = "net.vtst.ow.eclipse.js.closure.launching.compiler";
  
  private OwJsClosureMessages messages = OwJsClosurePlugin.getDefault().getMessages();
  private ClosureLinterLaunchConfigurationRecord record = ClosureLinterLaunchConfigurationRecord.getInstance();

  @Override
  public void launch(ILaunchConfiguration config, String mode, ILaunch launch, IProgressMonitor monitor)
      throws CoreException {
  }

}
