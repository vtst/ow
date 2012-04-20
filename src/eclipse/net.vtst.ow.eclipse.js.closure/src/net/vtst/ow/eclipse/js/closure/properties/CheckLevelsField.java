package net.vtst.ow.eclipse.js.closure.properties;

import java.util.Arrays;
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
import net.vtst.eclipse.easy.ui.util.SWTFactory;
import net.vtst.ow.closure.compiler.magic.MagicDiagnosticGroups;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.google.javascript.jscomp.CheckLevel;

public class CheckLevelsField extends AbstractField<Map<String, CheckLevel>> {
    
  // See http://code.google.com/p/closure-compiler/wiki/Warnings for display names.
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
      if (entry.getValue() == null) continue;
      if (first) first = false;
      else buf.append(SEPARATOR_1);
      buf.append(entry.getKey());
      buf.append(SEPARATOR_2);
      buf.append(checkLevelToString(entry.getValue()));
    }
    store.set(name, buf.toString());
  }

  @Override
  public AbstractFieldEditor<Map<String, CheckLevel>> createEditor(IEditorContainer container, Composite parent) {
    return new Editor(container, parent, this);
  }
  
  private static String checkLevelToString(CheckLevel level) {
    switch (level) {
    case ERROR: return "ERROR";
    case WARNING: return "WARNING";
    case OFF: default: return "OFF";
    }
  }
  
  private static CheckLevel checkLevelFromString(String level) {
    if ("ERROR".equals(level)) return CheckLevel.ERROR;
    if ("WARNING".equals(level)) return CheckLevel.WARNING;
    return CheckLevel.OFF;
  }
  
  private static class Editor extends AbstractFieldEditor<Map<String, CheckLevel>> {
    
    private Label label;
    private Table table;
    private String[] diagnosticGroups;
    private String[] checkLevelDisplayNames;  // 0 is default
    private Map<String, CheckLevel> currentValues;

    public Editor(IEditorContainer container, Composite parent, IField<Map<String, CheckLevel>> field) {
      super(container, field);
      int hspan = getColumnCount(parent);
      label = SWTFactory.createLabel(parent, getMessage(), hspan);
      label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
      
      createInternalTables();
      createTable(parent, hspan);
      createTableEditor();    
    }
    
    private void createInternalTables() {
      Map<String, ?> diagnosticGroupMap = (new MagicDiagnosticGroups()).getRegisteredGroups();
      diagnosticGroups = diagnosticGroupMap.keySet().toArray(new String[0]);
      Arrays.sort(diagnosticGroups);
      currentValues = new HashMap<String, CheckLevel>(diagnosticGroupMap.size());
      for (String groupName: diagnosticGroups) currentValues.put(groupName, CheckLevel.OFF);

      CheckLevel[] values = CheckLevel.values();
      checkLevelDisplayNames = new String[values.length + 1];
      checkLevelDisplayNames[0] = getMessage("DEFAULT");
      for (int i = 0; i < values.length; ++i) {
        checkLevelDisplayNames[i + 1] = getMessage(checkLevelToString(values[i]));
      }
    }
    
    private void createTable(Composite parent, int hspan) {
      // Create the widget
      table = new Table(parent, SWT.FULL_SELECTION | SWT.HIDE_SELECTION | SWT.BORDER);
      GridData layoutData = new GridData(GridData.FILL_HORIZONTAL);
      layoutData.horizontalSpan = hspan;
      layoutData.heightHint = 100;  // TODO
      table.setLayoutData(layoutData);
      
      // Create the columns
      TableColumn[] columns = new TableColumn[3];
      for (int i = 0; i < columns.length; ++i)
        columns[i] = new TableColumn(table, SWT.NONE);

      // Fill the table
      for (String groupName: diagnosticGroups) {
        TableItem item = new TableItem(table, SWT.NONE);
        item.setText(new String[] {groupName, checkLevelDisplayNames[0], getMessage(groupName)});
      }
      for (TableColumn column: columns) column.pack();
    }
    
    private void createTableEditor() {
      // Set up the table editor
      final TableEditor editor = new TableEditor(table);
      // The editor must have the same size as the cell and must
      // not be any smaller than 50 pixels.
      editor.horizontalAlignment = SWT.LEFT;
      editor.grabHorizontal = true;
      editor.minimumWidth = 50;
      // editing the column #1
      final int EDITABLECOLUMN = 1;
      
      table.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          // Clean up any previous editor control
          Control oldEditor = editor.getEditor();
          if (oldEditor != null) oldEditor.dispose();

          // Identify the selected row
          final TableItem item = (TableItem) e.item;
          if (item == null) return;

          // The control that will be the editor must be a child of the Table
          final Combo newEditor = SWTFactory.createCombo(table, SWT.READ_ONLY, 1, checkLevelDisplayNames);
          newEditor.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
              int index = newEditor.getSelectionIndex();
              if (index == 0) currentValues.remove(item.getText(0));
              else currentValues.put(item.getText(0), CheckLevel.values()[index - 1]);
              item.setText(EDITABLECOLUMN, checkLevelDisplayNames[index]);
            }
          });
          CheckLevel level = currentValues.get(item.getText(0));
          newEditor.select(level == null ? 0 : level.ordinal() + 1);
          newEditor.setFocus();
          editor.setEditor(newEditor, item, EDITABLECOLUMN);
        }
      });
    }

    @Override
    public void setEnabled(boolean enabled) {
      label.setEnabled(enabled);
      table.setEnabled(enabled);
    }

    @Override
    public Map<String, CheckLevel> getCurrentValue() {
      return Collections.unmodifiableMap(currentValues);
    }

    @Override
    public void setCurrentValue(Map<String, CheckLevel> value) {
      currentValues.clear();
      for (int i = 0; i < diagnosticGroups.length; ++i) {
        CheckLevel level = value.get(diagnosticGroups[i]);
        if (level == null) {
          table.getItem(i).setText(1, checkLevelDisplayNames[0]);
        } else {
          currentValues.put(diagnosticGroups[i], value.get(diagnosticGroups[i]));
          table.getItem(i).setText(1, checkLevelDisplayNames[level.ordinal() + 1]);
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
    
  }
}
