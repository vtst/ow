package net.vtst.eclipse.easyxtext.ui.nature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import net.vtst.eclipse.easyxtext.nature.IEasyProjectNature;
import net.vtst.eclipse.easyxtext.nature.ProjectNatureUtil;
import net.vtst.eclipse.easyxtext.ui.EasyXtextUiMessages;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.google.inject.Inject;

public class ToggleNatureAction implements IObjectActionDelegate {
  
  @Inject
  private EasyXtextUiMessages messages;
  
  @Inject
  private IEasyProjectNature projectNature;
    
  private static class Break extends Exception {
    private static final long serialVersionUID = 1L;
  }

  private ISelection selection;
  private IWorkbenchPart part;

  protected IEasyProjectNature getNature() {
    return projectNature;
  }
  
  @Override
  public void run(IAction action) {
    try {
      runInternal();
    } catch (CoreException e) {
      ErrorDialog.openError(
          getShell(),
          messages.format("project_nature_error_title", getNature().getName()),
          messages.getString("project_nature_error_message"),
          e.getStatus());
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
    if (ProjectNatureUtil.hasNature(getNature().getId(), projects.get(0))) {
      // Remove the nature
      try {
        boolean alsoRemoveXtextNature = alsoRemoveXtextNature();
        for (IProject project : projects) {
          ProjectNatureUtil.removeNatureRequiringXtext(getNature().getId(), alsoRemoveXtextNature, project);
        }      
      } catch (Break e) {}
    } else {
      // Add the nature
      for (IProject project : projects) {
        ProjectNatureUtil.addNatureRequiringXtext(getNature().getId(), project);
      }
    }
  }
  
  private Shell getShell() {
    return part.getSite().getShell();
  }
    
  private boolean alsoRemoveXtextNature() throws Break {
    MessageDialog dialog = new MessageDialog(
        getShell(),
        messages.format("remove_project_nature_dialog_title", getNature().getName()), 
        null, 
        messages.getString("remove_project_nature_dialog_message"), 
        MessageDialog.QUESTION, 
        new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL }, 
        0);
    switch (dialog.open()) {
    case 0:  // Yes
      return true;
    case 1:  // No
      return false;
    default:  // Cancel (== 2)
      throw new Break();
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
