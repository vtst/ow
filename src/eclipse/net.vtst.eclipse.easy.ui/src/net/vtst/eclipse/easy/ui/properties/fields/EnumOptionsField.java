package net.vtst.eclipse.easy.ui.properties.fields;

import net.vtst.eclipse.easy.ui.properties.editors.AbstractFieldEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.properties.stores.IReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.IStore;
import net.vtst.eclipse.easy.ui.util.SWTFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class EnumOptionsField<T extends Enum<T>> extends AbstractField<T> {
  
  private Class<T> cls;

  public EnumOptionsField(Class<T> cls) {
    this(cls, cls.getEnumConstants()[0]);
  }
  
  public EnumOptionsField(Class<T> cls, T defaultValue) {
    super(defaultValue);
    this.cls = cls;
  }

  @Override
  public T get(IReadOnlyStore store) throws CoreException {
    String value = store.get(name, (String) null);
    if (value == null) return defaultValue;
    try {
      return Enum.valueOf(cls, value);
    } catch (IllegalArgumentException e) {
      return defaultValue;
    }
  }

  @Override
  public void set(IStore store, T value) throws CoreException {
    store.set(name, value.toString());
  }

  @Override
  public AbstractFieldEditor<T> createEditor(IEditorContainer container, Composite parent) {
    return new Editor<T>(container, parent, this);
  }
  
  protected static class Editor<T extends Enum<T>> extends AbstractFieldEditor<T> {

    private Label label;
    private Button[] buttons;
    private T[] enumConstants;
    private EnumOptionsField<T> field;

    public Editor(IEditorContainer container, Composite parent, EnumOptionsField<T> field) {
      super(container, field);
      this.field = field;
      int hspan = getColumnCount(parent);
      if (hspan < 2) return;
      enumConstants = field.cls.getEnumConstants();
      buttons = new Button[enumConstants.length];
      label = SWTFactory.createLabel(parent, getMessage(), 1);
      Composite group = SWTFactory.createComposite(parent, enumConstants.length, hspan - 1, GridData.HORIZONTAL_ALIGN_BEGINNING);
      for (int i = 0; i < enumConstants.length; ++i) {
        buttons[i] = SWTFactory.createRadioButton(group, getMessage(enumConstants[i].toString()));
        buttons[i].addSelectionListener(this);
      }
    }

    @Override
    public T getCurrentValue() {
      for (int i = 0; i < buttons.length; ++i) {
        if (buttons[i].getSelection()) return enumConstants[i];
      }
      return field.defaultValue;
    }
    
    @Override
    public void setCurrentValue(T value) {
      int i = value.ordinal();
      for (int j = 0; j < buttons.length; ++j) {
        buttons[j].setSelection(i == j);
      }
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
      for (Button button: buttons) button.setEnabled(enabled);
    }    
  }
}
