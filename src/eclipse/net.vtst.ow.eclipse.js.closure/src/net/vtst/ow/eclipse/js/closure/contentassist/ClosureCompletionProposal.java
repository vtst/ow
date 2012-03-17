package net.vtst.ow.eclipse.js.closure.contentassist;

import java.util.LinkedList;
import java.util.List;

import net.vtst.ow.eclipse.js.closure.OwJsClosureImages;
import net.vtst.ow.eclipse.js.closure.util.Utils;

import com.google.common.base.Preconditions;
import com.google.javascript.jscomp.NodeUtil;
import com.google.javascript.rhino.JSDocInfo;
import com.google.javascript.rhino.JSDocInfo.Visibility;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;
import com.google.javascript.rhino.jstype.FunctionType;
import com.google.javascript.rhino.jstype.JSType;

/**
 * Completion proposal generated from the Closure compiler.  Thanks to
 * {@code AbstractCompletionProposal}, computations are made as lazy as possible.
 * @author Vincent Simonet
 */
public class ClosureCompletionProposal extends AbstractCompletionProposal {
  
  /**
   * The different kind of completion proposals.  Kinds are used to choose the right icon,
   * and to adapt some aspect of the behavior of the completion proposal.
   */
  private enum Kind {
    NAMESPACE,
    CLASS,
    INTERFACE,
    METHOD,
    FIELD,
    GLOBAL_VARIABLE,
    LOCAL_VARIABLE,
    UNKNOWN;
  }

  private Kind kind;
  private Visibility visibility = Visibility.PUBLIC;
  private Node node;
  private JSType type;
  private JSDocInfo docInfo;
  private boolean isProperty;
  private boolean isLocalVariable;
  private boolean isNamespace;

  /**
   * @param context  The context in which the completion proposal is created.
   * @param name  The name of the completion proposal, used as display and completion strings.
   * @param node  The name node for the completion proposal.
   * @param type  The type for the completion proposal.
   * @param docInfo  The doc info for the completion proposal.
   * @param isProperty  true if this is a completion proposal for an object property.
   * @param isLocalVariable  true if this is a completion proposal for a local variable.
   * @param isNamespace  true if this is a completion proposal for a namespace.
   */
  public ClosureCompletionProposal(
      ClosureContentAssistIncovationContext context, String name, 
      Node node, JSType type, JSDocInfo docInfo,
      boolean isProperty, boolean isLocalVariable, boolean isNamespace) {
    super(context, name);
    this.node = node;
    this.type = type;
    this.docInfo = docInfo;
    this.isProperty = isProperty;
    this.isLocalVariable = isLocalVariable;
    this.isNamespace = isNamespace;
    this.kind = getKind();
  }

  /**
   * Add a new visibility to the completion proposal.  By default, completion proposal are
   * considered as public.  The finally computed visibility is the lowest one.
   * @param extraVisibility  The visibility to add.
   */
  public void addVisibility(Visibility extraVisibility) {
    if (visibility == Visibility.PROTECTED && extraVisibility == Visibility.PRIVATE ||
        visibility == Visibility.PUBLIC && (extraVisibility == Visibility.PRIVATE || extraVisibility == Visibility.PROTECTED)) {
      visibility = extraVisibility;
    }
  }

  /**
   * Compute the kind of the completion proposal.
   * @return  The kind of the completion proposal.
   */
  private Kind getKind() {
    if (isNamespace) return Kind.NAMESPACE;
    if (docInfo != null) {
      if (docInfo.isConstructor()) {
        return Kind.CLASS;
      } else if (docInfo.isInterface()) {
        return Kind.INTERFACE;
      }
    }
    if (isProperty) {
      if (type.isFunctionType()) return Kind.METHOD;
      else return Kind.FIELD;
    } else if (isLocalVariable) {
      return Kind.LOCAL_VARIABLE;
    } else {
      return Kind.GLOBAL_VARIABLE;
    }    
  }

  @Override
  public int getRelevance() {
    switch (kind) {
    case LOCAL_VARIABLE: 
      return 5;
    case NAMESPACE: 
      return 4;
    case CLASS:
    case INTERFACE:
      return 3;
    case GLOBAL_VARIABLE: 
      return 2;
    case METHOD:
    case FIELD:
      return 1;
    }
    return 0;
  }

  @Override
  protected String getImageName() {
    switch (kind) {
    case NAMESPACE: return(OwJsClosureImages.PACKAGE);
    case CLASS: return(OwJsClosureImages.CLASS);
    case INTERFACE: return(OwJsClosureImages.INTERFACE);
    case METHOD:
      switch (visibility) {
      case PRIVATE: return(OwJsClosureImages.METHOD_PRIVATE);
      case PROTECTED: return(OwJsClosureImages.METHOD_PROTECTED);
      case PUBLIC: return(OwJsClosureImages.METHOD_PUBLIC);
      }
      break;
    case FIELD:
      switch (visibility) {
      case PRIVATE: return(OwJsClosureImages.FIELD_PRIVATE);
      case PROTECTED: return(OwJsClosureImages.FIELD_PROTECTED);
      case PUBLIC: return(OwJsClosureImages.FIELD_PUBLIC);
      }
      break;
    case GLOBAL_VARIABLE: return(OwJsClosureImages.GLOBAL_VARIABLE);
    case LOCAL_VARIABLE: return(OwJsClosureImages.LOCAL_VARIABLE);
    }
    return null;
  }

  @Override
  protected List<Fragment> makeFragments() {
    LinkedList<Fragment> list = new LinkedList<Fragment>();
    list.add(new Fragment(getDisplayString()));
    if (type.isFunctionType()) {
      addFragmentsForFunctionParameters(list, Utils.getFunctionNode(node));
    }
    return list;
  }
  
  protected char[] makeExitCharactersForLinkedMode() {
    return new char[]{')'};
  }
  
  /**
   * Add the fragments for the parameters of a function.
   * @param list  The list to which the fragments will be added.
   * @param fnNode  The function node.
   */
  private void addFragmentsForFunctionParameters(List<Fragment> list, Node fnNode) {
    if (fnNode == null) return;
    Preconditions.checkState(fnNode.getType() == Token.FUNCTION);
    JSType type = fnNode.getJSType();
    if (type == null || type.isUnknownType()) return;
    FunctionType funType = type.toMaybeFunctionType();
    Node paramNode = NodeUtil.getFunctionParameters(fnNode).getFirstChild();
    list.add(new Fragment("("));
    boolean first = true;
    for (@SuppressWarnings("unused") Node parameterTypeNode : funType.getParameters()) {
      // Bail out if the paramNode is not there.
      if (paramNode == null) break;
      if (first) first = false;
      else list.add(new Fragment(", "));
      list.add(new LinkedFragment(paramNode.getString()));
      paramNode = paramNode.getNext();
    }
    list.add(new Fragment(")"));
  }

  @Override
  protected char[] makeTriggerCharacters() {
    switch (kind) {
    case NAMESPACE: 
      return new char[]{'.'};
    case CLASS:
    case INTERFACE:
      return new char[]{'.', ',', ';'};
    case METHOD: 
      return new char[]{'(', ' ', ',', ';'};
    case FIELD: 
    case GLOBAL_VARIABLE: 
    case LOCAL_VARIABLE:
      return new char[]{' ', '.', ',', ';'};
    }
    return new char[]{};
  }

  @Override
  protected IAdditionalProposalInfo makeAdditionalProposalInfo() {
    return new ClosureAdditionalProposalInfo(node, docInfo, type);
  }

}
