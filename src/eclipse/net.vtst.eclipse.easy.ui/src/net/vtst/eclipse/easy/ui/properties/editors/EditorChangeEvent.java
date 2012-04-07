package net.vtst.eclipse.easy.ui.properties.editors;

/**
 * Default implementation of {@code IEditorChangeEvent}.
 * @author Vincent Simonet
 */
public class EditorChangeEvent implements IEditorChangeEvent {

  private IEditor editor;

  /**
   * @param editor  The editor which raised the event.
   */
  public EditorChangeEvent(IEditor editor) {
    this.editor = editor;
  }
  
  @Override
  public boolean hasChanged(IEditor otherEditor) {
    return editor.equals(otherEditor);
  }
  
  @Override
  public boolean propagate() {
    return true;
  }

}
