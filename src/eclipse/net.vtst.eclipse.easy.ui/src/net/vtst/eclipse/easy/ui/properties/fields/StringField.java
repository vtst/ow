package net.vtst.eclipse.easy.ui.properties.fields;

import net.vtst.eclipse.easy.ui.properties.editors.AbstractFieldEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.properties.stores.IReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.IStore;
import net.vtst.eclipse.easy.ui.util.SWTFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class StringField extends AbstractField<String> {


  public StringField(String defaultValue) {
    super(defaultValue);
  }
  
  @Override
  public String get(IReadOnlyStore store) throws CoreException {
    return store.get(this.name, defaultValue);
  }

  @Override
  public void set(IStore store, String value) throws CoreException {
    store.set(this.name, value);
  }

  @Override
  public AbstractFieldEditor<String> createEditor(IEditorContainer container, Composite parent) {
    return new Editor(container, parent, this);
  }
  
  protected static class Editor extends AbstractFieldEditor<String> {

    private Label label;
    private Text text;

    public Editor(IEditorContainer container, Composite parent, IField<String> field) {
      super(container, field);
      int hspan = getColumnCount(parent);
      if (hspan < 2) return;
      label = SWTFactory.createLabel(parent, getMessage(), 1);
      text = SWTFactory.createSingleText(parent, hspan - 1);
      text.addModifyListener(this);
    }

    @Override
    public String getCurrentValue() {
      return text.getText();
    }
    
    @Override
    public void setCurrentValue(String value) {
      text.setText(value);
    }

    @Override
    protected boolean computeIsValid() {
      return true;
    }

    @Override
    protected String computeErrorMessage() {
      return null;
    }

    @Override
    public void setEnabled(boolean enabled) {
      label.setEnabled(enabled);
      text.setEnabled(enabled);
    }    
  }
}
