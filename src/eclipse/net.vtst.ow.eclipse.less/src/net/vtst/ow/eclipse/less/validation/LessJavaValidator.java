// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.validation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.vtst.eclipse.easyxtext.validation.config.ConfigurableCheck;
import net.vtst.eclipse.easyxtext.validation.config.ConfigurableDeclarativeValidator;
import net.vtst.ow.eclipse.less.LessMessages;
import net.vtst.ow.eclipse.less.less.AtVariableRef;
import net.vtst.ow.eclipse.less.less.Block;
import net.vtst.ow.eclipse.less.less.BlockContents;
import net.vtst.ow.eclipse.less.less.BlockUtils;
import net.vtst.ow.eclipse.less.less.Declaration;
import net.vtst.ow.eclipse.less.less.HashOrClass;
import net.vtst.ow.eclipse.less.less.HashOrClassCrossReference;
import net.vtst.ow.eclipse.less.less.IdentTerm;
import net.vtst.ow.eclipse.less.less.ImportStatement;
import net.vtst.ow.eclipse.less.less.IncompleteToplevelStatement;
import net.vtst.ow.eclipse.less.less.LessPackage;
import net.vtst.ow.eclipse.less.less.Mixin;
import net.vtst.ow.eclipse.less.less.MixinCall;
import net.vtst.ow.eclipse.less.less.MixinDefinition;
import net.vtst.ow.eclipse.less.less.MixinDefinitionGuards;
import net.vtst.ow.eclipse.less.less.MixinDefinitionParameter;
import net.vtst.ow.eclipse.less.less.MixinDefinitionVariable;
import net.vtst.ow.eclipse.less.less.MixinParameter;
import net.vtst.ow.eclipse.less.less.MixinParameters;
import net.vtst.ow.eclipse.less.less.MixinSelectors;
import net.vtst.ow.eclipse.less.less.MixinUtils;
import net.vtst.ow.eclipse.less.less.MixinVarParameter;
import net.vtst.ow.eclipse.less.less.NumberWithUnitTerm;
import net.vtst.ow.eclipse.less.less.NumericLiteral;
import net.vtst.ow.eclipse.less.less.PseudoClassNthSpecialCase;
import net.vtst.ow.eclipse.less.less.StringTerm;
import net.vtst.ow.eclipse.less.less.StyleSheet;
import net.vtst.ow.eclipse.less.less.Term;
import net.vtst.ow.eclipse.less.less.TerminatedMixin;
import net.vtst.ow.eclipse.less.less.VariableDefinition;
import net.vtst.ow.eclipse.less.scoping.LessImportStatementResolver;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.util.Pair;
import org.eclipse.xtext.util.Tuples;
import org.eclipse.xtext.validation.Check;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

public class LessJavaValidator extends AbstractLessJavaValidator {
  
  @Inject
  LessMessages messages;
  
  @Inject
  LessImportStatementResolver importStatementResolver;
    
  protected boolean isResponsible(Map<Object, Object> context, EObject eObject) {
    ConfigurableDeclarativeValidator.makeConfigurable(this);
    return super.isResponsible(context, eObject);
  }
  
  // Check import statements
  @Check
  public void checkImportStatement(ImportStatement importStatement) {
    switch (importStatementResolver.checkImportStatement(importStatement)) {
    case INVALID_URI:
      warning(messages.getString("import_invalid_uri"), importStatement, LessPackage.eINSTANCE.getImportStatement_Uri(), 0);
      break;
    case LOOP:
      warning(messages.getString("import_loop"), importStatement, LessPackage.eINSTANCE.getImportStatement_Uri(), 0);
      break;
    default:
      break;
    }
  }
  
  // Check for multiple properties in block
  @Check
  public void checkBlockUniqueProperties(Block block) {
    Set<String> propertyNames = new HashSet<String>();
    for(EObject item: BlockUtils.iterator(block)) {
      if (!(item instanceof Declaration)) continue;
      Declaration declaration = (Declaration) item;
      String propertyName = declaration.getProperty();
      if (!propertyNames.add(propertyName)) {
        String message = String.format(messages.getString("duplicated_property"), propertyName);
        warning(message, declaration, LessPackage.eINSTANCE.getDeclaration_Property(), 0);
      }
    }
  }
  
  // Check for star property hack
  @Check
  public void checkStarPropertyHack(Declaration declaration) {
    if (declaration.isStar_property_hack()) {
      warning(
          messages.getString("star_property_hack"), declaration, 
          LessPackage.eINSTANCE.getDeclaration_Star_property_hack(), 0);
    }
  }
  
  // Check for multiple variables in block
  @Check
  @ConfigurableCheck(group = "checkUniqueVariables")
  public void checkBlockUniqueVariables(Block block) {
    checkUniqueVariables(BlockUtils.iterator(block));
  }
  
  // Check for multiple variables in stylesheet
  @Check
  @ConfigurableCheck(group = "checkUniqueVariables")
  public void checkStyleSheetUniqueVariables(StyleSheet styleSheet) {
    checkUniqueVariables(styleSheet.eContents());
  }
  
  public void checkUniqueVariables(Iterable<EObject> iterable) {
    Set<String> names = new HashSet<String>();
    for (EObject obj: iterable) {
      if (obj instanceof VariableDefinition) {
        VariableDefinition variableDefinition = (VariableDefinition) obj;
        if (!names.add(variableDefinition.getVariable().getIdent())) {
          String message = String.format(messages.getString("duplicated_variable"),
              variableDefinition.getVariable().getIdent());
          warning(message, variableDefinition, LessPackage.eINSTANCE.getVariableDefinition_Variable(), 0);
        }
      }
    }
  }
  
  // Check for multiple variables in mixin definitions
  @Check
  public void checkMixinDefinitionUniqueVariables(MixinDefinition mixinDefinition) {
    Set<String> names = new HashSet<String>();
    for (MixinDefinitionParameter parameter: mixinDefinition.getParameter()) {
      if (parameter instanceof MixinDefinitionVariable) {
        String ident = ((MixinDefinitionVariable) parameter).getVariable().getIdent();
        if (!names.add(ident)) {
          String message = String.format(messages.getString("duplicated_variable_mixin"), ident);
          warning(message, parameter, LessPackage.eINSTANCE.getMixinDefinitionVariable_Variable(), 0);
        }
      }
    }
  }
  
  private boolean isMixinDefinitionVarArgs(MixinDefinition mixinDefinition) {
    return (mixinDefinition.isVarArgsAnonymous() || mixinDefinition.isVarArgsLastVar());
  }
  
  // Check that optional parameters are at the end in mixin definitions
  @Check
  @ConfigurableCheck(configurable = false)
  public void checkMixinDefinitionParameters(MixinDefinition mixinDefinition) {
    boolean hasOptional = false;
    boolean lastOptional = false;
    int index = 0;
    int firstOptionalIndex = 0;
    for (MixinDefinitionParameter parameter: mixinDefinition.getParameter()) {
      lastOptional = parameter instanceof MixinDefinitionVariable &&
          ((MixinDefinitionVariable) parameter).getDefault_value().size() > 0;
      if (lastOptional) {
        if (!hasOptional) {
          hasOptional = true;
          firstOptionalIndex = index;
        }
      } else {
        if (hasOptional) {
          String message = messages.getString("illegal_optional_parameter");
          warning(message, mixinDefinition, LessPackage.eINSTANCE.getMixinDefinition_Parameter(), firstOptionalIndex);
        }
      }
      ++index;
    }
    if (lastOptional && mixinDefinition.isVarArgsLastVar()) {
      String message = messages.getString("illegal_optional_parameter_var_args");
      error(message, mixinDefinition, LessPackage.eINSTANCE.getMixinDefinition_Parameter(), index - 1);      
    }
  }
  
  // Check the number of parameters of mixin calls
  @Check
  public void checkMixinCallParameters(MixinCall mixinCall) {
    // Get the last selector, if any
    EList<HashOrClassCrossReference> selectors = mixinCall.getSelector();
    if (selectors.size() == 0) return;
    HashOrClassCrossReference hashOrClassCrossReference = selectors.get(selectors.size() - 1);
    // Get the reference, if any   
    EList<EObject> crossReferences = hashOrClassCrossReference.eCrossReferences();
    if (crossReferences.size() == 0) return;
    EObject crossReference = crossReferences.get(0);
    if (!(crossReference instanceof HashOrClass)) return;
    HashOrClass hashOrClass = (HashOrClass) crossReference;
    // We do not check the arguments if the called mixin is undefined, in order to avoid multiple error messages.
    if (hashOrClass.getIdent() != null) {
      Pair<Integer, Integer> expected = getNumberOfParametersForMixin(hashOrClass);
      int provided = mixinCall.getParameter().size();
      if (provided < expected.getFirst() || provided > expected.getSecond()) {
        warning(
            getErrorMessageForCheckMixinCallParameters(hashOrClass.getIdent(), expected.getFirst(), expected.getSecond(), provided),
            mixinCall, LessPackage.eINSTANCE.getMixinCall_Selector(), 0);
      }
    }
  }
  
  private String getErrorMessageForCheckMixinCallParameters(String ident, int expectedMin, int expectedMax, int provided) {
    if (expectedMin == expectedMax)
        return String.format(messages.getString("illegal_number_of_parameters_for_mixin"),
            ident, expectedMin, provided);
    if (expectedMax == Integer.MAX_VALUE)
      return String.format(messages.getString("illegal_number_of_parameters_for_mixin_min"),
          ident, expectedMin, provided); 
    return String.format(messages.getString("illegal_number_of_parameters_for_mixin_range"),
        ident, expectedMin, expectedMax, provided);
  }
  
  /** Compute the expected number of parameters for a mixin call.
   * @param hashOrClass
   * @return a pair (min, max)
   */
  private Pair<Integer, Integer> getNumberOfParametersForMixin(HashOrClass hashOrClass) {
    EObject container = hashOrClass.eContainer();
    int min = 0;
    int max = 0;
    if ((container instanceof MixinDefinition)) {
      MixinDefinition mixinDefinition = (MixinDefinition) container;
      EList<MixinDefinitionParameter> parameters = mixinDefinition.getParameter();
      for (MixinDefinitionParameter parameter: parameters) {
        ++max;
        if (!(parameter instanceof MixinDefinitionVariable &&
            ((MixinDefinitionVariable) parameter).getDefault_value().size() > 0)) {
          min = max;
        }
      }
      if (isMixinDefinitionVarArgs(mixinDefinition)) 
        max = Integer.MAX_VALUE;
    }
    return Tuples.pair(min, max);
  }
  
  @Check
  public void checkFinalSemicolonOfTerminatedMixin(TerminatedMixin mixin) {
    if (mixin.getBody() != null || mixin.isHasFinalSemicolon()) return;
    EObject parent = mixin.eContainer();
    if (parent instanceof BlockContents) {
      if (((BlockContents) parent).getNext() != null) {
        error(messages.getString("missing_semicolon_after_mixin_call"), mixin, LessPackage.eINSTANCE.getTerminatedMixin_Parameters(), 0);
      }
    } else if (parent instanceof StyleSheet) {
      StyleSheet styleSheet = (StyleSheet) parent;
      if (!mixin.equals(styleSheet.getStatements().get(styleSheet.getStatements().size() - 1))) {
        error(messages.getString("missing_semicolon_after_mixin_call"), mixin, LessPackage.eINSTANCE.getTerminatedMixin_Parameters(), 0);        
      }
    } else {
      throw new RuntimeException("Unknown class for a parent of a TerminatedMixin");
    }
  }
  
  @Check
  @ConfigurableCheck(configurable = false)
  public void checkMixin(Mixin mixin) {
    MixinUtils.Helper helper = MixinUtils.newHelper(mixin);
    if (helper.isDefinition()) checkMixinDefinition(helper);
    if (helper.isCall()) checkMixinCall(helper);
  }
  
  private void checkMixinDefinition(MixinUtils.Helper helper) {
    MixinParameters parameters = helper.getParameters();
    if (parameters != null) checkMixinDefinitionParameters(parameters);
    MixinSelectors selectors = helper.getSelectors();
    if (selectors.getSelector().size() != 0) {
      error(messages.getString("unexpected_selector"), selectors, LessPackage.eINSTANCE.getMixinSelectors_Selector(), 1);      
    }
  }
  
  private void checkMixinDefinitionParameters(MixinParameters parameters) {
    EList<MixinParameter> parameterList = parameters.getParameter();
    // Check that separators are the same
    EList<String> separators = parameters.getSep();
    if (separators.size() > 1) {
      String firstSeparator = separators.get(0);
      for (int i = 1; i < separators.size(); ++i) {
        if (!separators.get(i).equals(firstSeparator))
          error(messages.getString("unexpected_separator"), parameters, LessPackage.eINSTANCE.getMixinParameters_Sep(), i);
      }
    }
    
    // Check that only one variable term is provided when there is no default value
    for (MixinParameter parameter : parameterList) {
      if (!parameter.isHasDefaultValue()) {
        if (parameter.getTerm().size() > 1) {
          error(messages.getString("unexpected_term"), parameter, LessPackage.eINSTANCE.getMixinParameter_Term(), 1);
        } else {
          Term term = parameter.getTerm().get(0);
          if (!(term instanceof IdentTerm || term instanceof StringTerm || 
              term instanceof NumericLiteral || MixinUtils.isVariableRef(term))) {
            error(messages.getString("unexpected_term"), parameter, LessPackage.eINSTANCE.getMixinParameter_Term(), 0);              
          }
        }
      }
    }
    
    // Check the var parameter:
    // - Has the same separator as the rest, if any,
    // - Has no default value
    MixinVarParameter varParameter = parameters.getVarArg();
    if (varParameter != null) {
      if (varParameter.getSep() != null) {
        if (parameters.getParameter().size() == 0 ||
            separators.size() > 0 && !separators.get(0).equals(varParameter.getSep())) {
          error(messages.getString("unexpected_separator"), varParameter, LessPackage.eINSTANCE.getMixinVarParameter_Sep(), 0);
        }
      }
      if (varParameter.getSep() == null && parameterList.size() > 0 && parameterList.get(parameterList.size() - 1).isHasDefaultValue()) {
        error(messages.getString(
            "illegal_optional_parameter_var_args"), parameterList.get(parameterList.size() - 1),
            LessPackage.eINSTANCE.getMixinParameter_Term(), 0);
      }
    }
  }
  
  private void checkMixinCall(MixinUtils.Helper helper) {
    MixinParameters parameters = helper.getParameters();
    if (parameters != null) checkMixinCallParameters(parameters);
  }
  
  private void checkMixinCallParameters(MixinParameters parameters) {
    if (parameters.getVarArg() != null) {
      error(messages.getString("unexpected_token"), parameters.getVarArg(), null, 0);
    }
  }
  
  
  
  // Report error for IncompleteToplevelStatement
  @Check
  @ConfigurableCheck(configurable = false)
  public void checkIncompleteToplevelStatement(IncompleteToplevelStatement statement) {
    error(messages.getString("incomplete_toplevel_statement"), statement, null, 0);
  }
  
  @Check
  @ConfigurableCheck(configurable = false)
  public void checkPseudoClassNthSpecialCase(PseudoClassNthSpecialCase pseudo) {
    if (!doCheckPseudoClassNthSpecialCase(pseudo.getSpecial())) {
      error(messages.getString("illegal_pseudo_class_argument"), pseudo, null, 0);
    }
  }
  
  public boolean doCheckPseudoClassNthSpecialCase(String special) {
    int i = 0;
    int n = special.length();
    char c = special.charAt(i);
    if (c == '+' || c == '-') {
      ++i; if (i == n) return false; else c = special.charAt(i);
    }
    while (c >= '0' && c <= '9') {
      ++i; if (i == n) return false; else c = special.charAt(i);
    }
    if (c != 'n') return false;
    ++i; if (i == n) return false; else c = special.charAt(i);
    while (c == ' ') {
      ++i; if (i == n) return false; else c = special.charAt(i);      
    }
    if (c != '+' && c != '-') return false;
    ++i; if (i == n) return false; else c = special.charAt(i);
    while (c == ' ') {
      ++i; if (i == n) return false; else c = special.charAt(i);      
    }
    if (c < '0' && c > '9') return false;
    while (c >= '0' && c <= '9' && i < n) {
      ++i; if (i == n) return true; else c = special.charAt(i);
    }
    return false;
  }
  
  // Check that units are among the ones defined by the CSS standard.
  // http://www.w3.org/TR/css3-values/
  private static Set<String> CSS_UNITS = Sets.newHashSet(
      // Font-relative lengths
      "em", "ex", "ch", "rem",
      // Viewport-percentage lengths
      "vw", "vh", "vmin", "vmax",
      // Absolute lengths
      "cm", "mm", "in", "pt", "pc", "px",
      // Angles
      "deg", "grad", "rad", "turn",
      // Times
      "s", "ms",
      // Frequencies
      "hz", "khz",
      // Resolutions
      "dpi", "dpcm", "dppx");
  
  @Check
  @ConfigurableCheck(configurable = false)
  public void checkNumberWithUnitTerm(NumberWithUnitTerm term) {
    String unit = term.getUnit();
    if (unit != null && !CSS_UNITS.contains(unit.toLowerCase())) {
      warning(messages.getString("unknown_css_unit"), term, LessPackage.eINSTANCE.getNumberWithUnitTerm_Unit(), 0);
    }
  }

}
