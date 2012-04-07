package net.vtst.eclipse.easy.ui.properties.editors;

import net.vtst.eclipse.easy.ui.EasyUiPlugin;
import net.vtst.eclipse.easy.ui.properties.fields.IField;
import net.vtst.eclipse.easy.ui.properties.stores.IReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.IStore;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;

/**
 * Base class for implementing field editors.
 * @author Vincent Simonet
 *
 * @param <T>  Type of the editor values.
 */
/**
 * @author vtst
 *
 * @param <T>
 */
public abstract class AbstractFieldEditor<T> 
    extends AbstractEditor 
    implements ModifyListener, SelectionListener {
  
  private IField<T> field;

  public AbstractFieldEditor(IEditorContainer container, IField<T> field) {
    super(container);
    this.field = field;
  }

  /**
   * @return  The current value for the field in the editor.
   */
  public abstract T getCurrentValue();
  
  /**
   * Set the current value for the field in the editor.
   * @param value
   */
  public abstract void setCurrentValue(T value);
  
  public void readValuesFrom(IReadOnlyStore store) throws CoreException {
    setCurrentValue(field.get(store));
  }
  
  public void setValuesToDefault() {
    setCurrentValue(field.getDefault());
  }
  
  public void writeValuesTo(IStore store) throws CoreException {
    field.set(store, getCurrentValue());
  }

  @Override
  public void modifyText(ModifyEvent event) {
    this.triggerChangeEvent();
  }
  
  @Override
  public void widgetDefaultSelected(SelectionEvent event) {}

  @Override
  public void widgetSelected(SelectionEvent event) {
    this.triggerChangeEvent();
  }
  
  private String getMessageInternal(String key) {
    String message = container.getMessage(key);
    if (message == null) return "!" + key + "!";
    return message;
  }
  
  /**
   * Get the default message associated with the editor's field.
   * @return  The message string.
   */
  protected String getMessage() {
    return getMessageInternal(field.getName());
  }
  
  /**
   * Get the message associated with a given suffix key for the editor's field.
   * @param suffix  The suffix key.
   * @return  The message string.
   */
  protected String getMessage(String suffix) {
    return getMessageInternal(field.getName() + "_" + suffix);
  }

  /**
   * Get the message associated with a given suffix key for the editor's field.
   * @param suffix  The suffix key.
   * @param defaultKey  The key to look in {@code EasyUiMessages}.
   * @return  The message string.
   */
  protected String getMessage(String suffix, String defaultKey) {
    String key = field.getName() + "_" + suffix;
    String message = container.getMessage(key);
    if (message == null) {
      message = EasyUiPlugin.getDefault().getMessages().getStringOrNull(defaultKey);
      if (message == null) message = "!" + key + "!"; 
    }
    return message;
  }
  
  protected static int getColumnCount(Composite parent) {
    Layout layout = parent.getLayout();
    if (layout instanceof GridLayout) {
      return ((GridLayout) layout).numColumns;
    } else {
      return 0;
    }
  }
}
