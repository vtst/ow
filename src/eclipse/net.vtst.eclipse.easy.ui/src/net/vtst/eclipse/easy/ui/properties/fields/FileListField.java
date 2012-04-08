package net.vtst.eclipse.easy.ui.properties.fields;

import java.io.File;
import java.io.ObjectInputStream.GetField;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.vtst.eclipse.easy.ui.listeners.NullSwtSelectionListener;
import net.vtst.eclipse.easy.ui.properties.editors.AbstractFieldEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.properties.fields.FileListField.Type;
import net.vtst.eclipse.easy.ui.properties.stores.IReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.IStore;
import net.vtst.eclipse.easy.ui.util.SWTFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;

/**
 * A field whose values are lists of files.
 * @author Vincent Simonet
 */
public class FileListField extends AbstractField<List<File>> {
  
  public enum Type { DIRECTORY, FILE }

  private Type type;
  private String[] filterExtensions;
  private String[] filterNames;

  public FileListField(Type type) {
    this(type, new String[0], new String[0]);
  }
  
  public FileListField(Type type, String[] filterExtensions, String[] filterNames) {
    super(Collections.<File>emptyList());
    this.type = type;
    this.filterExtensions = filterExtensions;
    this.filterNames = filterNames;
  }
  
  @Override
  public List<File> get(IReadOnlyStore store) throws CoreException {
    List<String> list = store.get(name, Collections.<String>emptyList());
    ArrayList<File> result = new ArrayList<File>(list.size());
    for (String s: list) result.add(new File(s));
    return result;
  }

  @Override
  public void set(IStore store, List<File> value) throws CoreException {
    ArrayList<String> list = new ArrayList<String>(value.size());
    for (File f: value) list.add(f.getAbsolutePath());
    store.set(name, list);
  }

  @Override
  public AbstractFieldEditor<List<File>> createEditor(IEditorContainer container) {
    return new Editor(container, this);
  }
  
  public static class Editor extends AbstractFieldEditor<List<File>> {
    
    private FileListField field;
    private Label label;
    private org.eclipse.swt.widgets.List list;
    private Button removeButton;
    private Button addOtherLibrary;

    public Editor(IEditorContainer container, FileListField field) {
      super(container, field);
      this.field = field;
      int hspan = container.getColumnCount();
      Composite parent = container.getComposite();
      if (hspan < 3) return;  // TODO
      label = SWTFactory.createLabel(parent, getMessage(), 1);
      GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
      gd.verticalSpan = 3;
      label.setLayoutData(gd);
      
      list = new org.eclipse.swt.widgets.List(parent, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
      gd = new GridData(GridData.FILL_BOTH);
      gd.horizontalSpan = hspan - 2;
      gd.verticalSpan = 3;
      list.setLayoutData(gd);
      
      addOtherLibrary = SWTFactory.createPushButton(parent, getMessage("add", "FileListField_add"), null);
      addOtherLibrary.addSelectionListener(new NullSwtSelectionListener() {
        @Override public void widgetSelected(SelectionEvent event) {
          addFile();
        }
      });
      removeButton = SWTFactory.createPushButton(parent, getMessage("remove", "FileListField_remove"), null);
      removeButton.addSelectionListener(new NullSwtSelectionListener() {
        @Override public void widgetSelected(SelectionEvent event) {
          removeFile();
        }
      });
      SWTFactory.createLabel(parent, "", 1);
    }
    
    @Override
    public List<File> getCurrentValue() {
      ArrayList<File> result = new ArrayList<File>(list.getItemCount());
      for (String item: list.getItems()) result.add(new File(item));
      return result;
    }

    @Override
    public void setCurrentValue(List<File> value) {
      list.removeAll();
      for (File file: value) addFile(file.getAbsolutePath());
    }

    @Override
    protected boolean computeIsValid() {
      return true;
    }

    @Override
    protected String computeErrorMessage() {
      return null;
    }
    
    private void addFile() {
      switch (field.type) {
      case DIRECTORY:
        DirectoryDialog directoryDialog = new DirectoryDialog(removeButton.getShell());
        directoryDialog.setText(getMessage("add_title", "FileListField_add_title"));
        directoryDialog.setMessage(getMessage("add_message", "FileListField_add_message"));
        String newDir = directoryDialog.open();
        if (newDir != null) addFile(newDir);
        break;
      case FILE:
        FileDialog fileDialog = new FileDialog(removeButton.getShell());
        fileDialog.setText(getMessage("add_title", "FileListField_add_title"));
        fileDialog.setFilterExtensions(field.filterExtensions);
        String[] filterNamesMessages = new String[field.filterNames.length];
        for (int i = 0; i < filterNamesMessages.length; ++i) {
          filterNamesMessages[i] = getMessage("filter_" + field.filterNames[i]);
        }
        fileDialog.setFilterNames(filterNamesMessages);
        String newFile = fileDialog.open();
        if (newFile != null) addFile(newFile);
        break;
      } 
    }

    private void addFile(String path) {
      String[] existingPaths = list.getItems();
      int i = 0;
      for (i = 0; i < existingPaths.length; ++i) {
        int cmp = existingPaths[i].compareTo(path);
        if (cmp == 0) {
          select(i);
          return;
        }
        if (cmp > 0) break;
      }
      list.add(path, i);
      select(i);
    }
    
    private void select(int index) {
      list.select(index);
      updateRemoveButton();
    }
    
    private void removeFile() {
      int index = list.getSelectionIndex();
      list.remove(index);
      int numberOfRemainingElements = list.getItemCount();
      if (numberOfRemainingElements == 0) updateRemoveButton();
      else select(Math.min(index, numberOfRemainingElements - 1));
    }

    private void updateRemoveButton() {
      removeButton.setEnabled(list.getSelectionCount() > 0);
    }

    @Override
    public void setEnabled(boolean enabled) {
      label.setEnabled(enabled);
      list.setEnabled(enabled);
      addOtherLibrary.setEnabled(enabled);
      if (enabled) updateRemoveButton();
      else removeButton.setEnabled(false);
    }
    
  }

}
