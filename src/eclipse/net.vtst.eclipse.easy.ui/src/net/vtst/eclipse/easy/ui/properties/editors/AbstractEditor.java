package net.vtst.eclipse.easy.ui.properties.editors;

/**
 * Base implementation for {@code IEditor}.  This is the code shared between
 * {@code AbstractFieldEditor} and {@code AbstractFieldSetEditor}.
 * @author Vincent Simonet
 */
public abstract class AbstractEditor implements IEditor {
  
  protected IEditorContainer container;
  private boolean isValidDirty = true;
  private boolean isValid = false;
  private boolean errorMessageDirty = true;
  private String errorMessage = null;

  public AbstractEditor(IEditorContainer container) {
    this.container = container;
    container.addEditor(this);
  }
  
  public IEditorContainer getContainer() {
    return container;
  }
  
  /**
   * @return  true iif the current value of the field is valid.
   */
  protected abstract boolean computeIsValid();

  @Override
  public boolean isValid() {
    if (isValidDirty) {
      isValid = computeIsValid();
      isValidDirty = false;
    }
    return isValid;
  }

  /**
   * @return  The error message to display, or null.
   */
  protected abstract String computeErrorMessage();
  
  @Override
  public String getErrorMessage() {
    if (errorMessageDirty) {
      errorMessage = computeErrorMessage();
      errorMessageDirty = false;
    }
    return errorMessage;
  }
  
  /**
   * Trigger a change event for the current editor.
   */
  protected void triggerChangeEvent() {
    triggerChangeEvent(new EditorChangeEvent(this));
  }
  
  /**
   * Propagate a change event.
   * @param event  The event to propagate.
   */
  protected void triggerChangeEvent(IEditorChangeEvent event) {
    isValidDirty = true;
    errorMessageDirty = true;
    if (event.propagate()) container.editorChanged(event);
  }

}
