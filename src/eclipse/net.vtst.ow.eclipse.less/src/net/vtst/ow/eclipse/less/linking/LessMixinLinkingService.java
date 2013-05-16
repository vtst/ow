package net.vtst.ow.eclipse.less.linking;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.vtst.ow.eclipse.less.less.HashOrClassRefTarget;
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
  
  public static interface CheckMixinCallCallback {
    public void illegalNumberOfParameters(int provided);

    public void illegalParameterLabel(MixinParameter parameter);
  }
  
  public static class Prototype {
    public int minNumberOfParameters = 0;
    public int maxNumberOfParameters = 0;
    public Set<String> parameterNames = new HashSet<String>();
    
    private Prototype(TerminatedMixin mixinDefinition) {
      if (mixinDefinition == null) return;
      EList<MixinParameter> parameters = mixinDefinition.getParameters().getParameter();
      for (MixinParameter parameter: parameters) {
        ++maxNumberOfParameters;
        if (!parameter.isHasDefaultValue()) minNumberOfParameters = maxNumberOfParameters;
        String variable = MixinUtils.getVariableName(parameter);
        if (variable != null) parameterNames.add(variable);
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
      if (numberOfSemicolons > 0) return numberOfSemicolons + 1;
      else return parameters.getParameter().size();
    }

    public boolean checkMixinCall(Helper helper, CheckMixinCallCallback callback) {
      boolean result = true;
      int provided = getNumberOfParametersOfMixinCall(helper);
      if (provided < this.minNumberOfParameters || provided > this.maxNumberOfParameters) {
        if (callback == null) return false;
        callback.illegalNumberOfParameters(provided);
        result = false;
      }
      MixinParameters parameters = helper.getParameters();
      if (parameters != null) {
        for (MixinParameter parameter : parameters.getParameter()) {
          if (parameter.getIdent() != null && !this.parameterNames.contains(parameter.getIdent().getIdent())) {
            if (callback == null) return false;
            callback.illegalParameterLabel(parameter);
            result = false;
          }
        }
      }
      return result;
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
    private int numberOfMatches;
    private boolean isSuccess;
    
    public MixinLink() {
      this(null, false, 0);
    }

    public MixinLink(MixinScopeElement element, boolean isSuccess, int numberOfMatches) {
      this.element = element;
      this.matchLength = this.element.size();
      this.isSuccess = isSuccess;
      this.numberOfMatches = numberOfMatches;
    }
    
    public MixinLink(MixinScopeElement element, int matchLength) {
      this(element, false, 0);
      this.matchLength = matchLength;
    }
    
    public int matchLength() { return this.matchLength; }
    public int numberOfMatches() { return this.numberOfMatches; }
    public boolean isSuccess() { return this.isSuccess; }
    public MixinScopeElement getElement() { return this.element; }
  }
  
  // TODO: Check there is no place we assume that the target of a mixin call is a mixin declaration.
  // It could also be a simple ruleset.
  private MixinLink getBestFullMatch(MixinUtils.Helper mixinHelper, MixinScope mixinScope) {
    Iterable<MixinScopeElement> fullMatches = mixinScope.getFullMatches();
    MixinScopeElement bestMatch = null;
    MixinScopeElement lastMatch = null;
    int count = 0;
    for (MixinScopeElement fullMatch : fullMatches) {
      ++count;
      EObject eObject = fullMatch.getLastObject();
      if (eObject instanceof HashOrClassRefTarget) {
        LessMixinLinkingService.Prototype prototype = 
            getPrototypeForMixinDefinition((HashOrClassRefTarget) eObject);
        if (prototype.checkMixinCall(mixinHelper, null))
          bestMatch = fullMatch;
        lastMatch = fullMatch;
      }
    }
    if (bestMatch != null) return new MixinLink(bestMatch, true, count);
    else if (lastMatch != null) return new MixinLink(lastMatch, false, count);
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
