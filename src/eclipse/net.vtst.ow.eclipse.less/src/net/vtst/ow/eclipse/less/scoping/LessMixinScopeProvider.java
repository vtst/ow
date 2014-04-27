package net.vtst.ow.eclipse.less.scoping;

import net.vtst.ow.eclipse.less.less.Block;
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
import net.vtst.ow.eclipse.less.scoping.LessImportStatementResolver.ResolvedImportStatement;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.util.Pair;
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

  @Inject
  private LessImportingStatementFinder importingStatementFinder;

  /**
   * Main entry point.  Results are memoized.
   * @param mixin  A mixin.
   * @return The scope for the mixin, if this is a mixin call, null if this is a mixin definition.  
   */
  public MixinScope getScope(final Mixin mixin) {
    assert MixinUtils.isCall(mixin);
    return cache.get(Tuples.pair(LessMixinScopeProvider.class, mixin), mixin.eResource(), new Provider<MixinScope>() {
      public MixinScope get() {
        return getScopeRec(mixin.eContainer(), null, MixinUtils.getPath(mixin));
      }
    });      
  }
  
  public MixinScope getScopeForCompletionProposal(EObject context, MixinPath path) {
    return getScopeRec(context, null, path);
  }
  
  private static <T1, T2, T3, T4> Pair<Pair<T1, T2>, Pair<T3, T4>> create4(T1 x1, T2 x2, T3 x3, T4 x4) {
    return Tuples.create(Tuples.create(x1, x2), Tuples.create(x3, x4));
  }
  
  /**
   Ascending function.  Compute the scope of a context, and all its ancestors.
   Results are memoized for interesting contexts.
   */
  private MixinScope getScopeRec(final EObject context, final EObject statementToIgnore, final MixinPath path) {
    assert context != null;  // We were return MixinScope(path);
    if (context instanceof Block || context instanceof StyleSheet) {
      return cache.get(create4(LessMixinScopeProvider.class, context, statementToIgnore, path), context.eResource(), new Provider<MixinScope>() {
        public MixinScope get() {
          MixinScope scope = getScopeContainer(context.eContainer(), context, path);
          fillScope(scope, context, statementToIgnore, 0, new MixinScopeElement());
          return scope;
        }
      });      
    } else {
      return getScopeRec(context.eContainer(), null, path);
    }
  }

  private MixinScope getScopeContainer(final EObject container, final EObject context, final MixinPath path) {
    if (container == null) {
      if (context instanceof StyleSheet) {
        ImportStatement importingStatement = importingStatementFinder.getImportingStatement(context.eResource());
        if (importingStatement != null)
          return getScopeRec(importingStatement.eContainer(), importingStatement, path);
      }
      return new MixinScope(path);
    } else {
      return new MixinScope(getScopeRec(container, null, path));
    }
  }

  /**
   Descending function.  Add elements to an existing scope.
   */
  private void fillScope(MixinScope scope, EObject context, EObject statementToIgnore, int position, MixinScopeElement element) {
    if (context instanceof Block) {
      fillScopeForBlock(scope, (Block) context, statementToIgnore, position, element);
    } else if (context instanceof StyleSheet) {
      fillScopeForStatements(scope, context.eContents(), statementToIgnore, position, element);
    } else {
      assert false;
    }
  }

  private void fillScopeForBlock(MixinScope scope, Block block, EObject statementToIgnore, int position, MixinScopeElement element) {
    fillScopeForStatements(scope, block.getContent().getStatement(), statementToIgnore, position, element);
  }
  
  private void fillScopeForStatements(
      MixinScope scope, 
      Iterable<? extends EObject> statements,
      EObject statementToIgnore,
      int position,
      MixinScopeElement element) {
    if (position >= scope.getPath().size()) {
      scope.addFullMatch(element);
    } else {
      for (EObject obj : statements) {
        if (obj.equals(statementToIgnore)) continue;
        if (obj instanceof ImportStatement) {
          ResolvedImportStatement resolvedImportStatement = importStatementResolver.resolve((ImportStatement) obj);
          if (!resolvedImportStatement.hasError()) {
            // There is no cycle, and the imported stylesheet is not null.
            fillScopeForStatements(scope, resolvedImportStatement.getImportedStyleSheet().getStatements(), null, position, element);
          }
        } else if (obj instanceof Mixin) {
          Mixin mixin = (Mixin) obj;
          if (MixinUtils.isDefinition(mixin) && mixin.getSelectors().getSelector().size() == 1) {
            HashOrClassRef selector = mixin.getSelectors().getSelector().get(0);
            String selectorIdent = MixinUtils.getIdent(selector);
            MixinScopeElement newElement = element.cloneAndExtends(selectorIdent, selector);
            scope.addAtPosition(position, newElement);          
            if (scope.getPath().isMatching(position, selectorIdent)) {
              fillScopeForBlock(scope, mixin.getBody(), null, position + 1, newElement);
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
    fillScopeForBlock(scope, block, null, position + i, newElement);
  }
}
