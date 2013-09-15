package net.vtst.ow.eclipse.less.linking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.vtst.ow.eclipse.less.LessMessages;
import net.vtst.ow.eclipse.less.less.HashOrClassRefTarget;
import net.vtst.ow.eclipse.less.less.LessPackage;
import net.vtst.ow.eclipse.less.less.LessUtils;
import net.vtst.ow.eclipse.less.less.MixinParameter;
import net.vtst.ow.eclipse.less.less.MixinParameters;
import net.vtst.ow.eclipse.less.less.MixinUtils;
import net.vtst.ow.eclipse.less.less.MixinUtils.Helper;
import net.vtst.ow.eclipse.less.less.MixinVarParameter;
import net.vtst.ow.eclipse.less.less.TerminatedMixin;
import net.vtst.ow.eclipse.less.scoping.LessMixinScopeProvider;
import net.vtst.ow.eclipse.less.scoping.MixinContext;
import net.vtst.ow.eclipse.less.scoping.MixinScope;
import net.vtst.ow.eclipse.less.scoping.MixinScopeElement;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.linking.ILinkingService;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.util.Tuples;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LessMixinLinkingService implements ILinkingService {

  // The cache contains:
  // - (LessMixinLinkingHelper.Prototype.class, HashOrClassRefTarget) -> Prototype
  // - (LessMixinLinkingHelper.class, Mixin) -> LinkingResult
  @Inject
  private IResourceScopeCache cache;
  
  @Inject
  private LessMixinScopeProvider mixinScopeProvider;

  // **************************************************************************
  // Mixin prototypes
  
  public static interface ICheckMixinError {
    public void report(LessMessages messages, ValidationMessageAcceptor acceptor);    
  }
  
  public class IllegalParameterLabel implements ICheckMixinError {
    private MixinParameter parameter;
    private String messageKey;
    
    public IllegalParameterLabel(MixinParameter parameter, String messageKey) {
      this.parameter = parameter;
      this.messageKey = messageKey;
    }

    public void report(LessMessages messages, ValidationMessageAcceptor acceptor) {
      acceptor.acceptWarning(messages.format(messageKey, parameter.getIdent().getIdent()), parameter, LessPackage.eINSTANCE.getMixinParameter_Ident(), 0, null);      
    }
  }

  public class MissingParameter implements ICheckMixinError {
    private MixinParameters parameters;
    private String parameterName;
    
    public MissingParameter(MixinParameters parameters, String parameterName) {
      this.parameters = parameters;
      this.parameterName = parameterName;
    }

    public void report(LessMessages messages, ValidationMessageAcceptor acceptor) {
      acceptor.acceptWarning(messages.format("missing_parameter_label", parameterName), parameters, null, 0, null);      
    }
  }

  public class IllegalNumberOfParameters implements ICheckMixinError {
    private MixinUtils.Helper helper;
    private int expectedMin;
    private int expectedMax;
    private int provided;
    
    public IllegalNumberOfParameters(Helper helper, int expectedMin, int expectedMax, int provided) {
      this.helper = helper;
      this.expectedMin = expectedMin;
      this.expectedMax = expectedMax;
      this.provided = provided;
    }

    private String getErrorMessageForCheckMixinCallParameters(LessMessages messages, int expectedMin, int expectedMax, int provided) {
      if (expectedMin == expectedMax)
        return String.format(messages.getString("illegal_number_of_parameters_for_mixin"),
            expectedMin, provided);
      if (expectedMax == Integer.MAX_VALUE)
        return String.format(messages.getString("illegal_number_of_parameters_for_mixin_min"),
            expectedMin, provided); 
      return String.format(messages.getString("illegal_number_of_parameters_for_mixin_range"),
          expectedMin, expectedMax, provided);
    }

    public void report(LessMessages messages, ValidationMessageAcceptor acceptor) {
      acceptor.acceptWarning(
          getErrorMessageForCheckMixinCallParameters(messages, this.expectedMin, this.expectedMax, provided),
          helper.getSelectors(), null, 0, null);
    }
  }
    
  public class Prototype {
    public int minNumberOfParameters = 0;
    public int maxNumberOfParameters = 0;
    public Map<String, Boolean> parameterNames = new HashMap<String, Boolean>();  // name -> required/optional
    
    private Prototype(TerminatedMixin mixinDefinition) {
      if (mixinDefinition == null) return;
      EList<MixinParameter> parameters = mixinDefinition.getParameters().getParameter();
      for (MixinParameter parameter: parameters) {
        ++maxNumberOfParameters;
        boolean required = !parameter.isHasDefaultValue();
        if (required) minNumberOfParameters = maxNumberOfParameters;
        String variable = MixinUtils.getVariableName(parameter);
        if (variable != null) parameterNames.put(variable, required);
      }
      MixinVarParameter varArg = mixinDefinition.getParameters().getVarArg();
      if (varArg != null) {
        maxNumberOfParameters = Integer.MAX_VALUE;
        if (varArg.getSep() != null)
          --minNumberOfParameters;
      }
    }
    
    private int getNumberOfParametersOfMixinCall(Helper helper) {
      MixinParameters parameters = helper.getParameters();
      if (parameters == null) return 0;
      int numberOfSemicolons = 0;
      for (String separator : parameters.getSep()) {
        if (";".equals(separator)) ++numberOfSemicolons;
      }
      if (numberOfSemicolons > 0 || parameters.getDummySep() != null) return numberOfSemicolons + 1;
      else return parameters.getParameter().size();
    }

    public List<ICheckMixinError> checkMixinCall(Helper helper) {
      List<ICheckMixinError> errors = new ArrayList<ICheckMixinError>();
      
      // Check the number of parameters
      int provided = getNumberOfParametersOfMixinCall(helper);
      if (provided < this.minNumberOfParameters || provided > this.maxNumberOfParameters) {
        errors.add(new IllegalNumberOfParameters(helper, this.minNumberOfParameters, this.maxNumberOfParameters, provided));
      }
      
      MixinParameters parameters = helper.getParameters();
      if (parameters != null) {
        // Get the set of named of parameters.
        // Check unicity of names.
        // Check definition of names.
        Set<String> providedNames = new HashSet<String>(parameters.getParameter().size());
        for (MixinParameter parameter : parameters.getParameter()) {
          if (parameter.getIdent() != null) {
            String parameterName = parameter.getIdent().getIdent();
            if (!providedNames.add(parameterName)) {
              errors.add(new IllegalParameterLabel(parameter, "duplicated_parameter_label"));
            }
            if (parameterNames.get(parameterName) == null) {
              errors.add(new IllegalParameterLabel(parameter, "illegal_parameter_label"));              
            }
          }
        }
        
        // Check that all required names are provided.
        for (Map.Entry<String, Boolean> entry : parameterNames.entrySet()) {
          if (entry.getValue() && !providedNames.contains(entry.getKey())) {
            errors.add(new MissingParameter(parameters, entry.getKey()));
          }
        }
      }
      return errors;
    }
  }
  
  public Prototype getPrototypeForMixinDefinition(final HashOrClassRefTarget hashOrClass) {
    return cache.get(Tuples.pair(Prototype.class, hashOrClass), hashOrClass.eResource(), new Provider<Prototype>() {
      public Prototype get() {
        EObject mixinDefinition = LessUtils.getNthAncestor(hashOrClass, 2);
        if ((mixinDefinition instanceof TerminatedMixin)) {
          return new Prototype((TerminatedMixin) mixinDefinition);
        } else {
          return new Prototype(null);
        }
      }
    });
  }
  
  // **************************************************************************
  // Linking
  
  public static class MixinLink {
    private MixinScopeElement element;
    private int matchLength;
    private boolean isSuccess;
    private List<ICheckMixinError> error;
    
    // No match
    public MixinLink() {
      this.isSuccess = false;
      this.matchLength = 0;
    }

    // Full match
    public MixinLink(MixinScopeElement element) {
      this.element = element;
      this.matchLength = this.element.size();
      this.isSuccess = true;
    }

    // Full match with wrong prototype
    public MixinLink(MixinScopeElement element, List<ICheckMixinError> error) {
      this.element = element;
      this.matchLength = this.element.size();
      this.isSuccess = false;
      this.error = error;
    }

    // Partial match
    public MixinLink(MixinScopeElement element, int matchLength) {
      this.element = element;
      this.isSuccess = false;
      this.matchLength = matchLength;
    }
    
    public int matchLength() { return this.matchLength; }
    public boolean isSuccess() { return this.isSuccess; }
    public MixinScopeElement getElement() { return this.element; }
    public List<ICheckMixinError> getError() {
      return this.error;
    }
  }
  
  // This code could be optimized by generating the actual error objects only for the last call.
  // This would require two implementations of Prototype.checkMixinCall.
  // TODO: Check there is no place we assume that the target of a mixin call is a mixin declaration.
  // It could also be a simple ruleset.
  private MixinLink getBestFullMatch(MixinUtils.Helper mixinHelper, MixinScope mixinScope) {
    Iterable<MixinScopeElement> fullMatches = mixinScope.getFullMatches();
    MixinScopeElement bestMatch = null;
    MixinScopeElement lastMatch = null;
    List<ICheckMixinError> lastErrors = null;
    int numberOfMatches = 0;
    for (MixinScopeElement fullMatch : fullMatches) {
      EObject eObject = fullMatch.getLastObject();
      if (eObject instanceof HashOrClassRefTarget) {
        ++numberOfMatches;
        LessMixinLinkingService.Prototype prototype = 
            getPrototypeForMixinDefinition((HashOrClassRefTarget) eObject);
        lastErrors = prototype.checkMixinCall(mixinHelper);
        lastMatch = fullMatch;
        if (lastErrors.isEmpty()) bestMatch = fullMatch;
      }
    }
    if (bestMatch != null) return new MixinLink(bestMatch);
    else if (lastMatch != null) return new MixinLink(lastMatch, numberOfMatches == 1 ? lastErrors : null);
    else return null;
  }
  
  private MixinLink getLongestMatch(MixinUtils.Helper mixinHelper, MixinScope mixinScope) {
    int lastMatchingPosition = mixinScope.getLastMatchingPosition();
    if (lastMatchingPosition < 0) return new MixinLink();
    MixinScopeElement element = mixinScope.getLastElement(lastMatchingPosition);
    if (element == null) return new MixinLink();
    return new MixinLink(element, lastMatchingPosition + 1);
  }

  public MixinLink getLinkedMixin(final MixinUtils.Helper mixinHelper) {
    return cache.get(Tuples.pair(Prototype.class, mixinHelper.getMixin()), mixinHelper.getMixin().eResource(), new Provider<MixinLink>() {
      public MixinLink get() {
        MixinScope mixinScope = mixinScopeProvider.getScope(mixinHelper);
        MixinLink fullMatch = getBestFullMatch(mixinHelper, mixinScope);
        if (fullMatch != null) return fullMatch;
        return getLongestMatch(mixinHelper, mixinScope);
      }
    });    
  }
  
  public List<EObject> getLinkedObjects(EObject context, EReference ref, INode node) {
    MixinContext mixinContext = new MixinContext(context);
    if (!mixinContext.isValid()) return Collections.emptyList();
    MixinLink linkingResult = getLinkedMixin(mixinContext.getMixinHelper());
    if (linkingResult.matchLength() > mixinContext.getSelectorIndex()) {
      return Collections.singletonList(linkingResult.getElement().getObject(mixinContext.getSelectorIndex()));
    } else {
      return Collections.emptyList();
    }
  }

}
