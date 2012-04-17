package net.vtst.ow.eclipse.js.closure.launching.compiler;

import java.util.List;

import net.vtst.eclipse.easy.ui.properties.editors.DefaultCompoundEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorChangeEvent;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.util.SWTFactory;
import net.vtst.ow.eclipse.js.closure.OwJsClosureMessages;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Group;

public class ClosureCompilerInputsAndOutputsEditor extends DefaultCompoundEditor {
  
  private OwJsClosureMessages messages = OwJsClosurePlugin.getDefault().getMessages();
  private ClosureCompilerLaunchConfigurationRecord record = new ClosureCompilerLaunchConfigurationRecord(); 

  public ClosureCompilerInputsAndOutputsEditor(IEditorContainer container) {
    super(container, 1);
    Group group1 = SWTFactory.createGroup(
        getComposite(), getMessage("inputsGroup"), 3, 1, GridData.FILL_BOTH);
    addControl(group1);
    record.inputResources.bindEditor(this, group1);
    record.manageClosureDependencies.bindEditor(this, group1);
    record.useAsDefault.bindEditor(this, group1);

    Group group2 = SWTFactory.createGroup(
        getComposite(), getMessage("outputsGroup"), 3, 1, GridData.FILL_HORIZONTAL);
    addControl(group2);
    record.useDefaultOutputFile.bindEditor(this, group2);
    record.outputFile.bindEditor(this, group2);
  }
  
  public void editorChanged(IEditorChangeEvent event) {
    triggerChangeEvent(event);
    record.outputFile.editor().setEnabled(!record.useDefaultOutputFile.editor().getCurrentValue());
  }
  
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    if (record.useDefaultOutputFile.editor().getCurrentValue()) 
      record.outputFile.editor().setEnabled(false); 
  }
  
  private String errorMessage = null;
  
  public boolean isValid() {
    errorMessage = null;
    if (!super.isValid()) return false;
    List<IResource> resources = record.inputResources.editor().getCurrentValue();
    int numberOfResources = resources.size();
    if (numberOfResources == 0) {
      errorMessage = messages.getString("ClosureCompilerLaunchConfigurationDelegate_noInputResource");
      return false;
    }
    if (numberOfResources > 1 && record.useDefaultOutputFile.editor().getCurrentValue()) {
      errorMessage = messages.getString("ClosureCompilerLaunchConfigurationDelegate_missingOutputFile");
      return false;      
    }
    return true;
  }
  
  public String getErrorMessage() {
    String message = super.getErrorMessage();
    if (message != null) return message;
    return errorMessage;
  }


}
