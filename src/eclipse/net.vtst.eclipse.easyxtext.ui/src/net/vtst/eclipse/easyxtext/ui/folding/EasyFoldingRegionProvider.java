// EasyXtext
// (c) Vincent Simonet, 2011

package net.vtst.eclipse.easyxtext.ui.folding;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.ui.editor.folding.DefaultFoldingRegionProvider;
import org.eclipse.xtext.util.PolymorphicDispatcher;

/**
 * Implementing folding region provider by polymorphic dispatching.
 * 
 * <p>
 * Inherited classes just have to implement specialized versions of
 * <code>_isHandled</code> and <code>_shouldProcessContent</code> for some
 * semantic classes.
 * </p>
 * 
 * @author Vincent Simonet
 */
public class EasyFoldingRegionProvider extends DefaultFoldingRegionProvider {

  private PolymorphicDispatcher<Boolean> isHandledDispatcher = PolymorphicDispatcher
      .createForSingleTarget("_isHandled", 1, 1, this);
  private PolymorphicDispatcher<Boolean> shouldProcessContentDispatcher = PolymorphicDispatcher
      .createForSingleTarget("_shouldProcessContent", 1, 1, this);

  /**
   * Perform dynamic dispatch for _isHandled. This method should not be
   * overridden in sub-classes. See _isHandled instead.
   */
  protected boolean isHandled(EObject eObject) {
    if (eObject != null)
      return isHandledDispatcher.invoke(eObject).booleanValue();
    return false;
  }

  /**
   * Perform dynamic dispatch for _shouldProcessContent. This method should not
   * be overridden in sub-classes. See _shouldProcessContent instead.
   */
  protected boolean shouldProcessContent(EObject eObject) {
    if (eObject != null)
      return shouldProcessContentDispatcher.invoke(eObject).booleanValue();
    return false;
  }

  /**
   * This methods specifies which nodes of the AST are subject to folding. It
   * can be overridden by inherited classes, with specialized arguments.
   * 
   * @param obj
   *          The object to be tested.
   * @return <code>Boolean.TRUE</code> if the object should be folded if it
   *         spans more than one line. <code>Boolean.FALSE</code> (default) if
   *         the object should not be folded.
   */
  protected Boolean _isHandled(EObject obj) {
    return Boolean.FALSE;
  }

  /**
   * This method specifies the contents of which nodes is recursively traversed
   * to find nodes that can be folded in the AST. The default implementation
   * returns <code>Boolean.TRUE</code> for all nodes, what means that the whole
   * AST is explored. This class may be overridden by inherited classes, with
   * specialized arguments.
   * 
   * @param obj
   *          The object to be tested.
   * @return <code>Boolean.TRUE</code> if the contents of the object shall be
   *         traversed. <code>Boolean.FALSE</code> if the contents of the object
   *         shall not be traversed.
   */
  protected Boolean _shouldProcessContent(EObject obj) {
    return Boolean.TRUE;
  }

}
