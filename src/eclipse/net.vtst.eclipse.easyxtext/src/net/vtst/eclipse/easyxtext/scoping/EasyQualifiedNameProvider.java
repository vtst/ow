// EasyXtext
// (c) Vincent Simonet, 2011
package net.vtst.eclipse.easyxtext.scoping;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.util.PolymorphicDispatcher;
import org.eclipse.xtext.util.Tuples;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Implemeting qualified name provider by polymorphic dispatching.
 * 
 * <p>
 * Helper class to implement a qualified name provider. It uses polymorphic
 * dispatching, so that inherited class just have to implement specialized
 * versions of <code>_getFullyQualifiedName</code>.
 * </p>
 * 
 * @author Vincent Simonet
 */
public class EasyQualifiedNameProvider extends
    IQualifiedNameProvider.AbstractImpl {

  private PolymorphicDispatcher<QualifiedName> isHandledDispatcher = PolymorphicDispatcher
      .createForSingleTarget("_getFullyQualifiedName", 1, 1, this);

  @Inject
  private IResourceScopeCache cache;

  /**
   * This method is called by the default implementation of the qualified name
   * provider. It calls the polymorphic dispatcher. It should not be overriden.
   */
  public QualifiedName getFullyQualifiedName(final EObject obj) {
    return cache.get(Tuples.pair(obj, EasyQualifiedNameProvider.class),
        obj.eResource(), new Provider<QualifiedName>() {
          public QualifiedName get() {
            return isHandledDispatcher.invoke(obj);
          }
        });
  }

  /**
   * This is the default implementation of the polymorphic dispatcher. It
   * returns null for every object (meaning it has no name). It may be
   * overridden for every semantic class for which names have to be defined.
   * 
   * @param obj
   *          The semantic element.
   * @return The qualified name.
   */
  protected QualifiedName _getFullyQualifiedName(EObject obj) {
    return null;
  }

}
