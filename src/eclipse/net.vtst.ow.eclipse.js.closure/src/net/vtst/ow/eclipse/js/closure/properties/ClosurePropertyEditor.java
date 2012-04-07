package net.vtst.ow.eclipse.js.closure.properties;

import net.vtst.eclipse.easy.ui.properties.editors.DefaultCompoundEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.util.SWTFactory;

public class ClosurePropertyEditor extends DefaultCompoundEditor {
  
  ClosureProjectPropertyRecord record = new ClosureProjectPropertyRecord();
  
  public ClosurePropertyEditor(IEditorContainer container) {
    super(container, 3);
    record.closureBasePath.bindEditor(this);
    record.otherLibraries.bindEditor(this);
    addControl(SWTFactory.createLabel(container.getComposite(), getMessage("help"), 3));
  }

}
