package net.vtst.ow.eclipse.less.less;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

/**
 * Helper class for manipulating Mixins.
 */
public class MixinUtils {
  
  // **************************************************************************
  // Helper class for accessing Mixin objects
  
  public static abstract class Helper {
    public abstract MixinSelectors getSelectors();
    public abstract MixinParameters getParameters();
    public abstract MixinDefinitionGuards getGuard();
    public abstract Block getBody();
    
    public boolean isDefinition() {
      return this.getBody() != null;
    }
    
    public boolean isCall() {
      return this.getBody() == null;
    }
  }
  
  public static class HelperForTerminatedMixin extends Helper {
    private TerminatedMixin mixin;

    public HelperForTerminatedMixin(TerminatedMixin mixin) {
      this.mixin = mixin;
    }
    
    public MixinSelectors getSelectors() { return mixin.getSelectors(); }
    public MixinParameters getParameters() { return mixin.getParameters(); }
    public MixinDefinitionGuards getGuard() { return mixin.getGuards(); }
    public Block getBody() { return mixin.getBody(); }
  }
  
  public static class HelperForUnterminatedMixin extends Helper {
    private UnterminatedMixin mixin;

    public HelperForUnterminatedMixin(UnterminatedMixin mixin) {
      this.mixin = mixin;
    }
    
    public MixinSelectors getSelectors() { return mixin.getSelectors(); }
    public MixinParameters getParameters() { return null; }
    public MixinDefinitionGuards getGuard() { return null; }
    public Block getBody() { return null; }
  }


  public static Helper newHelper(TerminatedMixin mixin) { return new HelperForTerminatedMixin(mixin); }
  public static Helper newHelper(UnterminatedMixin mixin) { return new HelperForUnterminatedMixin(mixin); }
  public static Helper newHelper(Mixin mixin) {
    if (mixin instanceof UnterminatedMixin) return newHelper((UnterminatedMixin) mixin);
    if (mixin instanceof TerminatedMixin) return newHelper((TerminatedMixin) mixin);
    throw new RuntimeException("Unknown sub-class of Mixin");
  }


  // **************************************************************************
  // Other utility functions
  
  /**
   * @param term
   * @return true if term is a single variable.
   */
  public static boolean isVariableRef(Term term) {
    if (term instanceof ExtendedTerm) {
      ExtendedTerm extendedTerm = (ExtendedTerm) term;
      EList<EObject> subTerms = extendedTerm.getTerm();
      if (subTerms.size() != 1) return false;
      EObject subTerm = subTerms.get(0);
      if (subTerm instanceof AtVariableRef) return true;
      else if (subTerm instanceof Term) return isVariableRef((Term) subTerm);
      else return false;
    }
    return false;
  }
  
  /**
   * @param term
   * @return the single variable contained in term, or null.
   */
  public static AtVariableRef getVariableRef(Term term) {
    if (term instanceof ExtendedTerm) {
      ExtendedTerm extendedTerm = (ExtendedTerm) term;
      EList<EObject> subTerms = extendedTerm.getTerm();
      if (subTerms.size() != 1) return null;
      EObject subTerm = subTerms.get(0);
      if (subTerm instanceof AtVariableRef) return (AtVariableRef) subTerm;
      else if (subTerm instanceof Term) return getVariableRef((Term) subTerm);
      else return null;
    }
    return null;
  }
  
  public static AtVariableRefTarget getVariableBoundByMixinParameter(MixinParameter parameter) {
    if (parameter.isHasDefaultValue()) {
      return parameter.getIdent();
    } else if (parameter.getTerm().size() > 0) {
        return getVariableRef(parameter.getTerm().get(0));
    } else {
      return null;
    }
  }
 
  public static String getVariableName(MixinParameter parameter) {
    if (parameter.isHasDefaultValue()) {
      AtVariableDef variable = parameter.getIdent();
      if (variable != null) return variable.getIdent();
      else return null;
    } else if (parameter.getTerm().size() > 0) {
      AtVariableRef variable = MixinUtils.getVariableRef(parameter.getTerm().get(0));
      if (variable != null) return MixinUtils.getIdent(variable);
      else return null;
    } else {
      return null;
    }
  }

  public static EObject getFirstNonTermAncestor(EObject obj) {
    EObject result = obj.eContainer();
    while (result instanceof Term) result = result.eContainer();
    return result;
  }
  
  /**
   * @param obj
   * @return true if obj is a variable reference which is in fact the name of a mixin parameter in a
   *   mixin definition.
   */
  public static boolean isBoundByMixinDefinitionParameter(EObject obj) {
    EObject container = MixinUtils.getFirstNonTermAncestor(obj);
    if (container instanceof MixinParameter) {
      MixinParameter parameter = (MixinParameter) container;
      if (!parameter.isHasDefaultValue()) {
        EObject mixin = container.eContainer().eContainer();
        if (mixin instanceof Mixin) {
          MixinUtils.Helper helper = MixinUtils.newHelper((Mixin) mixin);
          if (helper.isDefinition()) {
            return true;
          }
        }
      }
    }
    return false;
  }
  
  public static boolean isBoundByMixinDefinitionSelector(EObject obj) {
    EObject container = obj.eContainer();
    if (container instanceof MixinSelectors) {
      EObject mixin = container.eContainer();
      if (mixin instanceof Mixin) {
        MixinUtils.Helper helper = MixinUtils.newHelper((Mixin) mixin);
        if (helper.isDefinition()) return true;
      }
    }
    return false;
  }
  
  private static String getIdentText(EObject obj) {
    INode node = NodeModelUtils.getNode(obj);
    for (ILeafNode leafNode : node.getLeafNodes()) {
      if (!leafNode.isHidden()) return leafNode.getText();
    }
    return "";
  }
  
  public static String getIdent(AtVariableRefTarget obj) {
    if (obj instanceof AtVariableDef) return ((AtVariableDef) obj).getIdent();
    if (obj instanceof AtVariableRef) return getIdentText(obj);
    throw new RuntimeException("Should not be called with a Mixin");
  }

  // Keep in sync with the above version
  public static String getIdent(AtVariableRef obj) {
    return getIdentText(obj);
  }
  
  public static String getIdent(HashOrClassRefTarget obj) {
    if (obj instanceof HashOrClass) return ((HashOrClass) obj).getIdent();
    if (obj instanceof HashOrClassRef) return getIdentText(obj);
    throw new RuntimeException("Unknown subclass of HashOrClassRefTarget");
  }

  // Keep in sync with the above version
  public static String getIdent(HashOrClassRef obj) {
    return getIdentText(obj);
  }

}
