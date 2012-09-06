package net.vtst.ow.closure.compiler.deps;

import java.util.Collection;

import javax.management.modelmbean.RequiredModelMBean;

import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.CodingConvention;
import com.google.javascript.jscomp.NodeTraversal;
import com.google.javascript.rhino.Node;

/**
 * A node traversal to get the dependencies specified in the goog.require and goog.provide
 * directives of a JavaScript file.
 * @author Vincent Simonet
 */
public class GetDependenciesNodeTraversal extends NodeTraversal {

  
  /* The typical structure of a root node is: 
    BLOCK [synthetic: 1]
      SCRIPT 1 [synthetic: 1] [source_file: src/test.js] [length: 441] [input_id: InputId: src/test.js]
        EXPR_RESULT 1 [source_file: src/test.js] [length: 21]
          CALL 1 [source_file: src/test.js] [length: 20]
            GETPROP 1 [source_file: src/test.js] [length: 12]
              NAME goog 1 [source_file: src/test.js] [length: 4]
              STRING provide 1 [source_file: src/test.js] [length: 7]
            STRING vtst 1 [source_file: src/test.js] [length: 6]
  */
  
  private Collection<String> requiredNames;

  public GetDependenciesNodeTraversal(
      AbstractCompiler compiler, 
      Collection<String> providedNames, 
      Collection<String> requiredNames) {
    super(compiler, new Callback(compiler, providedNames, requiredNames));
    this.requiredNames = requiredNames;
  }
  
  private static class Callback implements NodeTraversal.Callback {
    
    private CodingConvention codingConvention;
    private Collection<String> providedNames;
    private Collection<String> requiredNames;
    
    Callback(
        AbstractCompiler compiler, 
        Collection<String> providedNames, 
        Collection<String> requiredNames) {
      this.providedNames = providedNames;
      this.requiredNames = requiredNames;
      this.codingConvention = compiler.getCodingConvention();
    }

    @Override
    // This code is inspired from CompilerInput.DepsFinder
    public boolean shouldTraverse(NodeTraversal traversal, Node node, Node parent) {
      if (node.isCall()) {
        String require = codingConvention.extractClassNameIfRequire(node, parent);
        if (require != null) requiredNames.add(require);
        String provide = codingConvention.extractClassNameIfProvide(node, parent);
        if (provide != null) providedNames.add(provide);
        return false;
      } else {
        return (parent == null || parent.isExprResult() || parent.isScript());
      }
    }

    @Override
    public void visit(NodeTraversal traversal, Node node, Node parent) {}
    
  }
  
  public void traverse(Node node) {
    // We need to add the 'goog' object so that base.js is included even if no goog.require statement
    // is present in the file.
    requiredNames.add(JSLibrary.GOOG);
    super.traverse(node);
  }
  
  /**
   * Run the node traversal to get the dependencies.
   * @param compiler  The compiler used to report errors.
   * @param node  The node to traverse.
   * @param providedNames  The collection to which providedNames are added.
   * @param requiredNames  The collection to which requiredNames are added.
   */
  public static void run(
      AbstractCompiler compiler, Node node,
      Collection<String> providedNames, Collection<String> requiredNames) {
    GetDependenciesNodeTraversal traversal = 
        new GetDependenciesNodeTraversal(compiler, providedNames, requiredNames);
    traversal.traverse(node);
  }

}
