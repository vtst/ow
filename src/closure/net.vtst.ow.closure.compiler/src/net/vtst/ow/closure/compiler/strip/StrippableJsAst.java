package net.vtst.ow.closure.compiler.strip;

import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.JsAst;
import com.google.javascript.jscomp.NodeTraversal;
import com.google.javascript.jscomp.SourceFile;
import com.google.javascript.rhino.Node;

/**
 * An extension of {@code JsAst}, which provides on-the-fly stripping of the AST.
 * @author Vincent Simonet
 */
public class StrippableJsAst extends JsAst {
  private static final long serialVersionUID = 1L;
  
  private Node strippedRoot;

  public StrippableJsAst(SourceFile sourceFile) {
    super(sourceFile);
  }
  
  public void clearAst() {
    super.clearAst();
    strippedRoot = null;
  }

  public Node getStrippedAstRoot(AbstractCompiler compiler) {
    if (strippedRoot == null) {
      strippedRoot = getAstRoot(compiler).cloneTree();
      NodeTraversal traversal = new NodeTraversal(compiler, new StripNodeTraversalCallback());
      traversal.traverse(strippedRoot);
    }
    return strippedRoot;
  }
  
}
