package net.vtst.eclipse.easy.ui.properties.fields;

import net.vtst.eclipse.easy.ui.properties.editors.AbstractFieldEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.properties.stores.IReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.IStore;
import net.vtst.eclipse.easy.ui.util.SWTFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * A field whose values are booleans, and which is edited by a check-box.
 * @author Vincent Simonet
 */
public class BooleanField extends AbstractField<Boolean> {

  public BooleanField(boolean defaultValue) {
    super(defaultValue);
  }

  @Override
  public Boolean get(IReadOnlyStore store) throws CoreException {
    return Boolean.valueOf(store.get(name, defaultValue));
  }

  @Override
  public void set(IStore store, Boolean value) throws CoreException {
    store.set(name, value.booleanValue());
  }

  @Override
  public AbstractFieldEditor<Boolean> createEditor(IEditorContainer container, Composite parent) {
    return new Editor(container, parent, this);
  }

  public class Editor extends AbstractFieldEditor<Boolean> {
    
    Button checkbox;

    public Editor(IEditorContainer container, Composite parent, IField<Boolean> field) {
      super(container, field);
      checkbox = SWTFactory.createCheckButton(
          parent, getMessage(), null, false, 
          getColumnCount(parent));
      checkbox.addSelectionListener(this);
    }

    @Override
    public Boolean getCurrentValue() {
      return Boolean.valueOf(checkbox.getSelection());
    }

    @Override
    public void setCurrentValue(Boolean value) {
      checkbox.setSelection(value.booleanValue());
    }

    @Override
    protected boolean computeIsValid() {
      return true;
    }

    @Override
    protected String computeErrorMessage() {
      return null;
    }
    
    public void setEnabled(boolean enabled) {
      checkbox.setEnabled(enabled);
    }
    
  }
  
}
