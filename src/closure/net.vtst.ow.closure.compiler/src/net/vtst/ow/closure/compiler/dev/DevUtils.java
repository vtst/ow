package net.vtst.ow.closure.compiler.dev;

import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.NodeTraversal;
import com.google.javascript.rhino.Node;

public class DevUtils {

  public static void printNodeAsTree(AbstractCompiler compiler, Node node) {
    NodeTraversal traversal = new NodeTraversal(compiler, new PrintNodeAsTreeCallback());
    traversal.traverse(node);
  }
  
  private static class PrintNodeAsTreeCallback implements NodeTraversal.Callback {

    private int depth = 0;
    
    @Override
    public boolean shouldTraverse(NodeTraversal traversal, Node node, Node parent) {
      for (int i = 0 ; i < depth; i++) {
        System.out.print("  ");
      }
      System.out.println(node.toString() + " (" + Integer.toString(node.getType()) + ")");
      for (int i = 0 ; i < depth; i++) {
        System.out.print("  ");
      }
      System.out.print(node.getLineno());
      System.out.print(" ");
      if (!node.isSyntheticBlock())
        System.out.print(node.getSourceOffset());
      System.out.print("  ");
      System.out.print(node.getLength());
      System.out.println();
      depth++;
      return true;
    }

    @Override
    public void visit(NodeTraversal traversal, Node node, Node parent) {
      depth--;
    }
    
  }


}
