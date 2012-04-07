package net.vtst.eclipse.easy.ui.properties.editors;

import org.eclipse.swt.widgets.Composite;

/**
 * Interface for an editor which fits in a single composite widget.
 * @author Vincent Simonet
 */
public interface ICompositeEditor extends IEditor {

  /**
   * @return  The composite widget for the editor.
   */
  public Composite getComposite();

}
