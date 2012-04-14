package net.vtst.eclipse.easy.ui.properties.editors;

import org.eclipse.swt.widgets.Composite;

/**
 * Interface for objects containing editors.
 * @author Vincent Simonet
 *
 */
public interface IEditorContainer {
  
  /**
   * Register an editor in the container.  This method is intended to be called by the constructor
   * of the editor.
   * @param editor  The editor to register.
   */
  public void addEditor(IEditor editor);
  
  /**
   * This method is intended to be called by contained editor when their state changes.
   * @param event
   */
  public void editorChanged(IEditorChangeEvent event);
  
  /**
   * @return  The composite to which the contained editor may add their widgets.
   */
  public Composite getComposite();
    
  /**
   * Get a message string for user display.
   * @param key  The key of the message string.
   * @return  The message string.
   */
  public String getMessage(String key);
}
