package net.vtst.eclipse.easy.ui.properties.editors;

import java.util.ArrayList;
import java.util.List;

import net.vtst.eclipse.easy.ui.properties.stores.IReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.IStore;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Composite;

/**
 * Base class for implementing field set editors.
 * @author Vincent Simonet
 */
public abstract class AbstractCompoundEditor extends AbstractEditor implements IEditorContainer, ICompositeEditor {

  List<IEditor> editors = new ArrayList<IEditor>();
  
  public AbstractCompoundEditor(IEditorContainer container) {
    super(container);
  }

  @Override
  public void readValuesFrom(IReadOnlyStore store) throws CoreException {
    for (IEditor editor: editors) editor.readValuesFrom(store);
    editorChanged(new AllEditorChangeEvent());
  }
  
  @Override
  public boolean hasChanged(IReadOnlyStore store) throws CoreException {
    for (IEditor editor: editors) {
      if (editor.hasChanged(store)) return true;
    }
    return false;
  }

  
  @Override
  public void setValuesToDefault() {
    for (IEditor editor: editors) editor.setValuesToDefault();    
    editorChanged(new AllEditorChangeEvent());
  }


  @Override
  public void writeValuesTo(IStore store) throws CoreException {
    for (IEditor editor: editors) editor.writeValuesTo(store);
  }

  @Override
  public void addEditor(IEditor editor) {
    editors.add(editor);
  }

  @Override
  public void editorChanged(IEditorChangeEvent event) {
    triggerChangeEvent(event);
  }

  @Override
  protected boolean computeIsValid() {
    for (IEditor editor: editors) {
      if (!editor.isValid()) return false;
    }
    return true;
  }

  @Override
  protected String computeErrorMessage() {
    for (IEditor editor: editors) {
      String message = editor.getErrorMessage();
      if (message != null) return message;
    }
    return null;
  }

  @Override
  public abstract Composite getComposite();
}
