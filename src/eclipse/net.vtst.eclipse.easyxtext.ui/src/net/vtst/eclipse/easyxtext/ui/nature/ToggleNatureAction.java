package net.vtst.eclipse.easyxtext.ui.nature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import net.vtst.eclipse.easyxtext.nature.ProjectNatureUtil;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public abstract class ToggleNatureAction implements IObjectActionDelegate {
  
  // TODO: Could we re-use the constant from XText?
  private static String XTEXT_NATURE_ID = "org.eclipse.xtext.ui.shared.xtextNature";
  
  private static class Break extends Exception {
    private static final long serialVersionUID = 1L;
  }

  private ISelection selection;
  private IWorkbenchPart part;

  protected abstract String getNatureId();
    
  @Override
  public void run(IAction action) {
    try {
      runInternal();
    } catch (CoreException e) {
      // TODO Report properly the exception.
      e.printStackTrace();
    }
  }

  private IProject getProjectOrAdataptedProject(Object obj) {
    if (obj instanceof IProject) {
      return (IProject) obj;
    } else if (obj instanceof IAdaptable) {
      return (IProject) ((IAdaptable) obj).getAdapter(IProject.class);
    } else {
      return null;
    }
  }

  private ArrayList<IProject> getSelectedProjects() {
    if (!(selection instanceof IStructuredSelection)) return new ArrayList<IProject>();
    IStructuredSelection structuredSelection = (IStructuredSelection) selection;
    ArrayList<IProject> projects = new ArrayList<IProject>(structuredSelection.size());
    for (Iterator<?> it = structuredSelection.iterator(); it.hasNext();) {
      IProject project = getProjectOrAdataptedProject(it.next());
      if (project != null) projects.add(project);
    }
    return projects;
  }
  
  private void runInternal() throws CoreException {
    ArrayList<IProject> projects = getSelectedProjects();
    if (projects.isEmpty()) return;
    // Only the first project is tested, as all selected projects are assumed to be
    // in the same state for the handled nature.
    Collection<String> natureIds = new ArrayList<String>(2);
    natureIds.add(getNatureId());
    if (ProjectNatureUtil.hasNature(getNatureId(), projects.get(0))) {
      // Remove the nature
      try {
        if (alsoRemoveXTextNature()) {
          natureIds.add(XTEXT_NATURE_ID);
        }
        for (IProject project : projects) {
          ProjectNatureUtil.removeNatures(natureIds, project);
        }      
      } catch (Break e) {}
    } else {
      // Add the nature
      natureIds.add(XTEXT_NATURE_ID);
      for (IProject project : projects) {
        ProjectNatureUtil.addNatures(natureIds, project);
      }
    }
  }
  
  private Shell getShell() {
    return part.getSite().getShell();
  }
    
  private boolean alsoRemoveXTextNature() throws Break {
    // TODO: Messages should be customizable
    MessageDialog dialog = new MessageDialog(
        getShell(), "Remove nature", null, "Also remove the XText nature?", MessageDialog.QUESTION, 
        new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL }, 0);
    switch (dialog.open()) {
    case 0: return true;
    case 1: return false;
    default /* 2 */: throw new Break();
    }
  }

  @Override
  public void selectionChanged(IAction action, ISelection selection) {
    this.selection = selection;
  }

  @Override
  public void setActivePart(IAction action, IWorkbenchPart part) {
    this.part = part;
  }
}
