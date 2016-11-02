// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.scoping;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.impl.AbstractDeclarativeScopeProvider;
import org.eclipse.xtext.scoping.impl.MapBasedScope;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.util.Tuples;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Provider;

import net.vtst.ow.eclipse.less.ModelUtils;
import net.vtst.ow.eclipse.less.less.AtVariableDef;
import net.vtst.ow.eclipse.less.less.AtVariableRefTarget;
import net.vtst.ow.eclipse.less.less.Block;
import net.vtst.ow.eclipse.less.less.HashOrClassRef;
import net.vtst.ow.eclipse.less.less.ImportStatement;
import net.vtst.ow.eclipse.less.less.LessPackage;
import net.vtst.ow.eclipse.less.less.Mixin;
import net.vtst.ow.eclipse.less.less.MixinParameter;
import net.vtst.ow.eclipse.less.less.MixinUtils;
import net.vtst.ow.eclipse.less.less.StyleSheet;
import net.vtst.ow.eclipse.less.less.VariableDefinition;
import net.vtst.ow.eclipse.less.linking.LessMixinLinkingService;
import net.vtst.ow.eclipse.less.linking.LessMixinLinkingService.MixinLink;
import net.vtst.ow.eclipse.less.scoping.LessImportStatementResolver.ResolvedImportStatement;

public class LessScopeProvider extends AbstractDeclarativeScopeProvider {
  
  private static final String ARGUMENTS_VARIABLE_NAME = "@arguments";

  // The cache contains:
  // (LessScopeProvider.class, context) -> LessScopeProvider.class
  @Inject
  private IResourceScopeCache cache;
  
  @Inject
  private LessImportStatementResolver importStatementResolver;

  @Inject
  private LessImportingStatementFinder importingStatementFinder;

  @Inject
  private LessMixinScopeProvider mixinScopeProvider;  
  
  @Inject
  private LessMixinLinkingService mixinLinkingService;  
      
  private Iterable<EObject> getStyleSheetStatements(StyleSheet styleSheet) {
    return styleSheet.eContents();
  }
  
  public static <X, Y extends X> Iterable<Y> removeElementFromIterable(Iterable<Y> iterable, final X element) {
    if (element == null) {
      return Iterables.filter(iterable, new Predicate<X>(){
        public boolean apply(X input) {
          return input != null;
        }});      
    } else {
      return Iterables.filter(iterable, new Predicate<X>(){
        public boolean apply(X input) {
          return !element.equals(input);
        }});
    }
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
  
  /** Compute the scope of a context.  If the given context is a Block or a StyleSheet, call
   * computeVariableScopeOfStatements in order to lookup on the variables defined in this scope.
   * Otherwise, call the function on the container.
   * Results for Block and StyleSheet are cached.
   */
  public IScope computeVariableScope(final EObject context, EReference ref) {
    EObject container = context.eContainer();
    if (container == null) {
      if (context instanceof StyleSheet) {
        ImportStatement importingStatement = importingStatementFinder.getImportingStatement(context.eResource());
        if (importingStatement != null) {
          EObject importingContainer = importingStatement.eContainer();
          if (importingContainer instanceof StyleSheet) {
            return computeVariableScopeOfStatements(importingContainer, getStyleSheetStatements((StyleSheet) importingContainer), importingStatement, ref);            
          } else if (importingContainer instanceof Block) {
            return computeVariableScopeOfStatements(importingContainer, ((Block) importingContainer).getContent().getStatement(), importingStatement, ref);
          }
        }
        return IScope.NULLSCOPE;
      }
    } else if (container instanceof Block) {
      return computeVariableScopeOfStatements(container, ((Block) container).getContent().getStatement(), null, ref);
    } else if (container instanceof StyleSheet) {
      return computeVariableScopeOfStatements(container, getStyleSheetStatements((StyleSheet) container), null, ref);
    } else if (container instanceof Mixin) {
      EStructuralFeature containingFeature = context.eContainingFeature();
      if (containingFeature.equals(LessPackage.eINSTANCE.getMixin_Guards()) ||
          containingFeature.equals(LessPackage.eINSTANCE.getMixin_Body()) ||
          containingFeature.equals(LessPackage.eINSTANCE.getMixin_Parameters())) {
        return computeVariableScopeOfMixinDefinition((Mixin) container, ref);
      }
    }
    return computeVariableScope(container, ref);
  }
    
  /** Compute the scope of a context, which contains the statements returned by iterable.
   */
  public IScope computeVariableScopeOfStatements(final EObject context, final Iterable<? extends EObject> statements, final EObject statementToIgnore, final EReference ref) {
    return cache.get(Tuples.create(LessScopeProvider.class, context, statementToIgnore), context.eResource(), new Provider<IScope>() {
      public IScope get() {
        List<IEObjectDescription> variableDefinitions = new ArrayList<IEObjectDescription>();
        // Go through the variables bound by the statements
        addVariableDefinitions(removeElementFromIterable(statements, statementToIgnore), variableDefinitions);
        return MapBasedScope.createScope(computeVariableScope(context, ref), variableDefinitions);
      }
    });
  }
  
  /**
   * Compute the scope of a mixin definition, binding the parameters of the definition.
   */
  public IScope computeVariableScopeOfMixinDefinition(final Mixin context, final EReference ref) {
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
        ResolvedImportStatement resolvedImportStatement = importStatementResolver.resolve((ImportStatement) statement);
        if (!resolvedImportStatement.hasError()) {
          // There is no cycle, and the imported stylesheet is not null.
          addVariableDefinitions(resolvedImportStatement.getImportedStyleSheet().getStatements(), variableDefinitions);
        }
      // fix BEGIN
      // MixinUtils.isCall((Mixin)statement) should be redundant, but we do it to be sure
      } else if (statement instanceof Mixin && MixinUtils.isCall((Mixin)statement)) {
    	  addVariablesDefinitionsOfCalledMixin((Mixin)statement, variableDefinitions);
      }
      // fix END
    }
  }

  /** Add the variables defined by a mixin.
   */
  private void addVariableDefinitions(Mixin mixinDefinition, List<IEObjectDescription> variableDefinitions) {
    for (MixinParameter parameter: mixinDefinition.getParameters().getParameter()) {
      AtVariableRefTarget variable = MixinUtils.getVariableBoundByMixinParameter(parameter);
      if (variable != null) variableDefinitions.add(getEObjectDescriptionFor(variable));
    }
    variableDefinitions.add(EObjectDescription.create(QualifiedName.create(ARGUMENTS_VARIABLE_NAME), mixinDefinition));
  }

	/**
	 * In newer LESS versrions (since 1.4) it's possible to use a mixin as a
	 * function call (see
	 * http://lesscss.org/features/#mixins-as-functions-feature): the variables
	 * defined inside the mixin body are accessible in a calling mixin. So we
	 * add these variables for a Mixin call.
	 * 
	 * @param mixinStatement
	 *            the statement which calls a Mixin
	 * @param variableDefinitions
	 *            the list of variable definitions which should be used to add
	 *            the direct variables of called Mixin
	 */
	private void addVariablesDefinitionsOfCalledMixin(Mixin mixinStatement, List<IEObjectDescription> variableDefinitions) {
		  MixinLink linkedMixin = mixinLinkingService.getLinkedMixin(mixinStatement);
		  if (linkedMixin.isSuccess() && linkedMixin.getElement().size() > 0) {
			  EObject object = linkedMixin.getElement().getObject(0);
			  if (object instanceof HashOrClassRef) {
				  HashOrClassRef hashOrClassRef = (HashOrClassRef) object;
				  Mixin referencedMixin = ModelUtils.goUpTo(hashOrClassRef, Mixin.class);
		    	  if (referencedMixin != null) {
		    		  if (referencedMixin.getBody() != null && referencedMixin.getBody().getContent() != null) {
		    			  // we don't call addVariableDefinitions() recursivley because variables of called
		    			  // mixins aren't visible recursivly in calling scope. The direct variables of
		    			  // called mixins are visible only.
						  for (EObject innerStatement : referencedMixin.getBody().getContent().getStatement()) {
							 if (innerStatement instanceof VariableDefinition) {
								 variableDefinitions.add(getEObjectDescriptionFor(((VariableDefinition) innerStatement).getLhs().getVariable()));
							 }
						  }
		       		  }
		    	  }
			  }
		  }
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
  
  /** Entry point for the calculation of the scope of a cross-reference to
   * a HashOrClass.
   */
  IScope scope_HashOrClassRefTarget(EObject context, EReference ref) {
    MixinContext mixinContext = new MixinContext(context);
    if (!mixinContext.isValid()) return IScope.NULLSCOPE;
    MixinScope scope = mixinScopeProvider.getScope(mixinContext.getMixin());
    return scope.getScope(mixinContext.getSelectorIndex());
  }
}