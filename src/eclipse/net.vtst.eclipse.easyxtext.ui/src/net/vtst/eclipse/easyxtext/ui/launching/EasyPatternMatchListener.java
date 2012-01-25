// EasyXtext
// (c) Vincent Simonet, 2011

package net.vtst.eclipse.easyxtext.ui.launching;

import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.console.IPatternMatchListener;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

/**
 * Convenience class for easing the implementation of pattern match listeners.
 * 
 * <p>
 * Pattern match listeners are useful for creating hyperlinks in the console
 * output of jobs run by
 * {@link net.vtst.eclipse.easyxtext.ui.launching.EasyExtProgramLaunchConfigurationDelegate}
 * and
 * {@link net.vtst.eclipse.easyxtext.ui.launching.EasyJavaProgramLaunchConfigurationDelegate}
 * .
 * </p>
 * 
 * @author Vincent Simonet
 */
public abstract class EasyPatternMatchListener implements IPatternMatchListener {

  /**
   * The text console this pattern match listener is bound to. Set by
   * {@code connect}.
   */
  protected TextConsole console;

  /**
   * The document of the console this pattern match listener is bound to. Set by
   * {@code connect}.
   */
  protected IDocument document;

  /**
   * The default implementation sets the fields. This behavior may be extended
   * in sub-classes.
   */
  @Override
  public void connect(TextConsole console) {
    this.console = console;
    this.document = console.getDocument();
  }

  /**
   * The default implementation does nothing. This behavior may be overridden by
   * sub-classes.
   */
  @Override
  public void disconnect() {
  }

  /**
   * The default implementation returns {@code 0}, i.e. no flag. This behavior
   * may be overridden by sub-classes.
   */
  @Override
  public int getCompilerFlags() {
    return 0;
  }

  /**
   * The default implementation returns {@code null}, what means that all lines
   * are qualified. This behavior may be overriden by sub-classes.
   */
  @Override
  public String getLineQualifier() {
    return null;
  }

  @Override
  public abstract void matchFound(PatternMatchEvent arg0);

  @Override
  public abstract String getPattern();

}
