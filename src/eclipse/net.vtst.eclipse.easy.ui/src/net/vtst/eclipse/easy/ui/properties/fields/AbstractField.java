package net.vtst.eclipse.easy.ui.properties.fields;

import net.vtst.eclipse.easy.ui.properties.editors.AbstractFieldEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.properties.stores.IReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.IStore;

import org.eclipse.core.runtime.CoreException;

/**
 * Base class for fields.
 * @author Vincent Simonet
 *
 * @param <T>  The type of the field values.
 */
public abstract class AbstractField<T> implements IField<T> {
  
  private AbstractFieldEditor<T> editor;
  protected String name = "undefined";
  protected T defaultValue;
  
  /**
   * Create a new field with a given default value.
   * @param defaultValue
   */
  public AbstractField(T defaultValue) {
    this.defaultValue = defaultValue;
  }
  
  public void bind(String name) {
    this.name = name;
  }
  
  public String getName() {
    return this.name;
  }
  
  public abstract T get(IReadOnlyStore store) throws CoreException;
  public abstract void set(IStore store, T value) throws CoreException;
  
  public T getDefault() {
    return defaultValue;
  }
  
  /**
   * Create an editor for the current field.
   * @param container  The container for the newly created editor.
   * @return  The newly created editor.
   */
  abstract public AbstractFieldEditor<T> createEditor(IEditorContainer container);
  
  /**
   * Create an editor for the current field, into the container.
   * @param container
   */
  public void bindEditor(IEditorContainer container) {
    editor = createEditor(container);
  }
  
  /**
   * @return  The last created editor for the current field.
   */
  public AbstractFieldEditor<T> editor() { return editor; }
}
