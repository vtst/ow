package net.vtst.ow.eclipse.js.closure.properties;

import net.vtst.eclipse.easy.ui.properties.editors.DefaultCompoundEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorChangeEvent;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.util.SWTFactory;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Group;

public class ClosureIncludesEditor extends DefaultCompoundEditor {

  private ClosureProjectPropertyRecord record = new ClosureProjectPropertyRecord();

  public ClosureIncludesEditor(IEditorContainer container) {
    super(container, 1);
    Group group1 = SWTFactory.createGroup(
        getComposite(), getMessage("closureBaseGroup"), 3, 1, GridData.FILL_HORIZONTAL);
    addControl(group1);
    record.includes.useDefaultClosureBasePath.bindEditor(this, group1);
    record.includes.closureBasePath.bindEditor(this, group1);
    
    Group group2 = SWTFactory.createGroup(
        getComposite(), getMessage("otherIncludesGroup"), 3, 1, GridData.FILL_BOTH);
    addControl(group2);
    record.includes.otherLibraries.bindEditor(this, group2);
    SWTFactory.createLabel(group2, getMessage("otherLibrariesHelp"), 3);
    record.includes.externs.bindEditor(this, group2);
    record.includes.useOnlyCustomExterns.bindEditor(this, group2);
  }

  public void editorChanged(IEditorChangeEvent event) {
    triggerChangeEvent(event);
    record.includes.closureBasePath.editor().setEnabled(!record.includes.useDefaultClosureBasePath.editor().getCurrentValue());
  }
  
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    if (record.includes.useDefaultClosureBasePath.editor().getCurrentValue())
      record.includes.closureBasePath.editor().setEnabled(false); 
  }

}
