package net.vtst.ow.closure.compiler.compile;

import net.vtst.ow.closure.compiler.deps.GetDependenciesNodeTraversal;

import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.HotSwapCompilerPass;
import com.google.javascript.jscomp.NodeTraversal;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

/**
 * A compiler pass that builds a mapping of namespaces to declaring nodes (i.e. a 
 * {@code NamespaceProvidersMap}.
 * @author Vincent Simonet
 */
public class NamespaceProvidersPass implements HotSwapCompilerPass {
  
  private AbstractCompiler compiler;
  private NamespaceProvidersMap map;

  public NamespaceProvidersPass(AbstractCompiler compiler, NamespaceProvidersMap map) {
    this.compiler = compiler;
    this.map = map;
  }

  @Override
  public void process(Node externs, Node root) {
    traversal.traverse(root);
  }

  @Override
  public void hotSwapScript(Node scriptRoot, Node originalRoot) {
    map.removeAll(scriptRoot);
    traversal.traverse(scriptRoot);
  }

  /**
   * The node traversal used by the compiler pass.
   */
  private NodeTraversal traversal = new NodeTraversal(compiler, new NodeTraversal.Callback() {
    
    private Node currentScript;

    // TODO: Re-implement using the coding convention.
    @Override
    public boolean shouldTraverse(NodeTraversal traversal, Node node, Node parent) {
      switch (node.getType()) {
      case Token.BLOCK:
      case Token.EXPR_RESULT:
        return true;
      case Token.SCRIPT:
        currentScript = node;
        return true;
      case Token.CALL:
        String callee = node.getFirstChild().getQualifiedName();
        if (GetDependenciesNodeTraversal.GOOG_PROVIDE.equals(callee)) {
          if (node.getChildCount() == 2) {
            Node argument = node.getChildAtIndex(1);
            if (argument.getType() == Token.STRING) {
              map.put(argument.getString(), currentScript);
            }
          }
        }
        return false;
      default:
        return false;
      }
    }

    @Override
    public void visit(NodeTraversal traversal, Node node, Node parent) {}

  });

}
