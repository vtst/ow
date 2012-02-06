package net.vtst.ow.eclipse.js.closure.listeners;

import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Listener that monitors openings and closures of windows, 
 * and adds/removes a ClosurePartListener to them.
 * @author Vincent Simonet
 */
public class ClosureWindowListener implements IWindowListener {

  @Override
  public void windowActivated(IWorkbenchWindow window) {}

  @Override
  public void windowClosed(IWorkbenchWindow window) {
    ClosurePartListener.removeFrom(window.getPartService());    
  }

  @Override
  public void windowDeactivated(IWorkbenchWindow window) {}

  @Override
  public void windowOpened(IWorkbenchWindow window) {
    ClosurePartListener.addTo(window.getPartService());
  }
  
  // **************************************************************************
  // Static methods
  
  private static ClosureWindowListener instance = new ClosureWindowListener();
  
  public static ClosureWindowListener get() {
    return instance;
  }
  
  public static void addTo(IWorkbench workbench) {
    workbench.addWindowListener(get());
  }
  
  public static void removeFrom(IWorkbench workbench) {
    workbench.removeWindowListener(get());
  }

}
