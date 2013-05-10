package net.vtst.ow.eclipse.less.linking;

import java.util.HashSet;
import java.util.Set;

import net.vtst.ow.eclipse.less.less.HashOrClassRefTarget;
import net.vtst.ow.eclipse.less.less.LessUtils;
import net.vtst.ow.eclipse.less.less.MixinParameter;
import net.vtst.ow.eclipse.less.less.MixinParameters;
import net.vtst.ow.eclipse.less.less.MixinUtils;
import net.vtst.ow.eclipse.less.less.MixinVarParameter;
import net.vtst.ow.eclipse.less.less.TerminatedMixin;
import net.vtst.ow.eclipse.less.less.MixinUtils.Helper;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.util.Tuples;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LessMixinLinkingHelper {

  // The cache contains pairs (LessMixinLinkingHelper.Prototype.class, HashOrClassRefTarget).
  @Inject
  private IResourceScopeCache cache;

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
        EObject mixinCall = LessUtils.getNthAncestor(hashOrClass, 2);
        if ((mixinCall instanceof TerminatedMixin)) {
          return new Prototype((TerminatedMixin) mixinCall);
        } else {
          return new Prototype(null);
        }
      }
    });
  }

}
