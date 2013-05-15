package net.vtst.ow.eclipse.less.scoping;

import net.vtst.ow.eclipse.less.less.Block;
import net.vtst.ow.eclipse.less.less.BlockUtils;
import net.vtst.ow.eclipse.less.less.HashOrClass;
import net.vtst.ow.eclipse.less.less.HashOrClassRef;
import net.vtst.ow.eclipse.less.less.ImportStatement;
import net.vtst.ow.eclipse.less.less.InnerRuleSet;
import net.vtst.ow.eclipse.less.less.InnerSelector;
import net.vtst.ow.eclipse.less.less.Mixin;
import net.vtst.ow.eclipse.less.less.MixinUtils;
import net.vtst.ow.eclipse.less.less.SimpleSelector;
import net.vtst.ow.eclipse.less.less.StyleSheet;
import net.vtst.ow.eclipse.less.less.ToplevelRuleSet;
import net.vtst.ow.eclipse.less.less.ToplevelSelector;
import net.vtst.ow.eclipse.less.less.ToplevelStatement;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
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

  // The cache contains:
  // (LessMixinScopeProvider.class, Mixin) -> MixinScope
  // (LessMixinScopeProvider.class, EObject, Selectors) -> MixinScope
  @Inject
  private IResourceScopeCache cache;

  @Inject
  private LessImportStatementResolver importStatementResolver;

  /**
   * Main entry point.  Results are memoized.
   * @param mixin  A mixin.
   * @return The scope for the mixin, if this is a mixin call, null if this is a mixin definition.  
   */
  public MixinScope getScope(final MixinContext context) {
    assert context.isValid();
    return cache.get(Tuples.pair(LessMixinScopeProvider.class, context.getMixin()), context.getMixin().eResource(), new Provider<MixinScope>() {
      public MixinScope get() {
        return getScopeRec(context.getMixin().eContainer(), new MixinPath(context.getMixinHelper().getSelectors().getSelector()));
      }
    });      
  }
  
  /**
   Ascending function.  Compute the scope of a context, and all its ancestors.
   Results are memoized for interesting contexts.
   */
  private MixinScope getScopeRec(final EObject context, final MixinPath path) {
    if (context == null) {
      return new MixinScope(path);
    } else if (context instanceof Block || context instanceof StyleSheet) {
      return cache.get(Tuples.create(LessMixinScopeProvider.class, context, path), context.eResource(), new Provider<MixinScope>() {
        public MixinScope get() {
          MixinScope scope = new MixinScope(getScopeRec(context.eContainer(), path));
          fillScope(scope, context, 0, new MixinScopeElement());
          return scope;
        }
      });      
    } else {
      return getScopeRec(context.eContainer(), path);
    }
  }
  
  /**
   Descending function.  Add elements to an existing scope.
   */
  private void fillScope(MixinScope scope, EObject context, int position, MixinScopeElement element) {
    if (context instanceof Block) {
      fillScope(scope, (Block) context, position, element);
    } else if (context instanceof StyleSheet) {
      fillScope(scope, context.eContents(), position, element);
    } else {
      assert false;
    }
  }

  private void fillScope(MixinScope scope, Block block, int position, MixinScopeElement element) {
    fillScope(scope, BlockUtils.iterator(block), position, element);
  }
  
  private void fillScope(
      MixinScope scope, 
      Iterable<? extends EObject> statements, 
      int position,
      MixinScopeElement element) {
    if (position >= scope.getPath().size()) {
      scope.addFullMatch(element);
    } else {
      for (EObject obj : statements) {
        if (obj instanceof ImportStatement) {
          Iterable<ToplevelStatement> importedStatements = importStatementResolver.getAllStatements((ImportStatement) obj);
          fillScope(scope, importedStatements, position, element);
        } else if (obj instanceof Mixin) {
          MixinUtils.Helper mixinHelper = MixinUtils.newHelper((Mixin) obj);
          if (mixinHelper.isDefinition() && mixinHelper.getSelectors().getSelector().size() == 1) {
            HashOrClassRef selector = mixinHelper.getSelectors().getSelector().get(0);
            String selectorIdent = MixinUtils.getIdent(selector);
            MixinScopeElement newElement = element.cloneAndExtends(selectorIdent, selector);
            scope.addAtPosition(position, newElement);          
            if (scope.getPath().isMatching(position, selectorIdent)) {
              fillScope(scope, mixinHelper.getBody(), position + 1, newElement);
            }
          }
        } else if (obj instanceof ToplevelRuleSet) {
          ToplevelRuleSet toplevelRuleSet = (ToplevelRuleSet) obj;
          for (ToplevelSelector toplevelSelector: toplevelRuleSet.getSelector()) {
            fillScopeForRuleSet(scope, obj, toplevelSelector.getSelector(), toplevelRuleSet.getBlock(), position, element);
          }
        } else if (obj instanceof InnerRuleSet) {
          InnerRuleSet innerRuleSet = (InnerRuleSet) obj;
          for (InnerSelector innerSelector: innerRuleSet.getSelector()) {
            fillScopeForRuleSet(scope, obj, innerSelector.getSelector(), innerRuleSet.getBlock(), position, element);
          }
        }
      }
    }
  }

  /**
   This method implements the common code for ToplevelRuleSet and InnerRuleSet in {@code fillScope}.
   {@code selector} and {@code} must come from {@code statement}.
   */
  private void fillScopeForRuleSet(
      MixinScope scope, 
      EObject statement,
      EList<SimpleSelector> selector,
      Block block,
      int position,
      MixinScopeElement element) {
    MixinScopeElement newElement = element;
    int i = 0;
    for (SimpleSelector simpleSelector : selector) {
      for (EObject criteria : simpleSelector.getCriteria()) {
        if (i + position >= scope.getPath().size()) return;
        if (!(criteria instanceof HashOrClass)) return;
        String ident = MixinUtils.getIdent((HashOrClass) criteria);
        newElement = newElement.cloneAndExtends(ident, criteria);
        scope.addAtPosition(position + i, newElement);
        if (!scope.getPath().isMatching(position + i, ident)) return;
        ++i;
      }
    }
    fillScope(scope, block, position + i, newElement);
  }
}
