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
  }
}
