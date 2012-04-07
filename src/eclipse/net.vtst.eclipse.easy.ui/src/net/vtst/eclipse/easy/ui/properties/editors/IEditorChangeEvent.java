package net.vtst.eclipse.easy.ui.properties.editors;

/**
 * Interface for event objects raise by editors on change of their current value.
 * @author Vincent Simonet
 */
public interface IEditorChangeEvent {
  
  /**
   * @return The editor which raises the event.
   */
  public boolean hasChanged(IEditor editor);
  
  /**
   * @return  true iff the event shall be propagated to containers.
   */
  public boolean propagate();
  
}
