package net.vtst.ow.eclipse.js.closure.util.listeners;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;

/**
 * A document listener that does nothing.
 * @author Vincent Simonet
 */
public class NullDocumentListener implements IDocumentListener {

  @Override
  public void documentAboutToBeChanged(DocumentEvent event) {}

  @Override
  public void documentChanged(DocumentEvent event) {}

}
