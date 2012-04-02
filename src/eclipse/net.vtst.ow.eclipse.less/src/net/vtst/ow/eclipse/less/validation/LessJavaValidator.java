// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.validation;

import java.util.HashSet;
import java.util.Set;

import net.vtst.ow.eclipse.less.LessMessages;
import net.vtst.ow.eclipse.less.less.Block;
import net.vtst.ow.eclipse.less.less.BlockUtils;
import net.vtst.ow.eclipse.less.less.Declaration;
import net.vtst.ow.eclipse.less.less.HashOrClass;
import net.vtst.ow.eclipse.less.less.HashOrClassCrossReference;
import net.vtst.ow.eclipse.less.less.ImportStatement;
import net.vtst.ow.eclipse.less.less.IncompleteToplevelStatement;
import net.vtst.ow.eclipse.less.less.LessPackage;
import net.vtst.ow.eclipse.less.less.MixinCall;
import net.vtst.ow.eclipse.less.less.MixinDefinition;
import net.vtst.ow.eclipse.less.less.MixinDefinitionParameter;
import net.vtst.ow.eclipse.less.less.MixinDefinitionVariable;
import net.vtst.ow.eclipse.less.less.PseudoClassNthSpecialCase;
import net.vtst.ow.eclipse.less.less.StyleSheet;
import net.vtst.ow.eclipse.less.less.VariableDefinition;
import net.vtst.ow.eclipse.less.scoping.LessImportStatementResolver;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.util.Pair;
import org.eclipse.xtext.util.Tuples;
import org.eclipse.xtext.validation.Check;

import com.google.inject.Inject;
 

public class LessJavaValidator extends AbstractLessJavaValidator {
  
  @Inject
  LessMessages messages;
  
  @Inject
  LessImportStatementResolver importStatementResolver;
  
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
  public void checkBlockUniqueVariables(Block block) {
    checkUniqueVariables(BlockUtils.iterator(block));
  }
  
  // Check for multiple variables in stylesheet
  @Check
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
  
  // Check that optional parameters are at the end in mixin definitions
  @Check
  public void checkMixinDefinitionParameters(MixinDefinition mixinDefinition) {
    boolean hasOptional = false;
    int index = 0;
    for (MixinDefinitionParameter parameter: mixinDefinition.getParameter()) {
      if (parameter instanceof MixinDefinitionVariable &&
          ((MixinDefinitionVariable) parameter).getDefault_value().size() > 0) {
        hasOptional = true;
      } else {
        if (hasOptional) {
          String message = messages.getString("illegal_optional_parameter");
          warning(message, mixinDefinition, LessPackage.eINSTANCE.getMixinDefinition_Parameter(), index);
        }
      }
      ++index;
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
    Pair<Integer, Integer> expected = getNumberOfParametersForMixin(hashOrClass);
    int provided = mixinCall.getParameter().size();
    if (provided < expected.getFirst() || provided > expected.getSecond()) {
      String message = 
          (expected.getFirst() == expected.getSecond() ?
              String.format(messages.getString("illegal_number_of_parameters_for_mixin"),
                  hashOrClass.getIdent(), expected.getFirst(), provided) :
              String.format(messages.getString("illegal_number_of_parameters_for_mixin_range"),
                  hashOrClass.getIdent(), expected.getFirst(), expected.getSecond(), provided));
      warning(message, mixinCall, LessPackage.eINSTANCE.getMixinCall_Selector(), 0);
    }
  }
  
  /** Compute the expected number of parameters for a mixin call.
   * @param hashOrClass
   * @return a pair (min, max)
   */
  private Pair<Integer, Integer> getNumberOfParametersForMixin(HashOrClass hashOrClass) {
    EObject container = hashOrClass.eContainer();
    int required = 0;
    int optional = 0;
    if ((container instanceof MixinDefinition)) {
      MixinDefinition mixinDefinition = (MixinDefinition) container;
      EList<MixinDefinitionParameter> parameters = mixinDefinition.getParameter();
      for (MixinDefinitionParameter parameter: parameters) {
        if (parameter instanceof MixinDefinitionVariable &&
            ((MixinDefinitionVariable) parameter).getDefault_value().size() > 0) {
          ++optional;
        } else {
          ++required;
        }
      }
    }
    return Tuples.pair(required, required + optional);
  }
  
  // Report error for IncompleteToplevelStatement
  @Check
  public void checkIncompleteToplevelStatement(IncompleteToplevelStatement statement) {
    error(messages.getString("incomplete_toplevel_statement"), statement, null, 0);
  }
  
  
  @Check
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
    if (c != '+' && c != '-') return false;
    ++i; if (i == n) return false; else c = special.charAt(i);
    if (c < '0' && c > '9') return false;
    while (c >= '0' && c <= '9' && i < n) {
      ++i; if (i == n) return true; else c = special.charAt(i);
    }
    return false;
  }

}
