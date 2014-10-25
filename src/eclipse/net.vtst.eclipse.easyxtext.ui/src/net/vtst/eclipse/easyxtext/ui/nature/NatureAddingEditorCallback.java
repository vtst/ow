package net.vtst.eclipse.easyxtext.ui.nature;

import net.vtst.eclipse.easyxtext.nature.IEasyProjectNature;
import net.vtst.eclipse.easyxtext.nature.ProjectNatureUtil;
import net.vtst.eclipse.easyxtext.ui.EasyXtextUiMessages;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.IXtextEditorCallback;

import com.google.inject.Inject;

public class NatureAddingEditorCallback extends IXtextEditorCallback.NullImpl {
  
  private static final String QUALIFIER = "net.vtst.eclipse.easyxtext.ui.nature.NatureAddingEditorCallback";
  private static final String DISABLE = "DISABLE";

  @Inject
  private IEasyProjectNature projectNature;
  
  @Inject
  private EasyXtextUiMessages messages;
  
  protected IEasyProjectNature getNature() {
    return projectNature;
  }
  
  @Override
  public void afterCreatePartControl(XtextEditor editor) {
    try {
      super.afterCreatePartControl(editor);
      IResource resource = editor.getResource();
      if (resource == null) return;
      IProject project = resource.getProject();
      if (DISABLE.equals(project.getPersistentProperty(new QualifiedName(QUALIFIER, projectNature.getId()))))
        return;
      if (!ProjectNatureUtil.hasNature(getNature().getId(), project) && project.isAccessible()) {
        MessageDialog dialog = new MessageDialog(
            editor.getEditorSite().getShell(),
            messages.format("add_nature_to_project_title", getNature().getName()),
            null,
            messages.format("add_nature_to_project_message", getNature().getName(), project.getName()),
            MessageDialog.QUESTION, 
            new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL },
            0);
        int ret = dialog.open();
        if (ret == 0) {
          ProjectNatureUtil.addNatureRequiringXtext(getNature().getId(), project);
        } else if (ret == 1) {
          project.setPersistentProperty(new QualifiedName(QUALIFIER, projectNature.getId()), DISABLE);
        }
      }
    } catch (CoreException e) {
      ErrorDialog.openError(
          editor.getSite().getShell(),
          messages.format("add_nature_to_project_title", getNature().getName()),
          messages.getString("add_nature_to_project_error"),
          e.getStatus());
    }
  }
}
