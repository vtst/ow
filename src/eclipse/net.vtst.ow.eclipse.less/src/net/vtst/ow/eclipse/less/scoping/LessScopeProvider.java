// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.scoping;

import java.util.ArrayList;
import java.util.List;

import net.vtst.ow.eclipse.less.less.Block;
import net.vtst.ow.eclipse.less.less.BlockUtils;
import net.vtst.ow.eclipse.less.less.HashOrClass;
import net.vtst.ow.eclipse.less.less.HashOrClassCrossReference;
import net.vtst.ow.eclipse.less.less.ImportStatement;
import net.vtst.ow.eclipse.less.less.InnerRuleSet;
import net.vtst.ow.eclipse.less.less.InnerSelector;
import net.vtst.ow.eclipse.less.less.MixinCall;
import net.vtst.ow.eclipse.less.less.MixinDefinition;
import net.vtst.ow.eclipse.less.less.MixinDefinitionParameter;
import net.vtst.ow.eclipse.less.less.MixinDefinitionVariable;
import net.vtst.ow.eclipse.less.less.SimpleSelector;
import net.vtst.ow.eclipse.less.less.StyleSheet;
import net.vtst.ow.eclipse.less.less.ToplevelRuleSet;
import net.vtst.ow.eclipse.less.less.ToplevelSelector;
import net.vtst.ow.eclipse.less.less.ToplevelStatement;
import net.vtst.ow.eclipse.less.less.VariableCrossReference;
import net.vtst.ow.eclipse.less.less.VariableDefinition;
import net.vtst.ow.eclipse.less.less.VariableDefinitionIdent;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
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
  IScope scope_VariableCrossReference(EObject context, EReference ref) {
    return computeVariableScope(context, ref);
  }
  
  /** Compute the scope of a context.  If the given context is a Block or a StyleSheet, call
   * computeVariableScopeOfStatements in order to lookup on the variables defined in this scope.
   * Otherwise, call the function on the container.
   * Results for Block and StyleSheet are cached.
   */
  public IScope computeVariableScope(final EObject context, EReference ref) {
    if (context == null) {
      return IScope.NULLSCOPE;
    } else if (context instanceof Block) {
      return computeVariableScopeOfStatements(context, BlockUtils.iterator((Block) context), ref);
    } else if (context instanceof StyleSheet) {
      return computeVariableScopeOfStatements(context, getStyleSheetStatements((StyleSheet) context), ref);
    } else {
      return computeVariableScope(context.eContainer(), ref);
    }
  }
    
  /** Compute the scope of a context, which contains the statements returned by iterable.
   */
  public IScope computeVariableScopeOfStatements(final EObject context, final Iterable<EObject> statements, final EReference ref) {
    return cache.get(Tuples.pair(LessScopeProvider.class, context), context.eResource(), new Provider<IScope>() {
      public IScope get() {
        List<IEObjectDescription> variableDefinitions = new ArrayList<IEObjectDescription>();
        // Go through the variables bound by the statements
        addVariableDefinitions(statements, variableDefinitions);
        // Go through the variables bound by the container
        EObject container = context.eContainer();
        if (container instanceof MixinDefinition) {
          addVariableDefinitions((MixinDefinition) container, variableDefinitions);
        }
        return MapBasedScope.createScope(computeVariableScope(container, ref), variableDefinitions);
      }
    });
  }
  
  /** Add the variables defined by a set of statements.
   */
  private void addVariableDefinitions(Iterable<? extends EObject> statements, List<IEObjectDescription> variableDefinitions) {
    for (EObject statement: statements) {
      if (statement instanceof VariableDefinition) {
        variableDefinitions.add(getEObjectDescriptionFor(((VariableDefinition) statement).getVariable()));
      } else if (statement instanceof ImportStatement) {
        Iterable<ToplevelStatement> importedStatements = importStatementResolver.getAllStatements((ImportStatement) statement);
        addVariableDefinitions(importedStatements, variableDefinitions);
      }
    }    
  }
  
  /** Add the variables defined by a mixin.
   */
  private void addVariableDefinitions(MixinDefinition mixinDefinition, List<IEObjectDescription> variableDefinitions) {
    for (MixinDefinitionParameter parameter: mixinDefinition.getParameter()) {
      if (parameter instanceof MixinDefinitionVariable) {
        variableDefinitions.add(getEObjectDescriptionFor(((MixinDefinitionVariable) parameter).getVariable()));
      }
    }
    variableDefinitions.add(EObjectDescription.create(QualifiedName.create(ARGUMENTS_VARIABLE_NAME), mixinDefinition));
  }
  
  /** Create the object description for a variable definition ident.
   */
  private IEObjectDescription getEObjectDescriptionFor(VariableDefinitionIdent variableDefinitionIdent) {
    return EObjectDescription.create(QualifiedName.create(variableDefinitionIdent.getIdent()), variableDefinitionIdent);
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
  
  // TODO: I'm unsure whether this is useful?
  // If this is useful, there is probably a problem in LessGlobalScopeProvider.createLazyResourceScope
  IScope scope_Class(EObject context, EReference ref) {
    return scope_HashOrClass(context, ref);
  }
  
  /** Entry point for the calculation of the scope of a cross-reference to
   * a HashOrClass.
   */
  IScope scope_HashOrClass(EObject context, EReference ref) {
    // First step is to get the prefix, i.e. the class and hash selectors which appear before
    // the current hash or class in the mixin call.
    ArrayList<String> pattern = null;
    int index = 0;
    if (context instanceof HashOrClassCrossReference) {
      Pair<ArrayList<String>, Integer> result = getMixinCallPattern(context.eContainer(), (HashOrClassCrossReference) context);
      if (result == null) return IScope.NULLSCOPE;
      pattern = result.getFirst();
      index = result.getSecond().intValue();
    } else if (context instanceof MixinCall) {
      pattern = new ArrayList<String>(1);
      pattern.add("");
    }
    return computeMixinScope(context, pattern, ref, index);
  }
  
  /** Return the pattern (i.e. the preceding hashes and classes) of a cross-reference (current) in a
   * context (which should be a MixinCall, otherwise an empty context is returned).
   */
  private Pair<ArrayList<String>, Integer> getMixinCallPattern(EObject context, HashOrClassCrossReference current) {
    int index = 0;
    if (!(context instanceof MixinCall)) return null;
    MixinCall mixinCall = (MixinCall) context;
    ArrayList<String> pattern = new ArrayList<String>(mixinCall.getSelector().size());
    int i = 0;
    for (HashOrClassCrossReference item: mixinCall.getSelector()) {
      if (item == current) index = i;
      pattern.add(NodeModelUtils.getNode(item).getText());
      ++i;
    }
    return Tuples.pair(pattern, index);
  }

  /** Test whether a pattern matches a HashOrClass.
   * @return
   */
  private boolean matches(String pattern, HashOrClass obj) {
    return pattern.isEmpty() || pattern.equals(obj.getIdent());
  }

  private class Matches {
    private ArrayList<ArrayList<HashOrClass>> matches = new ArrayList<ArrayList<HashOrClass>>(); 
    public void add(ArrayList<HashOrClass> match) { matches.add(match); }
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
      for (ArrayList<HashOrClass> match: matches) {
        if (match.size() > index) {
          HashOrClass hashOrClass = match.get(index);
          scopeItems.add(EObjectDescription.create(getQualifiedName(hashOrClass.getIdent()), hashOrClass));
        }
      }
      return MapBasedScope.createScope(IScope.NULLSCOPE, scopeItems);
    }
  }
  
  private ArrayList<HashOrClass> cloneAndAdd(ArrayList<HashOrClass> list, HashOrClass element) {
    @SuppressWarnings("unchecked")
    ArrayList<HashOrClass> clonedList = (ArrayList<HashOrClass>) list.clone();
    clonedList.add(element);
    return clonedList;
  }
  
  /** Collect the set of matches for a given context and pattern.  match is the list of the HashOrClass which have
   * already been matched against pattern.
   * @param context  The context for which the matches are computed.
   * @param pattern  The pattern to match
   * @param match  The HashOrClass which have already been matched against the pattern
   * @param matches  Where to add the matches.
   */
  private void computeMixinMatchesDown(EObject context, ArrayList<String> pattern, ArrayList<HashOrClass> match, Matches matches) {
    if (context instanceof Block) {
      computeMixinMatchesDown(BlockUtils.iterator((Block) context), pattern, match, matches);
    } else if (context instanceof StyleSheet) {
      computeMixinMatchesDown(getStyleSheetStatements((StyleSheet) context), pattern, match, matches);
    }  // else do nothing
  }
  
  private void computeMixinMatchesDown(Iterable<? extends EObject> statements, ArrayList<String> pattern, ArrayList<HashOrClass> match, Matches matches) {
    if (pattern.size() == match.size()) {
      matches.add(match);
      return;
    }
    boolean hasMatch = false;
    for (EObject obj: statements) {
      if (obj instanceof ImportStatement) {
        Iterable<ToplevelStatement> importedStatements = importStatementResolver.getAllStatements((ImportStatement) obj);
        computeMixinMatchesDown(importedStatements, pattern, match, matches);
      } else if (obj instanceof MixinDefinition) {
        MixinDefinition mixinDefinition = (MixinDefinition) obj;
        if (matches(pattern.get(match.size()), mixinDefinition.getSelector())) {
          hasMatch = true;
          computeMixinMatchesDown(mixinDefinition.getBlock(), pattern, cloneAndAdd(match, mixinDefinition.getSelector()), matches);
        }
      } else if (obj instanceof ToplevelRuleSet) {
        ToplevelRuleSet toplevelRuleSet = (ToplevelRuleSet) obj;
        for (ToplevelSelector toplevelSelector: toplevelRuleSet.getSelector()) {
          ArrayList<HashOrClass> extMatch = matchSelector(toplevelSelector.getSelector(), pattern, match);
          if (extMatch != null) {
            hasMatch = true;
            computeMixinMatchesDown(toplevelRuleSet.getBlock(), pattern, extMatch, matches);
          }
        }
      } else if (obj instanceof InnerRuleSet) {
        InnerRuleSet innerRuleSet = (InnerRuleSet) obj;
        for (InnerSelector innerSelector: innerRuleSet.getSelector()) {
          ArrayList<HashOrClass> extMatch = matchSelector(innerSelector.getSelector(), pattern, match);
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
  private ArrayList<HashOrClass> matchSelector(EList<SimpleSelector> selectors, ArrayList<String> pattern, ArrayList<HashOrClass> match) {
    if (selectors.size() == 1 && selectors.get(0).getCriteria().size() == 1) {
      // Only one piece of selector, partial match is possible
      EObject criteria = selectors.get(0).getCriteria().get(0);
      if (criteria instanceof HashOrClass && matches(pattern.get(match.size()), (HashOrClass) criteria)) {
        return cloneAndAdd(match, (HashOrClass) criteria);
      } else {
        return null;
      }
    } else {
      // Several pieces of selector, only complete match is possible
      int i = match.size();
      int pattern_size = pattern.size();
      @SuppressWarnings("unchecked")
      ArrayList<HashOrClass> extMatch = (ArrayList<HashOrClass>) match.clone();
      for (SimpleSelector selector: selectors) {
        for (EObject criteria: selector.getCriteria()) {
          if (!(criteria instanceof HashOrClass)) return null;
          if (i < pattern_size && matches(pattern.get(i), (HashOrClass) criteria)) {
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
          computeMixinMatchesDown(context, pattern, new ArrayList<HashOrClass>(), matches);
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