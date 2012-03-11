package net.vtst.ow.eclipse.js.closure.util.listeners;

import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * A window listener that does nothing.
 * @author Vincent Simonet
 */
public class NullWindowListener implements IWindowListener {

  @Override
  public void windowActivated(IWorkbenchWindow window) {}

  @Override
  public void windowClosed(IWorkbenchWindow window) {}

  @Override
  public void windowDeactivated(IWorkbenchWindow window) {}

  @Override
  public void windowOpened(IWorkbenchWindow window) {}

}
