package net.vtst.ow.eclipse.js.closure.properties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.vtst.eclipse.easy.ui.properties.editors.AbstractFieldEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.properties.fields.AbstractField;
import net.vtst.eclipse.easy.ui.properties.fields.IField;
import net.vtst.eclipse.easy.ui.properties.stores.IReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.IStore;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import org.eclipse.core.runtime.CoreException;

import com.google.javascript.jscomp.CheckLevel;

public class CheckLevelsField extends AbstractField<Map<String, CheckLevel>> {
  
  private static String SEPARATOR_1 = "\n";
  private static String SEPARATOR_2 = "=";

  public CheckLevelsField() {
    super(Collections.<String, CheckLevel>emptyMap());
  }

  @Override
  public Map<String, CheckLevel> get(IReadOnlyStore store) throws CoreException {
    String value = store.get(name, "");
    Map<String, CheckLevel> map = new HashMap<String, CheckLevel>();
    for (String s: value.split(SEPARATOR_1)) {
      String[] hs = s.split(SEPARATOR_2);
      if (hs.length == 2) {
        map.put(hs[0], checkLevelFromString(hs[1]));
      }
    }
    return map;
  }

  @Override
  public void set(IStore store, Map<String, CheckLevel> value) throws CoreException {
    StringBuffer buf = new StringBuffer();
    boolean first = true;
    for (Entry<String, CheckLevel> entry: value.entrySet()) {
      if (first) first = false;
      else buf.append(SEPARATOR_1);
      buf.append(entry.getKey());
      buf.append(SEPARATOR_2);
      buf.append(checkLevelToString(entry.getValue()));
    }
    store.set(name, buf.toString());
  }

  @Override
  public AbstractFieldEditor<Map<String, CheckLevel>> createEditor(IEditorContainer container) {
    return new Editor(container, this);
  }
  
  private String checkLevelToString(CheckLevel level) {
    switch (level) {
    case ERROR: return "ERROR";
    case WARNING: return "WARNING";
    case OFF: default: return "OFF";
    }
  }
  
  private CheckLevel checkLevelFromString(String level) {
    if ("ERROR".equals(level)) return CheckLevel.ERROR;
    if ("WARNING".equals(level)) return CheckLevel.WARNING;
    return CheckLevel.OFF;
  }
  
  private static class Editor extends AbstractFieldEditor<Map<String, CheckLevel>> {

    public Editor(IEditorContainer container, IField<Map<String, CheckLevel>> field) {
      super(container, field);
      Composite parent = container.getComposite();
      Table table = new Table(parent, SWT.NONE);
      TableColumn column1 = new TableColumn(table, SWT.NONE);
      column1.setText("Check");
      TableColumn column2 = new TableColumn(table, SWT.NONE);
      column2.setText("Level");
      TableItem item = new TableItem(table, SWT.NONE);
      item.setText(new String[]{"a", "b"});
    }

    @Override
    public void setEnabled(boolean enabled) {
    }

    @Override
    public Map<String, CheckLevel> getCurrentValue() {
      // TODO Auto-generated method stub
      return Collections.emptyMap();
    }

    @Override
    public void setCurrentValue(Map<String, CheckLevel> value) {
      // TODO Auto-generated method stub      
    }

    @Override
    protected boolean computeIsValid() {
      return true;
    }

    @Override
    protected String computeErrorMessage() {
      return null;
    }
    
  }
}
