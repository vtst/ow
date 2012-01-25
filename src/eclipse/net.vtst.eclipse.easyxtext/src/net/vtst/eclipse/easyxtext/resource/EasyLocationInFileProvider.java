// EasyXtext
// (c) Vincent Simonet, 2011

package net.vtst.eclipse.easyxtext.resource;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.resource.DefaultLocationInFileProvider;
import org.eclipse.xtext.util.PolymorphicDispatcher;

/**
 * Implementing location-in-file provider by polymorphic dispatching.
 * 
 * <p>
 * Inherited classes just have to implement specialized versions of
 * <code>_getIdentifierFeature</code> for some semantic classes.
 * </p>
 * 
 * @author Vincent Simonet
 */
public class EasyLocationInFileProvider extends DefaultLocationInFileProvider {

  private PolymorphicDispatcher<EStructuralFeature> getIdentifierFeatureDispatcher = PolymorphicDispatcher
      .createForSingleTarget("_getIdentifierFeature", 1, 1, this);

  /**
   * This method is called by the default implementation of the location
   * provider. It calls the polymorphic dispatcher. It should not be overridden.
   */
  protected EStructuralFeature getIdentifierFeature(EObject obj) {
    EStructuralFeature structuralFeature = getIdentifierFeatureDispatcher
        .invoke(obj);
    if (structuralFeature != null)
      return structuralFeature;
    return super.getIdentifierFeature(obj);
  }

  /**
   * Default implementation of the polymorphic dispatcher. This method should be
   * overridden with a specialized version for each relevant semantic class.
   * 
   * @param obj
   *          The semantic element.
   * @return The structural feature which contains the identifier for the given
   *         semantic element.
   */
  protected EStructuralFeature _getIdentifierFeature(EObject obj) {
    return null;
  }

}
