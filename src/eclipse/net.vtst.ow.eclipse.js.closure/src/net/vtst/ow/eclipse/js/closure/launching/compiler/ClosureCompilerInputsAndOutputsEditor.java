package net.vtst.ow.eclipse.js.closure.launching.compiler;

import net.vtst.eclipse.easy.ui.properties.editors.DefaultCompoundEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorChangeEvent;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.util.SWTFactory;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Group;

public class ClosureCompilerInputsAndOutputsEditor extends DefaultCompoundEditor {
  
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

}
