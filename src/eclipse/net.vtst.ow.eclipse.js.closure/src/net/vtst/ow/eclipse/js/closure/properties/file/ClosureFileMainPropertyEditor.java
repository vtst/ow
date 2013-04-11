package net.vtst.ow.eclipse.js.closure.properties.file;

import net.vtst.eclipse.easy.ui.properties.editors.DefaultCompoundEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;

public class ClosureFileMainPropertyEditor extends DefaultCompoundEditor {
  
  ClosureFilePropertyRecord record = new ClosureFilePropertyRecord();
  
  public ClosureFileMainPropertyEditor(IEditorContainer container) {
    super(container, 3);
    record.generatedByCompiler.bindEditor(this);
  }
}
