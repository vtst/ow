// EasyXtext
// (c) Vincent Simonet, 2011

package net.vtst.eclipse.easy.ui.launching;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IPatternMatchListener;
import org.eclipse.ui.console.TextConsole;

/**
 * Utilities shared between
 * {@link net.vtst.eclipse.easyxtext.ui.launching.EasyExtProgramLaunchConfigurationDelegate}
 * and
 * {@link net.vtst.eclipse.easyxtext.ui.launching.EasyJavaProgramLaunchConfigurationDelegate}
 * .
 * 
 * @author Vincent Simonet
 */
public class EasyLaunchConfigurationDelegateUtils {

  /**
   * Interface for listening the termination of a process.
   * 
   * @author Vincent Simonet
   */
  public static interface IProcessTerminationListener {
    /**
     * This method is called when the listened process terminates.
     * 
     * @param process
     *          The terminated process.
     * @param exitValue
     *          The exit value of the process.
     */
    public void terminated(IProcess process, int exitValue);
  }

  /**
   * Interface for an acceptors which accepts various listener for a process.
   * 
   * @author Vincent Simonet
   */
  public static interface IProcessListenerAcceptor {
    public void acceptOutputStreamMonitor(IStreamListener listener);

    public void acceptErrorStreamMonitor(IStreamListener listener);

    public void acceptPatternMatchListener(IPatternMatchListener listener);

    public void acceptTerminationListener(IProcessTerminationListener listener);
  }

  /**
   * Default implementation of {@link IProcessListenerAcceptor}
   */
  public static class ProcessListernerAcceptorImpl implements
      IProcessListenerAcceptor {
    private IProcess process;

    public ProcessListernerAcceptorImpl(IProcess process) {
      this.process = process;
    }

    @Override
    public void acceptOutputStreamMonitor(final IStreamListener listener) {
      IStreamsProxy proxy = process.getStreamsProxy();
      if (proxy == null)
        return;
      IStreamMonitor monitor = proxy.getOutputStreamMonitor();
      if (monitor == null)
        return;
      monitor.addListener(listener);
    }

    @Override
    public void acceptErrorStreamMonitor(final IStreamListener listener) {
      IStreamsProxy proxy = process.getStreamsProxy();
      if (proxy == null)
        return;
      IStreamMonitor monitor = proxy.getErrorStreamMonitor();
      if (monitor == null)
        return;
      monitor.addListener(listener);
    }

    @Override
    public void acceptPatternMatchListener(final IPatternMatchListener listener) {
      IConsole console = DebugUITools.getConsole(process);
      if (console instanceof TextConsole)
        ((TextConsole) console).addPatternMatchListener(listener);
    }

    @Override
    public void acceptTerminationListener(
        final IProcessTerminationListener listener) {
      DebugPlugin.getDefault().addDebugEventListener(
          new IDebugEventSetListener() {
            public void handleDebugEvents(DebugEvent[] events) {
              for (DebugEvent event : events) {
                if (process.equals(event.getSource())
                    && event.getKind() == DebugEvent.TERMINATE) {
                  try {
                    listener.terminated(process, process.getExitValue());
                  } catch (DebugException e) {
                    // This should never arise if the code is correct.
                  }
                }
              }
            }
          });
    }
  }
}
