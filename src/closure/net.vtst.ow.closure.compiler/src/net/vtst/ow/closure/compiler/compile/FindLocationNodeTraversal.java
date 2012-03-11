package net.vtst.ow.closure.compiler.compile;

import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.NodeTraversal;
import com.google.javascript.jscomp.Scope;
import com.google.javascript.rhino.Node;

/**
 * Node traversal for finding the closest node containing a given offset in a source file.
 * @author Vincent Simonet
 */
public class FindLocationNodeTraversal extends NodeTraversal {

  private Callback callback;

  private FindLocationNodeTraversal(AbstractCompiler compiler, Callback callback) {
    super(compiler, callback);
    this.callback = callback;
  }
  
  private FindLocationNodeTraversal(AbstractCompiler compiler, String filename, int offset) {
    //super(compiler, new Callback(compiler, filename, offset));
    this(compiler, new Callback(compiler, filename, offset));
  }
  
  private Node getBestNode() {
    return callback.bestNode;
  }

  private Scope getBestScope() {
    return callback.bestScope;
  }

  private static class Callback implements NodeTraversal.Callback {

    private String filename;
    private int offset;
    private Node bestNode = null;
    private Scope bestScope;
    private int bestLength = Integer.MAX_VALUE;

    public Callback(AbstractCompiler compiler, String filename, int offset) {
      this.filename = filename;
      this.offset = offset;
    }
    
    @Override
    public boolean shouldTraverse(NodeTraversal traversal, Node node, Node parent) {
      String nodeFilename = node.getSourceFileName();
      if (node.isSyntheticBlock()) return true;
      if (nodeFilename != null && !nodeFilename.equals(filename)) return false;
      int nodeOffset = node.getSourceOffset();
      int nodeLength = node.getLength();
      if (nodeOffset <= offset && offset < nodeOffset + nodeLength) {
        if (nodeLength < bestLength) {
          bestNode = node;
          bestLength = nodeLength;
          bestScope = traversal.getScope();
        }
        return true;
      }
      return false;
    }

    @Override
    public void visit(NodeTraversal traversal, Node node, Node parent) {}
    
  }

  public static Node findNode(
      AbstractCompiler compiler, Node root, String filename, int offset) {
    FindLocationNodeTraversal traversal = 
        new FindLocationNodeTraversal(compiler, filename, offset);
    traversal.traverse(root);
    return traversal.getBestNode();
  }

}
