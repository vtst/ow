// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.validation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import net.vtst.eclipse.easyxtext.validation.config.AdditionalBooleanOption;
import net.vtst.eclipse.easyxtext.validation.config.ConfigurableCheck;
import net.vtst.eclipse.easyxtext.validation.config.ConfigurableDeclarativeValidator;
import net.vtst.ow.eclipse.less.LessMessages;
import net.vtst.ow.eclipse.less.less.Block;
import net.vtst.ow.eclipse.less.less.BlockContents;
import net.vtst.ow.eclipse.less.less.BlockUtils;
import net.vtst.ow.eclipse.less.less.Declaration;
import net.vtst.ow.eclipse.less.less.IdentTerm;
import net.vtst.ow.eclipse.less.less.ImportStatement;
import net.vtst.ow.eclipse.less.less.IncompleteToplevelStatement;
import net.vtst.ow.eclipse.less.less.InnerRuleSet;
import net.vtst.ow.eclipse.less.less.InnerSelector;
import net.vtst.ow.eclipse.less.less.KeyframesContents;
import net.vtst.ow.eclipse.less.less.KeyframesStatement;
import net.vtst.ow.eclipse.less.less.LessPackage;
import net.vtst.ow.eclipse.less.less.Mixin;
import net.vtst.ow.eclipse.less.less.MixinDefinitionGuards;
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
import net.vtst.ow.eclipse.less.less.ToplevelRuleSet;
import net.vtst.ow.eclipse.less.less.ToplevelSelector;
import net.vtst.ow.eclipse.less.less.VariableDefinition;
import net.vtst.ow.eclipse.less.linking.LessMixinLinkingService;
import net.vtst.ow.eclipse.less.linking.LessMixinLinkingService.ICheckMixinError;
import net.vtst.ow.eclipse.less.linking.LessMixinLinkingService.MixinLink;
import net.vtst.ow.eclipse.less.scoping.LessImportStatementResolver;
import net.vtst.ow.eclipse.less.scoping.LessImportStatementResolver2;
import net.vtst.ow.eclipse.less.scoping.LessImportStatementResolver2.ResolvedImportStatement;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.Check;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

public class LessJavaValidator extends AbstractLessJavaValidator {
  
  @Inject
  private LessMessages messages;
  
  @Inject
  private LessImportStatementResolver importStatementResolver;

  @Inject
  private LessImportStatementResolver2 importStatementResolver2;

  @Inject
  private LessMixinLinkingService mixinLinkingService;
  
  public AdditionalBooleanOption checkMixinLinking;
  public AdditionalBooleanOption checkVariableLinking;
  public AdditionalBooleanOption checkMixinCallParameters;
    
  protected boolean isResponsible(Map<Object, Object> context, EObject eObject) {
    ConfigurableDeclarativeValidator.makeConfigurable(this);
    return super.isResponsible(context, eObject);
  }
  
  // Check import statements
  @Check
  public void checkImportStatement(ImportStatement importStatement) {
    ResolvedImportStatement resolvedImportStatement = importStatementResolver2.resolve(importStatement);
    if (resolvedImportStatement.hasError()) {
      // TODO: Put the error at the right place.
      error(resolvedImportStatement.getError(), importStatement, null, 0);
    } else if (resolvedImportStatement.isCycleRoot()) {
      warning(messages.getString("import_loop"), importStatement, LessPackage.eINSTANCE.getImportStatement_Uri(), 0);      
    }
    // TODO: Delete messages.
//    switch (importStatementResolver.checkImportStatement(importStatement)) {
//    case INVALID_FORMAT:
//      error(messages.getString("invalid_import_format"), importStatement, LessPackage.eINSTANCE.getImportStatement_Format(), 0);
//      break;
//    case INVALID_URI:
//      warning(messages.getString("import_invalid_uri"), importStatement, LessPackage.eINSTANCE.getImportStatement_Uri(), 0);
//      break;
//    case LOOP:
//      warning(messages.getString("import_loop"), importStatement, LessPackage.eINSTANCE.getImportStatement_Uri(), 0);
//      break;
//    default:
//      break;
//    }
  }
  
  // Check for multiple properties in block
  @Check
  public void checkBlockUniqueProperties(Block block) {
    Map<String, Boolean> properties = new HashMap<String, Boolean>();  // Property name -> isMerged
    for(EObject item: BlockUtils.iterator(block)) {
      if (!(item instanceof Declaration)) continue;
      Declaration declaration = (Declaration) item;
      String propertyName = declaration.getProperty();
      boolean isMerge = declaration.isMerge();
      Boolean wasMerge = properties.put(propertyName, isMerge);
      if (wasMerge != null && (!wasMerge.booleanValue() || !isMerge)) {
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
        if (!names.add(variableDefinition.getLhs().getVariable().getIdent())) {
          String message = String.format(messages.getString("duplicated_variable"),
              variableDefinition.getLhs().getVariable().getIdent());
          warning(message, variableDefinition, LessPackage.eINSTANCE.getVariableDefinition_Lhs(), 0);
        }
      }
    }
  }
  
  // Check guards of non-mixin selectors
  @Check
  @ConfigurableCheck(configurable = false)
  public void checkGuardsInToplevelRuleSet(ToplevelRuleSet ruleSet) {
    EList<ToplevelSelector> selectors = ruleSet.getSelector();
    for (int i = 0, n = selectors.size() - 1; i < n; ++i) {
      checkSelectorHasNoGuard(selectors.get(i).getGuards());
    }
  }
  
  @Check
  @ConfigurableCheck(configurable = false)
  public void checkGuardsInInnerRuleSet(InnerRuleSet ruleSet) {
    EList<InnerSelector> selectors = ruleSet.getSelector();
    for (int i = 0, n = selectors.size() - 1; i < n; ++i) {
      checkSelectorHasNoGuard(selectors.get(i).getGuards());
    }    
  }
  
  private void checkSelectorHasNoGuard(MixinDefinitionGuards guards) {
    if (guards != null) {
      error(messages.getString("unexpected_guard"), guards, null, 0);
    }
  }

  @Check
  @ConfigurableCheck(configurable = false)
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
    if (selectors.getSelector().size() > 1) {
      error(messages.getString("unexpected_selector"), selectors, LessPackage.eINSTANCE.getMixinSelectors_Selector(), 1);      
    }
  }
  
  private void checkMixinDefinitionParameters(MixinParameters parameters) {
    checkMixinDefinitionParameters_SameSeparators(parameters);
    checkMixinDefinitionParameters_NoDummySep(parameters);
    checkMixinDefinitionParameters_SingleTermsAreVariables(parameters);
    checkMixinDefinitionParameters_VarArgSyntax(parameters);
    checkMixinDefinitionParameters_UniqueVariableNames(parameters);
    checkMixinDefinitionParameters_OptionalParametersAtTheEnd(parameters);
  }
  
  private void checkMixinDefinitionParameters_SameSeparators(MixinParameters parameters) {
    // Check that separators are the same
    EList<String> separators = parameters.getSep();
    if (separators.size() > 1) {
      String firstSeparator = separators.get(0);
      for (int i = 1; i < separators.size(); ++i) {
        if (!separators.get(i).equals(firstSeparator))
          error(messages.getString("unexpected_separator"), parameters, LessPackage.eINSTANCE.getMixinParameters_Sep(), i);
      }
    }    
  }
  
  private void checkMixinDefinitionParameters_NoDummySep(MixinParameters parameters) {
    if (parameters.getDummySep() != null) {
      error(messages.getString("unexpected_separator"), parameters, LessPackage.eINSTANCE.getMixinParameters_DummySep(), 0);      
    }
  }

  private void checkMixinDefinitionParameters_SingleTermsAreVariables(MixinParameters parameters) {
    // Check that only one variable term is provided when there is no default value
    for (MixinParameter parameter : parameters.getParameter()) {
      if (!parameter.isHasDefaultValue()) {
        if (parameter.getTerm().size() > 1) {
          error(messages.getString("unexpected_term"), parameter, LessPackage.eINSTANCE.getMixinParameter_Term(), 1);
        } else {
          Term term = parameter.getTerm().get(0);
          if (!(term instanceof IdentTerm || term instanceof StringTerm || 
              term instanceof NumericLiteral || MixinUtils.isVariableRefOrNumericLiteral(term))) {
            error(messages.getString("unexpected_term"), parameter, LessPackage.eINSTANCE.getMixinParameter_Term(), 0);              
          }
        }
      }
    }
    
  }

  private void checkMixinDefinitionParameters_VarArgSyntax(MixinParameters parameters) {
    // Check the var parameter:
    // - Has the same separator as the rest, if any,
    // - Has no default value
    MixinVarParameter varParameter = parameters.getVarArg();
    EList<String> separators = parameters.getSep();
    EList<MixinParameter> parameterList = parameters.getParameter();
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

  private void checkMixinDefinitionParameters_UniqueVariableNames(MixinParameters parameters) {
    // Check uniqueness of variable names
    Set<String> names = new HashSet<String>();
    for (MixinParameter parameter : parameters.getParameter()) {
      String variable = MixinUtils.getVariableName(parameter);
      if (variable != null && !names.add(variable)) {
        String message = String.format(messages.getString("duplicated_variable_mixin"), variable);
        if (parameter.isHasDefaultValue())
          warning(message, parameter, LessPackage.eINSTANCE.getMixinParameter_Ident(), 0);
        else
          warning(message, parameter, LessPackage.eINSTANCE.getMixinParameter_Term(), 0);
      }
    }    
  }

  private void checkMixinDefinitionParameters_OptionalParametersAtTheEnd(MixinParameters parameters) {
    // Check that optional parameters are at the end in mixin definitions
    boolean hasOptional = false;
    boolean lastOptional = false;
    int index = 0;
    int firstOptionalIndex = 0;
    for (MixinParameter parameter : parameters.getParameter()) {
      lastOptional = parameter.isHasDefaultValue();
      if (lastOptional) {
        if (!hasOptional) {
          hasOptional = true;
          firstOptionalIndex = index;
        }
      } else {
        if (hasOptional) {
          String message = messages.getString("illegal_optional_parameter");
          warning(message, parameters.getParameter().get(firstOptionalIndex), null, 0);
        }
      }
      ++index;
    }
    if (lastOptional && parameters.getVarArg() != null && parameters.getVarArg().getSep() == null) {
      String message = messages.getString("illegal_optional_parameter_var_args");
      error(message, parameters.getVarArg(), null, 0);      
    }
  }
  
  private void checkMixinCall(MixinUtils.Helper helper) {
    MixinParameters parameters = helper.getParameters();
    if (parameters != null) checkMixinCallParameters_Syntax(parameters);
    MixinLink linkingResult = mixinLinkingService.getLinkedMixin(helper);
    if (!linkingResult.isSuccess()) {
      List<ICheckMixinError> errors = linkingResult.getError();
      if (errors == null) {
        if (checkMixinCallParameters.get(this.getCurrentObject()))
          warning(messages.getString("mixin_parameters_match_no_definition"), helper.getSelectors(), null, 0);
      } else {
        for (ICheckMixinError error : errors)
          error.report(messages, getMessageAcceptor());
      }
    }
  }
  
  private void checkMixinCallParameters_Syntax(MixinParameters parameters) {
    if (parameters.getVarArg() != null) {
      error(messages.getString("unexpected_token"), parameters.getVarArg(), null, 0);
    }
  }
  
  @Check
  @ConfigurableCheck(configurable = false)
  public void checkTerminatedMixinsInKeyframesStatement(KeyframesStatement statement) {
    KeyframesContents contents = statement.getContents();
    while (contents != null) {
      EObject item = contents.getItem();
      if (item instanceof TerminatedMixin) {
        TerminatedMixin mixin = (TerminatedMixin) item;
        MixinUtils.Helper helper = MixinUtils.newHelper(mixin);
        if (helper.isDefinition()) {
          error(messages.getString("unexpected_token"), mixin.getBody(), null, 0);          
        }
        if (mixin.getPriority() != null) {
          error(messages.getString("unexpected_token"), mixin.getPriority(), null, 0);
        }
      }
      contents = contents.getNext();
    }
  }

  // Report error for IncompleteToplevelStatement
  @Check
  @ConfigurableCheck(configurable = false)
  public void checkIncompleteToplevelStatement(IncompleteToplevelStatement statement) {
    error(messages.getString("incomplete_toplevel_statement"), statement, null, 0);
  }

  
  private static Pattern N_MINUS_INT = Pattern.compile("n-[0-9]+");
  @Check
  @ConfigurableCheck(configurable = false)
  public void checkPseudoClassNthSpecialCase(PseudoClassNthSpecialCase pseudo) {
    doCheckPseudoClassNthSpecialCaseIdentEquals(
        pseudo, LessPackage.eINSTANCE.getPseudoClassNthSpecialCase_Ident1(), pseudo.getIdent1(), "-n");
    doCheckPseudoClassNthSpecialCaseIdentMatches(
        pseudo, LessPackage.eINSTANCE.getPseudoClassNthSpecialCase_Ident2(), pseudo.getIdent2(), N_MINUS_INT);
    doCheckPseudoClassNthSpecialCaseIdentMatches(
        pseudo, LessPackage.eINSTANCE.getPseudoClassNthSpecialCase_Ident3(), pseudo.getIdent3(), N_MINUS_INT);
    doCheckPseudoClassNthSpecialCaseIdentEquals(
        pseudo, LessPackage.eINSTANCE.getPseudoClassNthSpecialCase_Ident4(), pseudo.getIdent4(), "-n-");
    doCheckPseudoClassNthSpecialCaseIdentEquals(
        pseudo, LessPackage.eINSTANCE.getPseudoClassNthSpecialCase_Ident5(), pseudo.getIdent5(), "n-");
  }
  
  private void doCheckPseudoClassNthSpecialCaseIdentEquals(PseudoClassNthSpecialCase pseudo, EAttribute attribute, String provided, String expected) {
    if (provided == null || provided.equals(expected)) return;
    error(messages.getString("illegal_pseudo_class_argument"), pseudo, attribute, 0);
  }

  private void doCheckPseudoClassNthSpecialCaseIdentMatches(PseudoClassNthSpecialCase pseudo, EAttribute attribute, String provided, Pattern expected) {
    if (provided == null || expected.matcher(provided).matches()) return;
    error(messages.getString("illegal_pseudo_class_argument"), pseudo, attribute, 0);
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
