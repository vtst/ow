package net.vtst.eclipse.easy.ui.properties.fields;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.vtst.eclipse.easy.ui.properties.editors.AbstractFieldEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.properties.stores.IReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.IStore;
import net.vtst.eclipse.easy.ui.util.SWTFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

// TODO The editor should be implemented with check boxes.
public class FlagListField extends AbstractField<Set<String>> {

  private String[] flags;

  public FlagListField(String[] flags) {
    super(Collections.<String>emptySet());
    this.flags = flags;
  }

  @Override
  public Set<String> get(IReadOnlyStore store) throws CoreException {
    return new HashSet<String>(store.get(getName(), Collections.<String>emptyList()));
  }

  @Override
  public void set(IStore store, Set<String> value) throws CoreException {
    store.set(getName(), new ArrayList<String>(value));
  }

  @Override
  public AbstractFieldEditor<Set<String>> createEditor(IEditorContainer container, Composite parent) {
    return new Editor(container, parent, this);
  }
  
  protected static class Editor extends AbstractFieldEditor<Set<String>> {

    private Label label;
    private Table list;
    private FlagListField field;

    public Editor(IEditorContainer container, Composite parent, FlagListField field) {
      super(container, field);
      this.field = field;
      int hspan = getColumnCount(parent);
      if (hspan < 2) return;  // TODO
      label = SWTFactory.createLabel(parent, getMessage(), 1);
      list = new Table(parent, SWT.V_SCROLL | SWT.CHECK | SWT.BORDER);
      list.addSelectionListener(this);
      GridData gd = new GridData(GridData.FILL_BOTH);
      gd.horizontalSpan = hspan - 1;
      list.setLayoutData(gd);
      TableColumn column = new TableColumn(list, SWT.NONE);
      for (int i = 0; i < field.flags.length; ++i) {
        TableItem item = new TableItem(list, SWT.NONE);
        item.setText(new String[] {getMessage(field.flags[i])});
      }
      column.pack();
    }

    @Override
    public Set<String> getCurrentValue() {
      Set<String> value = new HashSet<String>();
      for (int i = 0; i < field.flags.length; ++i) {
        if (list.getItem(i).getChecked()) value.add(field.flags[i]);
      }
      return value;
    }
    
    @Override
    public void setCurrentValue(Set<String> value) {
      for (int i = 0; i < field.flags.length; ++i) {
        list.getItem(i).setChecked(value.contains(field.flags[i]));
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
      list.setEnabled(enabled);
    }    
  }
}
