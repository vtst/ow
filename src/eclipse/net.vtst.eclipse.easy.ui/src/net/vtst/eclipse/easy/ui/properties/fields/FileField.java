package net.vtst.eclipse.easy.ui.properties.fields;

import java.io.File;

import net.vtst.eclipse.easy.ui.listeners.NullSwtSelectionListener;
import net.vtst.eclipse.easy.ui.properties.editors.AbstractFieldEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.properties.stores.IReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.IStore;
import net.vtst.eclipse.easy.ui.util.SWTFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * A field whose values are fiels, and which is edited by a text box with a browse button.
 * @author Vincent Simonet
 */
public class FileField extends AbstractField<File> {
  
  public enum Type { DIRECTORY }

  public FileField(String defaultValue, Type type) {
    super(new File(defaultValue));
    // this.type = type;
  }
  
  @Override
  public File get(IReadOnlyStore store) throws CoreException {
    String value = store.get(name, (String) null);
    if (value == null) return defaultValue;
    return new File(value);
  }

  @Override
  public void set(IStore store, File value) throws CoreException {
    store.set(name, value.getAbsolutePath());
  }

  @Override
  public AbstractFieldEditor<File> createEditor(IEditorContainer container, Composite parent) {
    return new Editor(container, parent, this);
  }
  
  public static class Editor extends AbstractFieldEditor<File> {
    
    private Label label;
    private Text text;
    private Button buttonBrowse;

    public Editor(IEditorContainer container, Composite parent, IField<File> field) {
      super(container, field);
      int hspan = getColumnCount(parent);
      if (hspan < 3) return;  // TODO
      label = SWTFactory.createLabel(parent, getMessage(), 1);
      text = SWTFactory.createSingleText(parent, hspan - 2);
      text.addModifyListener(this);
      buttonBrowse = SWTFactory.createPushButton(parent, getMessage("browse", "FileField_browse"), null);
      buttonBrowse.addSelectionListener(new NullSwtSelectionListener() {
        @Override public void widgetSelected(SelectionEvent arg0) {
          browse();
        }
      });
    }
    
    private void browse() {
      DirectoryDialog dialog = new DirectoryDialog(text.getShell());
      dialog.setText(getMessage("browse_title", "FileField_browse_title"));
      dialog.setMessage(getMessage("browse_message", "FileField_browse_message"));
      dialog.setFilterPath(text.getText());
      String newDir = dialog.open();
      if (newDir != null) text.setText(newDir);
    }

    @Override
    public File getCurrentValue() {
      return new File(text.getText());
    }

    @Override
    public void setCurrentValue(File value) {
      text.setText(value.getAbsolutePath());
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
      buttonBrowse.setEnabled(enabled);
    }
    
  }

}
