// EasyXtext
// (c) Vincent Simonet, 2011

package net.vtst.eclipse.easy.ui.launching;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.vtst.eclipse.easy.ui.EasyUiMessages;
import net.vtst.eclipse.easy.ui.EasyUiPlugin;
import net.vtst.eclipse.easy.ui.launching.EasyLaunchConfigurationDelegateUtils.IProcessListenerAcceptor;
import net.vtst.eclipse.easy.ui.util.Utils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.debug.core.model.RuntimeProcess;

/**
 * Abstract class for implementing launch configuration delegates which run an external program.
 * If the external program is a Java program, consider using 
 * {@link EasyJavaProgramLaunchConfigurationDelegate} instead.
 * <br/>
 * Listeners can be attached to the process in order to analyze the output of the program.
 * <br/>
 * The launch configuration tab group should include at least the following tabs:
 * <ul>
 *   <li>{@code org.eclipse.debug.ui.EnvironmentTab},</li>
 *   <li>{@code import org.eclipse.debug.ui.CommonTab}.</li>
 * </ul>  
 * @author Vincent Simonet
 */
public abstract class EasyExtProgramLaunchConfigurationDelegate<Fixture> extends LaunchConfigurationDelegate {
  
  private EasyUiMessages messages = EasyUiPlugin.getDefault().getMessages();
  
  /**
   * Get the environment as a string array from the launch configuration.  The default implementation
   * get the environment set on the environment tab.
   * @param config
   * @return  The environment, as an array of strings "variable=value".
   */
  protected String[] getEnvironmentAsArray(ILaunchConfiguration config, Fixture fixture) throws CoreException {
    String[] array = DebugPlugin.getDefault().getLaunchManager().getEnvironment(config);
    if (array == null) return new String[0];
    return array;
  }
  
  /**
   * Get the environment as a map from variables to values.
   * @param config
   * @return The environment, as a map.
   */
  private Map<String, String> getEnvironmentAsMap(ILaunchConfiguration config, Fixture fixture) throws CoreException { 
    Map<String, String> map = new HashMap<String, String>();
    for (String s: getEnvironmentAsArray(config, fixture)) {
      int index = s.indexOf('=');
      if (index >= 1) map.put(s.substring(0, index), s.substring(index + 1));
    }
    return map;
  }

  /**
   * Create a process builder for the given launch configuration.  The default implementation
   * only set the environment variables.  You should refine this implementation in sub-classes
   * by setting other attributes, like the command to run.
   * @param config
   * @return  The process builder.
   */
  protected ProcessBuilder getProcessBuilder(ILaunchConfiguration config, Fixture fixture) throws CoreException {
    ProcessBuilder pb = new ProcessBuilder();
    pb.environment().putAll(getEnvironmentAsMap(config, fixture));
    return pb;
  }
  
  /**
   * Returns the fixture for a launch configuration.  The fixture is an object where you can store
   * some structured information which is elaborated from the launch configuration, and
   * used in several methods or event listeners.
   * In the default implementation, the fixture is <code>null</code>.
   * @param config  The launch configuration.
   * @return  The fixture for that launch configuration.
   */
  protected Fixture getFixture(ILaunchConfiguration config) throws CoreException {
    return null;
  }

  /**
   * Add listeners to the launched process.  The default implementation does nothing.
   * @param config  The launch configuration, which may be used to configure listeners.
   * @param fixture  The fixture, which may be used to configure listeners.
   * @param acceptor  The acceptor on which the function can register new listeners.
   */
  protected void addProcessListeners(ILaunchConfiguration config, Fixture fixture, IProcessListenerAcceptor acceptor) {}
  
  @Override
  public void launch(ILaunchConfiguration config, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
    Fixture fixture = getFixture(config);
    if (monitor == null) monitor = new NullProgressMonitor();
    monitor.beginTask(config.getName() + "...", 3);
    if (monitor.isCanceled()) return;

    ProcessBuilder pb = this.getProcessBuilder(config, fixture);
    try {
      Process process;
      try {
        // We do not catch NullPointerException or IndexOutOfBoundsException, because it would mean
        // the command of the process is null or empty.
        process = pb.start();
      } catch (SecurityException exn) {
        throw createCoreException(messages.getString("process_security_exception"), exn);
      } catch (IOException exn) {
        throw createCoreException(messages.getString("process_io_exception"), exn);
      }
      launch.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, "true");
      RuntimeProcess runtimeProcess = new RuntimeProcess(launch, process, "External program", Collections.EMPTY_MAP);
      runtimeProcess.setAttribute(IProcess.ATTR_CMDLINE, Utils.renderCommandLine(pb.command()));
      addProcessListeners(config, fixture, new EasyLaunchConfigurationDelegateUtils.ProcessListernerAcceptorImpl(runtimeProcess));
    } finally {
      monitor.done();
    }
  }
  
  /**
   * Create a core exception to report a fatal error to the user.
   * @param message  The error message
   * @param exn  The exception which originated the fatal error.
   * @return  The core exception, ready to be thrown.
   */
  protected CoreException createCoreException(final String message, final Throwable exn) {
    // http://wiki.eclipse.org/Platform_UI_Error_Handling
    IStatus status = new Status(IStatus.ERROR, EasyUiPlugin.PLUGIN_ID, message, exn);
    return new CoreException(status);
  }

}