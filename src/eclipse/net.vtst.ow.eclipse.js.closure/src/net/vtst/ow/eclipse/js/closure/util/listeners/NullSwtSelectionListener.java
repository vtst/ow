package net.vtst.ow.eclipse.js.closure.util.listeners;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

/**
 * A selection listener which does nothing.
 * @author Vincent Simonet
 */
public class NullSwtSelectionListener implements SelectionListener {

  @Override
  public void widgetDefaultSelected(SelectionEvent event) {}

  @Override
  public void widgetSelected(SelectionEvent events) {}

}
