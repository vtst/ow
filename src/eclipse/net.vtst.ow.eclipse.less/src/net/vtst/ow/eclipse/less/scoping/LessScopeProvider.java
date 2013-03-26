// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.scoping;

import java.util.ArrayList;
import java.util.List;

import net.vtst.ow.eclipse.less.less.AtVariableDef;
import net.vtst.ow.eclipse.less.less.AtVariableRefTarget;
import net.vtst.ow.eclipse.less.less.Block;
import net.vtst.ow.eclipse.less.less.BlockUtils;
import net.vtst.ow.eclipse.less.less.HashOrClass;
import net.vtst.ow.eclipse.less.less.HashOrClassRef;
import net.vtst.ow.eclipse.less.less.HashOrClassRefTarget;
import net.vtst.ow.eclipse.less.less.ImportStatement;
import net.vtst.ow.eclipse.less.less.InnerRuleSet;
import net.vtst.ow.eclipse.less.less.InnerSelector;
import net.vtst.ow.eclipse.less.less.LessPackage;
import net.vtst.ow.eclipse.less.less.Mixin;
import net.vtst.ow.eclipse.less.less.MixinParameter;
import net.vtst.ow.eclipse.less.less.MixinSelectors;
import net.vtst.ow.eclipse.less.less.MixinUtils;
import net.vtst.ow.eclipse.less.less.SimpleSelector;
import net.vtst.ow.eclipse.less.less.StyleSheet;
import net.vtst.ow.eclipse.less.less.TerminatedMixin;
import net.vtst.ow.eclipse.less.less.ToplevelRuleSet;
import net.vtst.ow.eclipse.less.less.ToplevelSelector;
import net.vtst.ow.eclipse.less.less.ToplevelStatement;
import net.vtst.ow.eclipse.less.less.VariableDefinition;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.impl.AbstractDeclarativeScopeProvider;
import org.eclipse.xtext.scoping.impl.MapBasedScope;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.util.Pair;
import org.eclipse.xtext.util.Tuples;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LessScopeProvider extends AbstractDeclarativeScopeProvider {
  
  private static final String ARGUMENTS_VARIABLE_NAME = "@arguments";

  // The cache contains pairs (LessScopeProvider.class, context) for variable scopes
  // and triples (LessScopeProvider.HashOrClassCrossReferenceclass, context, prefix) for mixin scopes.
  @Inject
  private IResourceScopeCache cache;
  
  @Inject
  private LessImportStatementResolver importStatementResolver;
      
  private Iterable<EObject> getStyleSheetStatements(StyleSheet styleSheet) {
    return styleSheet.eContents();
  }

  // **************************************************************************
  // Scoping of variables

  /** Entry point for the calculation of the scope of a cross-reference to
   * a VariableDefinitionIdent.
   */
  IScope scope_AtVariableRefTarget(EObject context, EReference ref) {
    if (MixinUtils.isBoundByMixinDefinitionParameter(context)) return IScope.NULLSCOPE;
    return computeVariableScope(context, ref);
  }

  /** Entry point for the calculation of the scope of a cross-reference to
   * a VariableDefinitionIdent.
   */
  // TODO: Delete
  IScope scope_VariableCrossReference(EObject context, EReference ref) {
    return computeVariableScope(context, ref);
  }
  
  /** Compute the scope of a context.  If the given context is a Block or a StyleSheet, call
   * computeVariableScopeOfStatements in order to lookup on the variables defined in this scope.
   * Otherwise, call the function on the container.
   * Results for Block and StyleSheet are cached.
   */
  public IScope computeVariableScope(final EObject context, EReference ref) {
    EObject container = context.eContainer();
    if (container == null) {
      return IScope.NULLSCOPE;
    } else if (container instanceof Block) {
      return computeVariableScopeOfStatements(container, BlockUtils.iterator((Block) container), ref);
    } else if (container instanceof StyleSheet) {
      return computeVariableScopeOfStatements(container, getStyleSheetStatements((StyleSheet) container), ref);
    } else if (container instanceof TerminatedMixin) {
      EStructuralFeature containingFeature = context.eContainingFeature();
      if (containingFeature.equals(LessPackage.eINSTANCE.getTerminatedMixin_Guards()) ||
          containingFeature.equals(LessPackage.eINSTANCE.getTerminatedMixin_Body())) {
        return computeVariableScopeOfMixinDefinition((TerminatedMixin) container, ref);
      }
    }
    return computeVariableScope(container, ref);
  }
    
  /** Compute the scope of a context, which contains the statements returned by iterable.
   */
  public IScope computeVariableScopeOfStatements(final EObject context, final Iterable<EObject> statements, final EReference ref) {
    return cache.get(Tuples.pair(LessScopeProvider.class, context), context.eResource(), new Provider<IScope>() {
      public IScope get() {
        List<IEObjectDescription> variableDefinitions = new ArrayList<IEObjectDescription>();
        // Go through the variables bound by the statements
        addVariableDefinitions(statements, variableDefinitions);
        return MapBasedScope.createScope(computeVariableScope(context, ref), variableDefinitions);
      }
    });
  }
  
  /**
   * Compute the scope of a mixin definition, binding the parameters of the definition.
   */
  public IScope computeVariableScopeOfMixinDefinition(final TerminatedMixin context, final EReference ref) {
    return cache.get(Tuples.pair(LessScopeProvider.class, context), context.eResource(), new Provider<IScope>() {
      public IScope get() {
        List<IEObjectDescription> variableDefinitions = new ArrayList<IEObjectDescription>();
        // Go through the variables bound by the container
        addVariableDefinitions(context, variableDefinitions);
        return MapBasedScope.createScope(computeVariableScope(context, ref), variableDefinitions);
      }
    });    
  }
  
  /** Add the variables defined by a set of statements.
   */
  private void addVariableDefinitions(Iterable<? extends EObject> statements, List<IEObjectDescription> variableDefinitions) {
    for (EObject statement: statements) {
      if (statement instanceof VariableDefinition) {
        variableDefinitions.add(getEObjectDescriptionFor(((VariableDefinition) statement).getLhs().getVariable()));
      } else if (statement instanceof ImportStatement) {
        Iterable<ToplevelStatement> importedStatements = importStatementResolver.getAllStatements((ImportStatement) statement);
        addVariableDefinitions(importedStatements, variableDefinitions);
      }
    }    
  }
  
  /** Add the variables defined by a mixin.
   */
  private void addVariableDefinitions(TerminatedMixin mixinDefinition, List<IEObjectDescription> variableDefinitions) {
    for (MixinParameter parameter: mixinDefinition.getParameters().getParameter()) {
      AtVariableRefTarget variable = MixinUtils.getVariableBoundByMixinParameter(parameter);
      if (variable != null) variableDefinitions.add(getEObjectDescriptionFor(variable));
    }
    variableDefinitions.add(EObjectDescription.create(QualifiedName.create(ARGUMENTS_VARIABLE_NAME), mixinDefinition));
  }
  
  /** Create the object description for a variable definition ident.
   */
  private IEObjectDescription getEObjectDescriptionFor(AtVariableRefTarget atVariable) {
    return EObjectDescription.create(QualifiedName.create(MixinUtils.getIdent(atVariable)), atVariable);
  }

  /** Create the object description for a variable definition ident.
   */
  private IEObjectDescription getEObjectDescriptionFor(AtVariableDef atVariable) {
    return EObjectDescription.create(QualifiedName.create(atVariable.getIdent()), atVariable);
  }

  
  // **************************************************************************
  // Scoping of mixins
  
  // Let's consider a call s_1 ... s_n and a definition d_1 ... d_m
  // s matches d if and only if one of the following condition holds:
  // * s_1 ... s_n is a subword of d_1 ... d_m
  // * m = 1 and s_1 = d_1 and s_2 ... s_n matches an element of d's block
  // Combinators are not considered.
  
  // The resolution of links for mixin calls is tricky, because the XText framework
  // is designed to resolve single cross-references, while a mixin call may consist
  // in a sequence of cross-references.  For this purpose, the following approach
  // is followed:
  // 1. Given a cross-reference, we compute the whole pattern for the cross-reference,
  // 2. We compute the matches for this pattern in the current context. The computation
  //    of these matches require walking up and then down in the tree of contexts.
  // 3. We deduce from this the scope for one of the cross-references.
  
  /** Entry point for the calculation of the scope of a cross-reference to
   * a HashOrClass.
   */
  IScope scope_HashOrClassRefTarget(EObject context, EReference ref) {
    if (MixinUtils.isBoundByMixinDefinitionSelector(context)) return IScope.NULLSCOPE;
    // First step is to get the prefix, i.e. the class and hash selectors which appear before
    // the current hash or class in the mixin call.
    ArrayList<String> pattern = null;
    int index = 0;
    if (context instanceof HashOrClassRef) {
      Pair<ArrayList<String>, Integer> result = getMixinCallPattern(context.eContainer(), (HashOrClassRef) context);
      if (result == null) return IScope.NULLSCOPE;
      pattern = result.getFirst();
      index = result.getSecond().intValue();
    } else if (context instanceof Mixin) {
      pattern = new ArrayList<String>(1);
      pattern.add("");
    }
    return computeMixinScope(context, pattern, ref, index);
  }
  
  /** Return the pattern (i.e. the preceding hashes and classes) of a cross-reference (current) in a
   * context (which should be a Mixin call, otherwise an empty context is returned).
   */
  private Pair<ArrayList<String>, Integer> getMixinCallPattern(EObject context, HashOrClassRef current) {
    int index = 0;
    if (!(context instanceof MixinSelectors)) return null;
    MixinSelectors selectors = (MixinSelectors) context;
    ArrayList<String> pattern = new ArrayList<String>(selectors.getSelector().size());
    int i = 0;
    for (HashOrClassRef item: selectors.getSelector()) {
      if (item == current) index = i;
      pattern.add(MixinUtils.getIdent(item));
      ++i;
    }
    return Tuples.pair(pattern, index);
  }

  /** Test whether a pattern matches a HashOrClass.
   * @return
   */
  private boolean isMatching(String pattern, HashOrClassRefTarget obj) {
    return pattern.isEmpty() || pattern.equals(MixinUtils.getIdent(obj));
  }
  
  private static class Match extends ArrayList<HashOrClassRefTarget> {
    private static final long serialVersionUID = 1L;

    public Match cloneAndAdd(HashOrClassRefTarget element) {
      Match clonedList = (Match) this.clone();
      clonedList.add(element);
      return clonedList;
    }

  };

  private class Matches {
    private ArrayList<Match> matches = new ArrayList<Match>(); 
    public void add(Match match) { matches.add(match); }
    public void addAll(Matches other) { matches.addAll(other.matches); }
    private QualifiedName getQualifiedName(String ident) {
      if (ident.startsWith(".")) {
        // This is because of the special interpretation of '.' by xtext when doing linking.
        // TODO A better approach would be to de-activate the special handling of . by implementing
        // IQualifiedNameConverter.
        return QualifiedName.create("", ident.substring(1));
      } else {
        return QualifiedName.create(ident);
      }
    }
    public IScope getScope(int index) { 
      List<IEObjectDescription> scopeItems = new ArrayList<IEObjectDescription>();
      for (Match match: matches) {
        if (match.size() > index) {
          HashOrClassRefTarget hashOrClass = match.get(index);
          scopeItems.add(EObjectDescription.create(getQualifiedName(MixinUtils.getIdent(hashOrClass)), hashOrClass));
        }
      }
      return MapBasedScope.createScope(IScope.NULLSCOPE, scopeItems);
    }
  }
  
  /** Collect the set of matches for a given context and pattern.  match is the list of the HashOrClass which have
   * already been matched against pattern.
   * @param context  The context for which the matches are computed.
   * @param pattern  The pattern to match
   * @param match  The HashOrClass which have already been matched against the pattern
   * @param matches  Where to add the matches.
   */
  private void computeMixinMatchesDown(EObject context, ArrayList<String> pattern, Match match, Matches matches) {
    if (context instanceof Block) {
      computeMixinMatchesDown(BlockUtils.iterator((Block) context), pattern, match, matches);
    } else if (context instanceof StyleSheet) {
      computeMixinMatchesDown(getStyleSheetStatements((StyleSheet) context), pattern, match, matches);
    }  // else do nothing
  }
  
  private void computeMixinMatchesDown(Iterable<? extends EObject> statements, ArrayList<String> pattern, Match match, Matches matches) {
    if (pattern.size() == match.size()) {
      matches.add(match);
      return;
    }
    boolean hasMatch = false;
    for (EObject obj: statements) {
      if (obj instanceof ImportStatement) {
        Iterable<ToplevelStatement> importedStatements = importStatementResolver.getAllStatements((ImportStatement) obj);
        computeMixinMatchesDown(importedStatements, pattern, match, matches);
      } else if (obj instanceof Mixin) {
        MixinUtils.Helper mixinHelper = MixinUtils.newHelper((Mixin) obj);
        if (mixinHelper.isDefinition() && mixinHelper.getSelectors().getSelector().size() == 1) {
          HashOrClassRef selector = mixinHelper.getSelectors().getSelector().get(0);
          if (isMatching(pattern.get(match.size()), selector)) {
            hasMatch = true;
            computeMixinMatchesDown(mixinHelper.getBody(), pattern, match.cloneAndAdd(selector), matches);
          }
        }
      } else if (obj instanceof ToplevelRuleSet) {
        ToplevelRuleSet toplevelRuleSet = (ToplevelRuleSet) obj;
        for (ToplevelSelector toplevelSelector: toplevelRuleSet.getSelector()) {
          Match extMatch = matchSelector(toplevelSelector.getSelector(), pattern, match);
          if (extMatch != null) {
            hasMatch = true;
            computeMixinMatchesDown(toplevelRuleSet.getBlock(), pattern, extMatch, matches);
          }
        }
      } else if (obj instanceof InnerRuleSet) {
        // This is the same code as for ToplevelRuleSet above.  It is duplicated because there is no commun super class
        // for ToplevelSelector and InnerSelector.
        InnerRuleSet innerRuleSet = (InnerRuleSet) obj;
        for (InnerSelector innerSelector: innerRuleSet.getSelector()) {
          Match extMatch = matchSelector(innerSelector.getSelector(), pattern, match);
          if (extMatch != null) {
            hasMatch = true;
            computeMixinMatchesDown(innerRuleSet.getBlock(), pattern, extMatch, matches);
          }
        }
      }      
    }
    if (!hasMatch && match.size() > 0) matches.add(match);
  }
  
  /** Try to match a pattern against a selector (made of several SimpleSelector).
   * @param selectors  The list of SimpleSelector to match against
   * @param pattern  The pattern to be matched
   * @param match  The already performed matches
   * @return null if no match, or the new match
   */
  private Match matchSelector(EList<SimpleSelector> selectors, ArrayList<String> pattern, Match match) {
    if (selectors.size() == 1 && selectors.get(0).getCriteria().size() == 1) {
      // Only one piece of selector, partial match is possible
      EObject criteria = selectors.get(0).getCriteria().get(0);
      if (criteria instanceof HashOrClass && isMatching(pattern.get(match.size()), (HashOrClass) criteria)) {
        return match.cloneAndAdd((HashOrClass) criteria);
      } else {
        return null;
      }
    } else {
      // Several pieces of selector, only complete match is possible
      int i = match.size();
      int pattern_size = pattern.size();
      Match extMatch = (Match) match.clone();
      for (SimpleSelector selector: selectors) {
        for (EObject criteria: selector.getCriteria()) {
          if (!(criteria instanceof HashOrClass)) return null;
          if (i < pattern_size && isMatching(pattern.get(i), (HashOrClass) criteria)) {
            extMatch.add((HashOrClass) criteria);
            ++i;
          }
        }
      }
      if (i < pattern_size) return null;
      else return extMatch;
    }
  }

  /** Ascending function.  Compute the matches of a context, and all its ancestors.
   * @param reference 
   */
  private Matches computeMixinMatches(final EObject context, final ArrayList<String> pattern, final EReference reference) {
    assert context != null;
    if (context instanceof Block || context instanceof StyleSheet) {
      return cache.get(Tuples.create(LessScopeProvider.class, context, pattern), context.eResource(), new Provider<Matches>() {
        public Matches get() {
          EObject container = context.eContainer();
          Matches matches = new Matches();
          if (container != null) matches.addAll(computeMixinMatches(container, pattern, reference));
          computeMixinMatchesDown(context, pattern, new Match(), matches);
          return matches;
        }
      });
    } else {
      EObject container = context.eContainer();
      if (container == null) return new Matches();
      else return computeMixinMatches(container, pattern, reference);
    }
  }

  private IScope computeMixinScope(EObject context, ArrayList<String> pattern, EReference reference, int index) {
    Matches matches = computeMixinMatches(context, pattern, reference);
    return matches.getScope(index);
  }

}