package net.vtst.ow.eclipse.js.closure.properties.project;

import net.vtst.eclipse.easy.ui.properties.editors.DefaultCompoundEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;

public class ClosureMainConfigurationEditor extends DefaultCompoundEditor {
  
  ClosureProjectPropertyRecord record = new ClosureProjectPropertyRecord();
  
  public ClosureMainConfigurationEditor(IEditorContainer container) {
    super(container, 1);
  }
}
