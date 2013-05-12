package net.vtst.ow.eclipse.less.scoping;

import net.vtst.ow.eclipse.less.less.Mixin;

import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.util.Tuples;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Scope provider for mixin calls.
 * 
 * Mixin calls require a more elaborated scope than the standard {@code IScope} from XText.
 * This class generates {@code MixinScope}, which are then used by the linker
 * {@code LessLinkingService} and the scope provider {@code LessScopeProvider}.
 * 
 * @author Vincent Simonet
 */
public class LessMixinScopeProvider {

  // The cache contains pairs (LessMixinScopeProvider.class, MixinScope).
  @Inject
  private IResourceScopeCache cache;

  /**
   * @param mixin  A mixin call.
   * @return The scope for the mixin call.
   */
  public MixinScope getScope(final Mixin mixin) {
    return cache.get(Tuples.pair(LessMixinScopeProvider.class, mixin), mixin.eResource(), new Provider<MixinScope>() {
      public MixinScope get() {
        return null;
      }
    });
  }
  
}
