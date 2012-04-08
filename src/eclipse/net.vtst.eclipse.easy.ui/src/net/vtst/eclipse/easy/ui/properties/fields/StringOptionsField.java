package net.vtst.eclipse.easy.ui.properties.fields;

import java.util.HashMap;
import java.util.Map;

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

public class StringOptionsField extends AbstractField<String> {
  
  private String[] values;
  private Map<String, Integer> valueMap;
  
  public StringOptionsField(String...values) {
    this(values[0], values);
  }

  public StringOptionsField(String defaultValue, String[] values) {
    super(defaultValue);
    this.values = values;
    this.valueMap = new HashMap<String, Integer>(values.length);
    for (int i = 0; i < values.length; ++i) this.valueMap.put(values[i], i);
  }

  @Override
  public String get(IReadOnlyStore store) throws CoreException {
    String value = store.get(name, defaultValue);
    if (valueMap.containsKey(value)) return value;
    return defaultValue;
  }

  @Override
  public void set(IStore store, String value) throws CoreException {
    store.set(name, value);
  }

  @Override
  public AbstractFieldEditor<String> createEditor(IEditorContainer container, Composite parent) {
    return new Editor(container, parent, this);
  }
  
  protected static class Editor extends AbstractFieldEditor<String> {

    private Label label;
    private Button[] buttons;
    private StringOptionsField field;

    public Editor(IEditorContainer container, Composite parent, StringOptionsField field) {
      super(container, field);
      this.field = field;
      int hspan = getColumnCount(parent);
      if (hspan < 2) return;
      buttons = new Button[field.values.length];
      label = SWTFactory.createLabel(parent, getMessage(), 1);
      Composite group = SWTFactory.createComposite(parent, field.values.length, hspan - 1, GridData.HORIZONTAL_ALIGN_BEGINNING);
      for (int i = 0; i < field.values.length; ++i) {
        buttons[i] = SWTFactory.createRadioButton(group, getMessage(field.values[i]));
        buttons[i].addSelectionListener(this);
      }
    }

    @Override
    public String getCurrentValue() {
      for (int i = 0; i < buttons.length; ++i) {
        if (buttons[i].getSelection()) return field.values[i];
      }
      return field.defaultValue;
    }
    
    @Override
    public void setCurrentValue(String value) {
      Integer i = field.valueMap.get(value);
      if (i != null) {
        int ii = i.intValue();
        for (int j = 0; j < buttons.length; ++j) {
          buttons[j].setSelection(ii == j);
        }
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
