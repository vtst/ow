// EasyXtext
// (c) Vincent Simonet, 2011

package net.vtst.eclipse.easy.ui.launching;

import java.io.File;
import java.util.Map;

import net.vtst.eclipse.easy.ui.EasyUiMessages;
import net.vtst.eclipse.easy.ui.EasyUiPlugin;
import net.vtst.eclipse.easy.ui.launching.EasyLaunchConfigurationDelegateUtils.IProcessListenerAcceptor;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;


/**
 * Abstract class for implementing launch configuration delegates which run a Java program (in a
 * separate VM).
 * <br/>
 * The launch configuration tab group should include at least the following tabs:
 * <br/>
 * Listeners can be attached to the process in order to analyze the output of the program.
 * <ul>
 *   <li>{@code org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab},</li>
 *   <li>{@code org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab},</li>
 *   <li>{@code org.eclipse.debug.ui.EnvironmentTab},</li>
 *   <li>{@code org.eclipse.debug.ui.CommonTab}.</li>
 * </ul>  
 * @author Vincent Simonet
 */
public class EasyJavaProgramLaunchConfigurationDelegate<Fixture> extends AbstractJavaLaunchConfigurationDelegate {
  
  private EasyUiMessages messages = EasyUiPlugin.getDefault().getMessages();
  
  /**
   * Get the full name of the main class to run.  The default implementation gets it
   * from the launch configuration, but you probably want to override this.
   * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getMainTypeName(org.eclipse.debug.core.ILaunchConfiguration)
   */
  public String getMainTypeName(ILaunchConfiguration configuration) throws CoreException {
    return super.getMainTypeName(configuration);
  }

  /**
   * Helper function to convert a string of arguments into an array of string.
   * @param arguments
   * @return
   */
  private final static String[] parseArguments(String arguments) {
    if (arguments == null) throw new IllegalArgumentException();
    return DebugPlugin.parseArguments(arguments);
  }
  
  /**
   * The method {@code getProgramArguments} returns the program arguments specified by the user
   * in the "free text" field of launch configuration.
   * You may customize this method by adding some other arguments for other reasons.
   * @param config  The launch configuration to investigate
   * @return Array of the program arguments
   * @throws CoreException
   */
  protected String[] getProgramArgumentsArray(ILaunchConfiguration config, Fixture fixture) throws CoreException {
    return parseArguments(super.getProgramArguments(config));
  }
  
  /**
   * The method {@code getVMArguments} returns the VM arguments specified by the user
   * in the "free text" field of launch configuration.
   * You may customize this method by adding some other arguments for other reasons.
   * @param config  The launch configuration to investigate
   * @return Array of the VM arguments
   * @throws CoreException
   */
  protected String[] getVMArgumentsArray(ILaunchConfiguration config, Fixture fixture) throws CoreException {
    return parseArguments(super.getVMArguments(config));
  }

  /*
  * (non-Javadoc)
  * 
  * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate2#preLaunchCheck(org.eclipse.debug.core.ILaunchConfiguration,
  *      java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
  */
  public boolean preLaunchCheck(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) {
    return true;
  }

  /* (non-Javadoc)
  * @see org.eclipse.debug.core.model.LaunchConfigurationDelegate#getBreakpoints(org.eclipse.debug.core.ILaunchConfiguration)
  */
  protected IBreakpoint[] getBreakpoints(ILaunchConfiguration configuration) {
    return new IBreakpoint[0];
  }
  
  /**
   * Add listeners to the launched process.  The default implementation does nothing.
   * @param acceptor 
   */
  protected void addProcessListeners(ILaunchConfiguration config, Fixture fixture, IProcessListenerAcceptor acceptor) {}

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
  
  public void launch(ILaunchConfiguration config, String mode, ILaunch launch, IProgressMonitor monitor)
      throws CoreException {
    Fixture fixture = getFixture(config);
    if (monitor == null) monitor = new NullProgressMonitor();
    monitor.beginTask(config.getName() + "...", 3);
    if (monitor.isCanceled()) return;
    
    try {
        monitor.subTask(messages.getString("verifying_launch_attributes"));
  
        String mainTypeName = verifyMainTypeName(config);
        IVMRunner runner = getVMRunner(config, mode);
        
        File workingDir = verifyWorkingDirectory(config);
        String workingDirName = null;
        if (workingDir != null) {
            workingDirName = workingDir.getAbsolutePath();
        }
  
        // Environment variables
        String[] envp = getEnvironment(config);
  
  
        // VM-specific attributes
        @SuppressWarnings("unchecked")
        Map<String, Object> vmAttributesMap = getVMSpecificAttributesMap(config);
  
        // Classpath
        String[] classpath = getClasspath(config);
  
        // Create VM config
        VMRunnerConfiguration runConfig = new VMRunnerConfiguration(mainTypeName, classpath);
        runConfig.setProgramArguments(getProgramArgumentsArray(config, fixture));
        runConfig.setEnvironment(envp);
        runConfig.setVMArguments(getVMArgumentsArray(config, fixture));
        runConfig.setWorkingDirectory(workingDirName);
        runConfig.setVMSpecificAttributesMap(vmAttributesMap);
  
        // Bootpath
        runConfig.setBootClassPath(getBootpath(config));
  
        // check for cancellation
        if (monitor.isCanceled()) {
            return;
        }
  
        // stop in main
        prepareStopInMain(config);
  
        // done the verification phase
        monitor.worked(1);
  
        monitor.subTask(messages.getString("creating_source_locator"));
        // set the source locator if required
        setDefaultSourceLocator(launch, config);
        monitor.worked(1);
  
        // Launch the configuration - 1 unit of work
        monitor.subTask(messages.getString("run_external_program"));
        runner.run(runConfig, launch, monitor);
        for (IProcess process: launch.getProcesses()) {
          addProcessListeners(config, fixture, new EasyLaunchConfigurationDelegateUtils.ProcessListernerAcceptorImpl(process));
        }
        
        // check for cancellation
        if (monitor.isCanceled()) return;
    } finally {
        monitor.done();
    }
  }

  // The methods below are defined in AbstractJavaLaunchConfigurationDelegate.  We "null" them
  // below, because they make sense for launching a Java project, not for launching a Java program.

  @Override
  public IJavaProject getJavaProject(ILaunchConfiguration configuration) {
    return null;
  }            
  
  @Override
  public String getJavaProjectName(ILaunchConfiguration configuration) {
    return null;
  }

  @Override
  public String verifyMainTypeName(ILaunchConfiguration configuration) throws CoreException {
    return getMainTypeName(configuration);
  }

  @Override
  public IJavaProject verifyJavaProject(ILaunchConfiguration configuration) {
    return null;
  }

  @Override
  protected void setDefaultSourceLocator(ILaunch launch, ILaunchConfiguration configuration) {}
  
  @Override
  public void handleDebugEvents(DebugEvent[] events) {}
  
  protected IProject[] getBuildOrder(ILaunchConfiguration configuration, String mode) {
    return new IProject[0];
  }

  @Override
  protected IProject[] getProjectsForProblemSearch(ILaunchConfiguration configuration, String mode) {
    return new IProject[0];
  }

  protected boolean isLaunchProblem(IMarker problemMarker) throws CoreException {
    return super.isLaunchProblem(problemMarker);  // TODO Should call super2
  }

}
