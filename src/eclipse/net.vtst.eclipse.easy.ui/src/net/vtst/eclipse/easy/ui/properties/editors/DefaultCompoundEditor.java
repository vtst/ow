package net.vtst.eclipse.easy.ui.properties.editors;

import java.util.ArrayList;
import java.util.List;

import net.vtst.eclipse.easy.ui.util.SWTFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;

/**
 * Default field set editor, which disposes its widgets on a grid.
 * @author Vincent Simonet
 */
public class DefaultCompoundEditor extends AbstractCompoundEditor {

  private Composite composite;
  private List<Control> controls = new ArrayList<Control>();
  
  public DefaultCompoundEditor(IEditorContainer container, int numColumns) {
    this(container, container.getComposite(), numColumns);
  }

  public DefaultCompoundEditor(IEditorContainer container, Composite parent, int numColumns) {
    super(container);
    composite = SWTFactory.createComposite(parent, numColumns, 1, GridData.FILL_BOTH);
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
    for (Control control: controls) control.setEnabled(enabled);
  }
  
  public void addControl(Control control) {
    controls.add(control);
  }

}
