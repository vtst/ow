// EasyXtext
// (c) Vincent Simonet, 2011
package net.vtst.eclipse.easyxtext.scoping;

import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IGlobalScopeProvider;
import org.eclipse.xtext.scoping.IScope;

import com.google.common.base.Predicate;
import com.google.inject.Inject;

/**
 * Global scope provider supporting built-in definitions.
 * <p>
 * This global provider generates a global scope which consists in:
 * </p>
 * <ul>
 * <li>Built-in definitions which are defined in the plug-in bundle,</li>
 * <li>A global scope, which is computed by the global scope provider
 * (optional).</li>
 * </ul>
 * 
 * @author Vincent Simonet
 */
public abstract class EasyBuiltinGlobalScopeProvider implements
    IGlobalScopeProvider {

  @Inject
  private EasyBuiltinResourceDescriptionProvider builtinResourceDescriptionProvider;

  // **************************************************************************
  // Methods to be overridden in concrete implementations.

  /**
   * This method shall return the full identifier of the plugin's bundle, e.g.
   * "net.vtst.ow.eclipse.soy".
   * 
   * @return The full identifier of the plugin's bundle.
   */
  protected abstract String getBundleSymbolicName();

  /**
   * This method shall return the paths of the files which shall be read to
   * build the built-in scope. The paths shall be relative to the root of the
   * plugin's bundle.
   * 
   * @return The set of paths.
   */
  protected abstract Iterable<String> getBuiltinFiles();

  /**
   * This method shall return the global scope provider. It may return null if
   * the global scope shall include only the built-in scope. A typical
   * implementation consist in injecting the default IGlobalScopeProvider, and
   * returning it.
   * 
   * @return The global scope provider, or null.
   */
  protected abstract IGlobalScopeProvider getDelegatedGlobalScopeProvider();

  // **************************************************************************
  // Internal implementation.

  /**
   * Return the global scope computed by the delegated global scope provider.
   */
  private IScope getDelegatedScope(Resource context, EReference reference,
      Predicate<IEObjectDescription> filter) {
    IGlobalScopeProvider globalScopeProvider = getDelegatedGlobalScopeProvider();
    if (globalScopeProvider == null)
      return IScope.NULLSCOPE;
    return globalScopeProvider.getScope(context, reference, filter);
  }

  /**
   * Get the object descriptions for the built-in scope.
   */
  private Iterable<IEObjectDescription> getEObjectDescriptions() {
    return builtinResourceDescriptionProvider.getObjectDescriptions(
        getBundleSymbolicName(), getBuiltinFiles());
  }

  /**
   * Implementation of IGlobalScopeProvider.
   */
  public IScope getScope(Resource context, EReference reference,
      Predicate<IEObjectDescription> filter) {
    return new GlobalScopeWithBuiltins(getDelegatedScope(context, reference,
        filter), getEObjectDescriptions());
  }
}
