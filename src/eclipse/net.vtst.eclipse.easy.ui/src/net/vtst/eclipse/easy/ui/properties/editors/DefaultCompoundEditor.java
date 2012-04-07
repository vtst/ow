package net.vtst.eclipse.easy.ui.properties.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * Default field set editor, which disposes its widgets on a grid.
 * @author Vincent Simonet
 */
public class DefaultCompoundEditor extends AbstractCompoundEditor {

  private int numColumns;
  private Composite composite;
  
  public DefaultCompoundEditor(IEditorContainer container, int numColumns) {
    this(container, container.getComposite(), numColumns);
  }

  public DefaultCompoundEditor(IEditorContainer container, Composite parent, int numColumns) {
    super(container);
    this.numColumns = numColumns;
    composite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout(numColumns, false);
    composite.setLayout(layout);
  }

  @Override
  public int getColumnCount() {
    return numColumns;
  }

  @Override
  public Composite getComposite() {
    return composite;
  }

  @Override
  public String getMessage(String key) {
    return container.getMessage(key);
  }

  public void setEnabled(boolean enabled) {
    for (IEditor editor: editors) editor.setEnabled(enabled);
  }

}
