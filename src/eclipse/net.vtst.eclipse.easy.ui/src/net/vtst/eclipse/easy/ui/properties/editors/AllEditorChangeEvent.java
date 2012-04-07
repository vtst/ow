package net.vtst.eclipse.easy.ui.properties.editors;

/**
 * An implementation of {@code IEditorChangeEvent} to be raised when all fields
 * of an editor are update.
 * @author Vincent Simonet
 */
public class AllEditorChangeEvent implements IEditorChangeEvent {

  @Override
  public boolean hasChanged(IEditor editor) {
    return true;
  }
  
  @Override
  public boolean propagate() {
    return false;
  }

}
