package net.vtst.ow.eclipse.js.closure.listeners;

import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Listener that monitors openings and closures of parts, 
 * and adds/removes a ClosureDocumentListener to them.
 * @author Vincent Simonet
 */
public class ClosurePartListener implements IPartListener2 {

  @Override
  public void partActivated(IWorkbenchPartReference partReference) {}

  @Override
  public void partBroughtToTop(IWorkbenchPartReference partReference) {}

  @Override
  public void partClosed(IWorkbenchPartReference partReference) {
    ClosureDocumentListener.removeFrom(partReference);
  }

  @Override
  public void partDeactivated(IWorkbenchPartReference partReference) {}

  @Override
  public void partHidden(IWorkbenchPartReference partReference) {}

  @Override
  public void partInputChanged(IWorkbenchPartReference partReference) {}

  @Override
  public void partOpened(IWorkbenchPartReference partReference) {
    ClosureDocumentListener.addToIfApplicable(partReference);
  }

  @Override
  public void partVisible(IWorkbenchPartReference partReference) {}
  
  // **************************************************************************
  // Static methods
  
  private static ClosurePartListener instance = new ClosurePartListener();
  
  public static ClosurePartListener get() {
    return instance;
  }
  
  public static void addTo(IPartService partService) {
    partService.addPartListener(get());
  }
  
  public static void addTo(IWorkbench workbench) {
    for (IWorkbenchWindow window: workbench.getWorkbenchWindows()) {
      addTo(window.getPartService());
    }
  }

  public static void removeFrom(IPartService partService) {
    partService.removePartListener(get());
  }

}
