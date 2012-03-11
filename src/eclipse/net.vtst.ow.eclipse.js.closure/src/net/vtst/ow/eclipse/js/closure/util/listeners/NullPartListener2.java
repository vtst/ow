package net.vtst.ow.eclipse.js.closure.util.listeners;

import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;

/**
 * A part listener which does nothing.
 * @author Vincent Simonet
 */
public class NullPartListener2 implements IPartListener2 {

  @Override
  public void partActivated(IWorkbenchPartReference partReference) {}

  @Override
  public void partBroughtToTop(IWorkbenchPartReference partReference) {}

  @Override
  public void partClosed(IWorkbenchPartReference partReference) {}

  @Override
  public void partDeactivated(IWorkbenchPartReference partReference) {}

  @Override
  public void partHidden(IWorkbenchPartReference partReference) {}

  @Override
  public void partInputChanged(IWorkbenchPartReference partReference) {}

  @Override
  public void partOpened(IWorkbenchPartReference partReference) {}

  @Override
  public void partVisible(IWorkbenchPartReference partReference) {}

}
