package net.vtst.ow.eclipse.js.closure.properties;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Group;

import net.vtst.eclipse.easy.ui.properties.editors.DefaultCompoundEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorChangeEvent;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.util.SWTFactory;

public class ClosureIncludesEditor extends DefaultCompoundEditor {

  private ClosureProjectPropertyRecord record = new ClosureProjectPropertyRecord();

  public ClosureIncludesEditor(IEditorContainer container) {
    super(container, 1);
    Group group1 = SWTFactory.createGroup(
        getComposite(), getMessage("closureBaseGroup"), 3, 1, GridData.FILL_HORIZONTAL);
    addControl(group1);
    record.useDefaultClosureBasePath.bindEditor(this, group1);
    record.closureBasePath.bindEditor(this, group1);
    
    Group group2 = SWTFactory.createGroup(
        getComposite(), getMessage("otherIncludesGroup"), 3, 1, GridData.FILL_BOTH);
    addControl(group2);
    record.otherLibraries.bindEditor(this, group2);
    SWTFactory.createLabel(group2, getMessage("otherLibrariesHelp"), 3);
    record.externs.bindEditor(this, group2);
    record.useOnlyCustomExterns.bindEditor(this, group2);
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
