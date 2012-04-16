package net.vtst.ow.eclipse.js.closure.launching.compiler;

import net.vtst.eclipse.easy.ui.properties.editors.DefaultCompoundEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorChangeEvent;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.properties.fields.BooleanField;

public abstract class ClosureCompilerProjectPropertiesEditor extends DefaultCompoundEditor {
  
  private BooleanField field;
  private IEditor delegate;
  
  public ClosureCompilerProjectPropertiesEditor(IEditorContainer container, BooleanField field) {
    super(container, 1);
    this.field = field;
    field.bindEditor(this);
    this.delegate = createDelegate(this);
  }
  
  public abstract IEditor createDelegate(IEditorContainer container);

  public void editorChanged(IEditorChangeEvent event) {
    triggerChangeEvent(event);
    delegate.setEnabled(!field.editor().getCurrentValue());
  }
  
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    if (field.editor().getCurrentValue()) delegate.setEnabled(false); 
  }

}
