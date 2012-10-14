package net.vtst.ow.eclipse.soy.validation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import net.vtst.eclipse.easyxtext.util.Misc;
import net.vtst.ow.eclipse.soy.SoyMessages;
import net.vtst.ow.eclipse.soy.scoping.SoyScopeProvider;
import net.vtst.ow.eclipse.soy.soy.CallCommand;
import net.vtst.ow.eclipse.soy.soy.CallParam;
import net.vtst.ow.eclipse.soy.soy.Command;
import net.vtst.ow.eclipse.soy.soy.CommandAttribute;
import net.vtst.ow.eclipse.soy.soy.DelCallCommand;
import net.vtst.ow.eclipse.soy.soy.DelTemplate;
import net.vtst.ow.eclipse.soy.soy.Expr;
import net.vtst.ow.eclipse.soy.soy.FunctionCall;
import net.vtst.ow.eclipse.soy.soy.FunctionDeclaration;
import net.vtst.ow.eclipse.soy.soy.Items;
import net.vtst.ow.eclipse.soy.soy.ListOrMapLiteral;
import net.vtst.ow.eclipse.soy.soy.ListOrMapLiteralItem;
import net.vtst.ow.eclipse.soy.soy.MsgCommand;
import net.vtst.ow.eclipse.soy.soy.Namespace;
import net.vtst.ow.eclipse.soy.soy.PrintDirective;
import net.vtst.ow.eclipse.soy.soy.PrintDirectiveDeclaration;
import net.vtst.ow.eclipse.soy.soy.RegularCallCommand;
import net.vtst.ow.eclipse.soy.soy.SoyFile;
import net.vtst.ow.eclipse.soy.soy.SoyPackage;
import net.vtst.ow.eclipse.soy.soy.Template;
import net.vtst.ow.eclipse.soy.soy.TemplateParameter;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.impl.HiddenLeafNode;
import org.eclipse.xtext.nodemodel.impl.LeafNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.validation.Check;

import com.google.inject.Inject;
 

public class SoyJavaValidator extends AbstractSoyJavaValidator {
  
  @Inject
  SoyScopeProvider scopeProvider;
  
  @Inject
  SoyMessages messages;
  
  // **************************************************************************
  // Patches for the parser
  
  /**
   * Check that all elements of a list-or-map are either list elements or map elements.
   */
  @Check
  public void checkListOrMapLiteral(ListOrMapLiteral listOrMap) {
    EList<ListOrMapLiteralItem> items = listOrMap.getItem();
    boolean isMap = false;
    int index = 0;
    for (ListOrMapLiteralItem item: items) {
      if (index == 0) {
        isMap = (item.getSecond() != null);
      } else {
        if ((item.getSecond() != null) != isMap) {
          error(messages.getString(isMap ? "map_becoming_list" : "list_becoming_map"), 
              listOrMap, SoyPackage.Literals.LIST_OR_MAP_LITERAL__ITEM, index);
        }
      }
      ++index;
    }
  }
  
  /**
   * Check that there are no nested msg commands.
   */
  @Check
  public void checkNestedMsgCommand(MsgCommand msgCommand) {
    TreeIterator<EObject> treeIterator = msgCommand.eAllContents();
    while (treeIterator.hasNext()) {
      EObject obj = treeIterator.next();
      if (obj instanceof MsgCommand) {
        error(messages.getString("nested_msg"), obj, null, 0);
      }  
      else if (!(obj instanceof Command || obj instanceof Items)) {
        treeIterator.prune();
      }
    }
  }
  
  /**
   * Check there is no whitespace before or after the '=' sign in command attributes
   */
  @Check
  public void checkWhiteSpaceInCommandAttributes(CommandAttribute commandAttribute) {
    doCheckNoHiddenLeafNode(commandAttribute, "command_attribute_whitespace", true);
  }
    
  /**
   * Helper function that checks that the parser nodes of a semantic element do not contain 
   * any hidden leaf node (whitespace, comment, etc.).
   * @param object  The semantic element to check.
   * @param error_message  The ID of the error message to display in case of error.
   * @param stripBegin  If true, hidden leaf nodes which appear at the beginning are ignored.
   */
  private void doCheckNoHiddenLeafNode(EObject object, String error_message, boolean stripBegin) {
    ICompositeNode parserNode = NodeModelUtils.getNode(object);
    boolean atBegin = stripBegin;
    for (INode node: parserNode.getLeafNodes()) {
      if ("\"".equals(node.getText())) break;
      if (node instanceof HiddenLeafNode) {
        if (!atBegin) error(messages.getString(error_message), object, null, 0);
      } else {
        atBegin = false;
      }
    }
  }
  
  // **************************************************************************
  // Command attributes
  
  /**
   * Check the attributes of namespace commands.
   */
  @Check
  public void checkNamespaceCommandAttributes(Namespace namespace) {
    doCheckCommandAttributes(namespace, namespace.getAttribute(), namespaceRequiredAttributes, namespaceOptionalAttributes);
  }
  private static Set<String> namespaceRequiredAttributes = new HashSet<String>(
      Arrays.asList(new String[]{}));
  private static Set<String> namespaceOptionalAttributes = new HashSet<String>(
      Arrays.asList(new String[]{"autoescape"}));
  
  /**
   * Check the attributes of template commands.
   */
  @Check
  public void checkTemplateCommandAttributes(Template template) {
    doCheckCommandAttributes(template, template.getAttribute(), templateRequiredAttributes, templateOptionalAttributes);
  }
  private static Set<String> templateRequiredAttributes = new HashSet<String>(
      Arrays.asList(new String[]{}));
  private static Set<String> templateOptionalAttributes = new HashSet<String>(
      Arrays.asList(new String[]{"private", "autoescape"}));
  
  /**
   * Check the attributes of msg commands.
   */
  @Check
  public void checkMsgCommandAttributes(MsgCommand msgCommand) {
    doCheckCommandAttributes(msgCommand, msgCommand.getAttribute(), msgRequiredAttributes, msgOptionalAttributes);
  }
  private static Set<String> msgRequiredAttributes = new HashSet<String>(
      Arrays.asList(new String[]{"desc"}));
  private static Set<String> msgOptionalAttributes = new HashSet<String>(
      Arrays.asList(new String[]{"meaning", "hidden"}));
  
  /**
   * Check the attributes of call commands (regular and del ones).
   */
  @Check
  // This works for regular and del call commands
  public void checkCallCommandAttributes(CallCommand callCommand) {
    doCheckCommandAttributes(callCommand, callCommand.getAttribute(), callRequiredAttributes, callOptionalAttributes);
  }
  private static Set<String> callRequiredAttributes = new HashSet<String>(
      Arrays.asList(new String[]{}));
  private static Set<String> callOptionalAttributes = new HashSet<String>(
      Arrays.asList(new String[]{"data"}));

  /**
   * Helper function to check the attributes of a command.
   * @param object  The command element to which the attributes belong to.
   * @param commandAttributes  An iterable over the command attributes of that element.
   * @param requiredAttributes  The set of required attributes for that command.
   * @param optionalAttributes  The set of optional attributes for that command.
   */
  private void doCheckCommandAttributes(EObject object, Iterable<CommandAttribute> commandAttributes, 
      Set<String> requiredAttributes, Set<String> optionalAttributes) {
    int foundRequiredAttributes = 0;
    Set<String> attributes = new HashSet<String>();
    for (CommandAttribute commandAttribute: commandAttributes) {
      String ident = commandAttribute.getIdent(); 
      if (ident != null) {
        if (attributes.add(ident)) {
          if (requiredAttributes.contains(ident)) {
            ++foundRequiredAttributes;
          } else if (!optionalAttributes.contains(ident)) {
            error(String.format(messages.getString("command_attribute_unknown"), ident),
                commandAttribute, SoyPackage.eINSTANCE.getCommandAttribute_Ident(), 0);
            
          }
        } else {
          error(String.format(messages.getString("command_attribute_duplicated"), ident),
              commandAttribute, SoyPackage.eINSTANCE.getCommandAttribute_Ident(), 0);
        }
      }
    }

    if (foundRequiredAttributes != requiredAttributes.size()) {
      HashSet<String> missingAttributes = new HashSet<String>();
      missingAttributes.addAll(requiredAttributes);
      missingAttributes.removeAll(attributes);
      String attributeNames = "";
      for (String attributeName: missingAttributes) {
        attributeNames += (attributeNames.isEmpty() ? "" : ", ") + attributeName;
      }
      error(String.format(messages.getString("command_attribute_missing"), attributeNames),
          object, null, 0);
    }
  }

  
  // **************************************************************************
  // Template definitions
  
  /**
   * Check that the template commands are at the beginning of their lines.
   */
  @Check
  public void checkTemplateNewLine(Template template) {
    doCheckNewLineKeywords(template);
  }

  /**
   * Check that the template commands are at the beginning of their lines.
   */
  @Check
  public void checkTemplateNewLine(DelTemplate template) {
    doCheckNewLineKeywords(template);
  }
  
  /**
   * Keywords that must appear at the beginning of a line. 
   */
  private static Set<String> keywordsBeginOfLine = new HashSet<String>(
      Arrays.asList(new String[]{"{template", "{/template}", "{deltemplate", "{/deltemplate}"}));
  
  /**
   * Check that the keywords in {@code keywordsBeginOfLine} that appear within
   * {@code object} are all at the beginning of a line.
   * @param object
   */
  private void doCheckNewLineKeywords(EObject object) {
    ICompositeNode parserNode = NodeModelUtils.getNode(object);
    INode previousNode = null;
    for (INode node: parserNode.getChildren()) {
      EObject grammarElement = node.getGrammarElement();
      if (grammarElement instanceof Keyword) {
        Keyword keyword = (Keyword) grammarElement;
        if (keywordsBeginOfLine.contains(keyword.getValue()) && !nodeEndsByNewLine(previousNode)) {
          error(messages.getString("keyword_must_be_after_newline"), node.getSemanticElement(), null, -1);
        }
      }
      previousNode = node;
    }    
  }
  
  private boolean nodeEndsByNewLine(INode node) {
    if (node == null) return true;
    String text = node.getText();
    int i = text.length() - 1;
    if (i < 0) return false;
    char c = text.charAt(i);
    return c == '\n' || c == '\r';
  }

  /**
   * Check the uniqueness of templates (regular and del) in a soy file.
   */
  @Check
  public void checkTemplateUniqueness(SoyFile soyFile) {
    Set<String> seenIdents = new HashSet<String>();
    for (Template template: soyFile.getTemplate()) {
      String ident = template.getIdent();
      if (ident != null) {
        if (!seenIdents.add(ident)) {
          error(messages.getString("duplicated_template"), template,
              SoyPackage.eINSTANCE.getTemplate_Ident(), 0);
        }
      }
    }
  }

  private static class TemplateParameterRegister {
    private Set<TemplateParameter> set;
    private HashMap<String, TemplateParameter> map = new HashMap<String, TemplateParameter>();
    public TemplateParameterRegister(Collection<TemplateParameter> parameters) {
      this.set = new HashSet<TemplateParameter>(parameters);
      for (TemplateParameter parameter: parameters) map.put(parameter.getIdent(), parameter);
    }
    public void remove(TemplateParameter parameter) { set.remove(parameter); }
    public void remove(EObject object) {
      if (object instanceof TemplateParameter) remove((TemplateParameter) object);
    }
    public void remove(String name) {
      TemplateParameter parameter = map.get(name);
      if (parameter != null) remove(parameter);
    }
    public boolean isEmpty() { return set.isEmpty(); }
    public Iterable<TemplateParameter> get() { return set; }
  }
  
  /**
   * Check the parameters of a template: unique and use.
   */
  @Check
  public void checkTemplateParameters(Template template) {
    // Check use.
    TemplateParameterRegister templateParameters = new TemplateParameterRegister(template.getSoydoc().getParam());
    TreeIterator<EObject> iterator = template.eAllContents();
    while (iterator.hasNext() && !templateParameters.isEmpty()) {
      EObject object = iterator.next();
      for (EObject crossReferencedObject: object.eCrossReferences()) {
        templateParameters.remove(crossReferencedObject);
      }
      if (object instanceof CommandAttribute && isDataAttributeEqualToAll((CommandAttribute) object)) {
        EObject parent = object.eContainer();
        if (parent instanceof CallCommand) {
          Template calledTemplate = getCallCommandTemplate((CallCommand) parent);
          if (calledTemplate != null) {
            for (TemplateParameter calledParameter: calledTemplate.getSoydoc().getParam()) {
              templateParameters.remove(calledParameter.getIdent());
            }
          }
        }
      }
    }
    for (TemplateParameter templateParameter: templateParameters.get()) {
      error(messages.getString("unused_template_parameter"), templateParameter, SoyPackage.eINSTANCE.getVariableDefinition_Ident(), 0);
    }
    
    // Check unicity
    int index = 0;
    Set<String> seenIdents = new HashSet<String>();
    for (TemplateParameter param: template.getSoydoc().getParam()) {
      String ident = param.getIdent();
      if (ident != null) {
        if (!seenIdents.add(ident)) {
          error(messages.getString("duplicated_template_parameter"), template.getSoydoc(),
              SoyPackage.eINSTANCE.getSoyDoc_Param(), index);        
        }
      }
      ++index;
    }
  }

  // **************************************************************************
  // Template calls

  /**
   * Check the parameters of a call command.
   */
  @Check
  public void checkCallCommandParameters(CallCommand callCommand) {
    Template template = getCallCommandTemplate(callCommand);
    // Do not validate unlinked elements
    if (template.getIdent() == null) return;
    
    Set<String> seenCallParams = new HashSet<String>();
    Set<String> requiredParams = new HashSet<String>();
    Set<String> optionalParams = new HashSet<String>();
    for (TemplateParameter templateParam: template.getSoydoc().getParam()) {
      if (templateParam.isOptional()) optionalParams.add(templateParam.getIdent());
      else requiredParams.add(templateParam.getIdent());
    }
    for (CallParam callParam: callCommand.getParam()) {
      String ident = callParam.getIdent();
      if (ident != null) {
        if (!seenCallParams.add(ident)) {
          error(String.format(String.format(messages.getString("duplicated_call_parameter"), ident)),
              callParam, SoyPackage.eINSTANCE.getCallParam_Ident(), 0);
        } else if (!requiredParams.remove(ident) && !optionalParams.remove(ident)) {
          error(String.format(String.format(messages.getString("unexpected_call_parameter"), ident)),
              callParam, SoyPackage.eINSTANCE.getCallParam_Ident(), 0);          
        }
      }
    }

    CommandAttribute dataAttribute = getDataAttribute(callCommand);
    if (dataAttribute != null) {
      if (isDataAttributeEqualToAll(dataAttribute)) {
        // All parameters of the enclosing template are implicitly passed as parameters
        for (TemplateParameter templateParameter: getParametersOfEnclosingTemplate(callCommand)) {
          if (!templateParameter.isOptional()) requiredParams.remove(templateParameter.getIdent());
        }        
      } else {
        // Otherwise, any attribute of the passed variable may be passed as argument, so we don't know
        requiredParams.clear();
      }
    }
    
    if (!requiredParams.isEmpty()) {
      String missingParams = Misc.join(requiredParams, ", ");
      error(String.format(messages.getString("missing_call_parameters"), missingParams), callCommand, 
          getCallCommandIdentStructuralFeature(callCommand), 0);
    }
  }

  /**
   * Helper function which finds the data attribute of a call command.
   * @param callCommand
   * @return  The command attribute, or null.
   */
  private CommandAttribute getDataAttribute(CallCommand callCommand) {
    for (CommandAttribute commandAttribute: callCommand.getAttribute()) {
      if ("data".equals(commandAttribute.getIdent())) {
        return commandAttribute;
      }
    }
    return null;
  }
  
  /**
   * Helper function which tests whether a data attribute is equal to "all"
   */
  private boolean isDataAttributeEqualToAll(CommandAttribute dataAttribute) {
    Expr expr = dataAttribute.getExpr();
    if (expr == null) return false;
    ICompositeNode parserNode = NodeModelUtils.getNode(expr);
    // This is not very elegant, but simple.
    return ("all".equals(parserNode.getText()));
  }
  
  /**
   * Helper function which retrieves the parameters of the enclosing template of a call command.
   * @param callCommand
   * @return
   */
  private Iterable<TemplateParameter> getParametersOfEnclosingTemplate(CallCommand callCommand) {
    EObject object = callCommand;
    while (object != null && !(object instanceof Template)) {
      object = object.eContainer();
    }
    if (object == null) return Collections.emptySet();;
    return ((Template) object).getSoydoc().getParam();
  }
  
  /**
   * Get the template element which is referenced by a call command. This is equivalent to
   * <code>callCommand.getIdent()</code>, with the appropriate type casts.
   * @param callCommand
   * @return
   */
  private Template getCallCommandTemplate(CallCommand callCommand) {
    if (callCommand instanceof RegularCallCommand) return ((RegularCallCommand) callCommand).getIdent();
    else if (callCommand instanceof DelCallCommand) return ((DelCallCommand) callCommand).getIdent();
    return null;
  }
  
  /**
   * Get the structural feature for the identifier of a call command, depending of the type of
   * the call command.
   * @param callCommand
   * @return
   */
  private EStructuralFeature getCallCommandIdentStructuralFeature(CallCommand callCommand) {
    if (callCommand instanceof RegularCallCommand) return SoyPackage.eINSTANCE.getRegularCallCommand_Ident();
    else if (callCommand instanceof DelCallCommand) return SoyPackage.eINSTANCE.getDelCallCommand_Ident();
    return null;
  }

  
  // **************************************************************************
  // Print directives

  /**
   * Check:
   * - There is no whitespace after the '|' in print directives,
   * - The directive name is known,
   * - The directive has the right number of parameters.
   */
  @Check
  public void checkPrintDirective(PrintDirective printDirective) {
    // Check there is no whitespace after the '|'
    ICompositeNode parserNode = NodeModelUtils.getNode(printDirective);
    for (INode node: parserNode.getChildren()) {
      EObject grammarElement = node.getGrammarElement();
      if (grammarElement instanceof Keyword) {
        String keywordValue = ((Keyword) grammarElement).getValue();
        if ("|".equals(keywordValue)) {
          if (node.getNextSibling() instanceof HiddenLeafNode) {
            error(messages.getString("print_directive_whitespace_after_pipe"), printDirective,
                SoyPackage.eINSTANCE.getPrintDirective_Ident(), 0);
          }
        } else if (":".equals(keywordValue)) {
          if (node.getNextSibling() instanceof HiddenLeafNode) {
            error(messages.getString("print_directive_whitespace_before_colon"), printDirective,
                SoyPackage.eINSTANCE.getPrintDirective_Ident(), 0);
          }          
        }
      }
    }
    
    PrintDirectiveDeclaration printDirectiveDeclaration = printDirective.getIdent();
    // Do not validate unlinked elements
    if (printDirectiveDeclaration.getIdent() == null) return;
    doCheckNumberOfArguments(
        printDirective.getParameter().size(), 
        printDirectiveDeclaration.getNumber_of_required_arguments(), 
        printDirectiveDeclaration.getNumber_of_optional_arguments(), 
        printDirectiveDeclaration.getIdent(), 
        "print_directive_parameters", 
        "print_directive_parameters_opt", 
        printDirective, 
        SoyPackage.eINSTANCE.getPrintDirective_Ident(), 
        0);

  }
  
  // **************************************************************************
  // Function calls
  
  /**
   * Check the number of arguments of a function call.
   */
  @Check
  public void checkFunctionCall(FunctionCall functionCall) {
    FunctionDeclaration functionDeclaration = functionCall.getFunction();
    // Do not validate unlinked elements
    if (functionDeclaration.getIdent() == null) return;
    doCheckNumberOfArguments(
        functionCall.getArgument().size(), 
        functionDeclaration.getNumber_of_required_arguments(), 
        functionDeclaration.getNumber_of_optional_arguments(), 
        functionDeclaration.getIdent(), 
        "function_call_arguments", 
        "function_call_arguments_opt", 
        functionCall, 
        SoyPackage.eINSTANCE.getFunctionCall_Function(), 
        0);
  }
  
  /**
   * Helper function to check the number of arguments of a print directive or a function call.
   * @param numberOfArguments  The effective number of arguments.
   * @param numberOfRequiredArguments  The number of required arguments according to the prototype.
   * @param numberOfOptionalArguments  The number of optional arguments according to the prototype.
   * @param ident  The identifier of the print directive or the function call.
   * @param errorMessageFormat  The ID of the message to be displayed in case of error if there is
   *   no optional argument.
   * @param errorMessageFormatOpt  The ID of the message to be displayed in case of error if there are
   *   optional arguments.
   * @param object  The semantic element on which the error has to be reported.
   * @param feature  The structural feature on which the error has to be reported.
   * @param index  The index of the feature.
   */
  private void doCheckNumberOfArguments(
      int numberOfArguments, 
      int numberOfRequiredArguments, 
      int numberOfOptionalArguments, 
      String ident,
      String errorMessageFormat, 
      String errorMessageFormatOpt,
      EObject object,
      EStructuralFeature feature,
      int index) {
    if (numberOfArguments < numberOfRequiredArguments || 
        numberOfArguments > numberOfRequiredArguments + numberOfOptionalArguments) {
      if (numberOfOptionalArguments == 0) {
        error(String.format(messages.getString(errorMessageFormat), ident, numberOfRequiredArguments),
            object, feature, index);
      } else {
        error(String.format(messages.getString(errorMessageFormatOpt), ident, numberOfRequiredArguments, numberOfRequiredArguments + numberOfOptionalArguments),
            object, feature, index);
      }
    }
  }
    
}
