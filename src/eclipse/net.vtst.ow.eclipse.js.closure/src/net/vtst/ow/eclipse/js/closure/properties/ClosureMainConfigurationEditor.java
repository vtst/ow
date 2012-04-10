package net.vtst.ow.eclipse.js.closure.properties;

import net.vtst.eclipse.easy.ui.properties.editors.DefaultCompoundEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorChangeEvent;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.util.SWTFactory;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Group;

public class ClosureMainConfigurationEditor extends DefaultCompoundEditor {
  
  ClosureProjectPropertyRecord record = new ClosureProjectPropertyRecord();
  
  public ClosureMainConfigurationEditor(IEditorContainer container) {
    super(container, 1);
    Group group1 = SWTFactory.createGroup(
        getComposite(), getMessage("closureBaseGroup"), 3, 1, GridData.FILL_HORIZONTAL);
    addControl(group1);
    record.useDefaultClosureBasePath.bindEditor(this, group1);
    record.closureBasePath.bindEditor(this, group1);
  }
  
  public void editorChanged(IEditorChangeEvent event) {
    triggerChangeEvent(event);
    record.closureBasePath.editor().setEnabled(!record.useDefaultClosureBasePath.editor().getCurrentValue());
  }
  
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    if (record.useDefaultClosureBasePath.editor().getCurrentValue())
      record.closureBasePath.editor().setEnabled(false); 
  }

}
