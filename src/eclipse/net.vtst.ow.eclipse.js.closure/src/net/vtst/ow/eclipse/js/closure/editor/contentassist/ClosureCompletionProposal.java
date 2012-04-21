package net.vtst.ow.eclipse.js.closure.editor.contentassist;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import net.vtst.ow.eclipse.js.closure.editor.JSElementInfo;
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

  private JSElementInfo elementInfo;

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
      boolean isProperty, boolean isLocalVariable) {
    super(context, name);
    elementInfo = new JSElementInfo(context.getCompilerRun(), node, type, docInfo, isProperty, isLocalVariable);
  }

  @Override
  public int getRelevance() {
    switch (elementInfo.getKind()) {
    case LOCAL_VARIABLE: 
      return 5;
    case NAMESPACE: 
      return 4;
    case CLASS:
    case INTERFACE:
    case ENUM:
      return 3;
    case GLOBAL_VARIABLE: 
      return 2;
    case METHOD:
    case FIELD:
    case CONSTANT:
      return 1;
    }
    return 0;
  }

  @Override
  protected String getImageName() {
    return elementInfo.getImageName();
  }

  @Override
  protected List<Fragment> makeFragments() {
    LinkedList<Fragment> list = new LinkedList<Fragment>();
    list.add(new Fragment(getDisplayString()));
    if (elementInfo.getType().isFunctionType()) {
      addFragmentsForFunctionParameters(list, Utils.getFunctionNode(elementInfo.getNode()));
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
    switch (elementInfo.getKind()) {
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
  protected IAdditionalProposalInfoProvider getAdditionalProposalInfoProvider() {
    return elementInfo;
  }

  // TODO: This is temporary and should be deleted
  public void addVisibility(Visibility extraVisibility) {
    elementInfo.addVisibility(extraVisibility);
  }
}
