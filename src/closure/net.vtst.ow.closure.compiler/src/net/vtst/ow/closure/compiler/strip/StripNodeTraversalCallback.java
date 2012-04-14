package net.vtst.ow.closure.compiler.strip;

import com.google.javascript.jscomp.NodeTraversal;
import com.google.javascript.rhino.JSDocInfo;
import com.google.javascript.rhino.JSDocInfo.Visibility;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

public class StripNodeTraversalCallback implements NodeTraversal.Callback {

  @Override
  public boolean shouldTraverse(NodeTraversal traversal, Node node, Node parent) {
    // In the descending phase, we delete private top-level definitions and the
    // contents of functions.
    switch (node.getType()) {
    case Token.EXPR_RESULT:
      // Delete private top-level definitions.
      if (node.getFirstChild() != null &&
          node.getFirstChild().getType() == Token.ASSIGN &&
          node.getFirstChild().getJSDocInfo() != null &&
          node.getFirstChild().getJSDocInfo().getVisibility() == Visibility.PRIVATE) {
        node.detachFromParent();
        return false; 
      }
      break;
    case Token.FUNCTION:
      // Strip the contents of functions
      for (Node child: node.children()) {
        if (child.getType() == Token.BLOCK) {
          child.removeChildren();
        }
      }
      return false;
    default:
      JSDocInfo info = node.getJSDocInfo();
      if (info != null && info.isConstructor()) return false;
    }
    return true;
  }

  @Override
  public void visit(NodeTraversal arg0, Node arg1, Node arg2) {      
  }

}
