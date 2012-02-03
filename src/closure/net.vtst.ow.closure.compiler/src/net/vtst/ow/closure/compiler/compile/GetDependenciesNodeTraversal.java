package net.vtst.ow.closure.compiler.compile;

import java.util.Collection;

import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.DiagnosticType;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.NodeTraversal;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

/**
 * A node traversal to get the dependencies specified in the goog.require and goog.provide
 * directives of a JavaScript file.
 * @author Vincent Simonet
 */
public class GetDependenciesNodeTraversal extends NodeTraversal {

  private static final String GOOG_PROVIDE = "goog.provide";
  private static final String GOOG_REQUIRE = "goog.require";
  
  static final DiagnosticType WRONG_NUMBER_OF_ARGUMENTS = DiagnosticType.warning(
      "OW_GOOG_DIRECTIVE_WRONG_NUMBER_OF_ARGUMENTS",
      "{0} expects exactly one argument");

  static final DiagnosticType WRONG_ARGUMENT_TYPE = DiagnosticType.warning(
      "OW_GOOG_DIRECTIVE_WRONG_ARGUMENT_TYPE",
      "{0} expects a string literal as argument");

  
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

  public GetDependenciesNodeTraversal(
      AbstractCompiler compiler, 
      Collection<String> providedNames, 
      Collection<String> requiredNames) {
    super(compiler, new Callback(compiler, providedNames, requiredNames));
  }
  
  private static class Callback implements NodeTraversal.Callback {
    
    private AbstractCompiler compiler;
    private Collection<String> providedNames;
    private Collection<String> requiredNames;
    
    Callback(
        AbstractCompiler compiler, 
        Collection<String> providedNames, 
        Collection<String> requiredNames) {
      this.compiler = compiler;
      this.providedNames = providedNames;
      this.requiredNames = requiredNames;
    }

    @Override
    public boolean shouldTraverse(NodeTraversal traversal, Node node, Node parent) {
      switch (node.getType()) {
      case Token.BLOCK:
      case Token.SCRIPT:
      case Token.EXPR_RESULT:
        return true;
      case Token.CALL:
        String callee = node.getFirstChild().getQualifiedName();
        if (GOOG_REQUIRE.equals(callee)) {
          if (node.getChildCount() != 2) {
            compiler.report(JSError.make(node.getSourceFileName(), node, WRONG_NUMBER_OF_ARGUMENTS, GOOG_REQUIRE));
            return false;
          }
          Node argument = node.getChildAtIndex(1);
          if (argument.getType() == Token.STRING) {
            requiredNames.add(argument.getString());
          } else {
            compiler.report(JSError.make(node.getSourceFileName(), node, WRONG_ARGUMENT_TYPE, GOOG_REQUIRE));
            return false;            
          }
        } else if (GOOG_PROVIDE.equals(callee)) {
          if (node.getChildCount() != 2) {
            compiler.report(JSError.make(node.getSourceFileName(), node, WRONG_NUMBER_OF_ARGUMENTS, GOOG_PROVIDE));
            return false;
          }
          Node argument = node.getChildAtIndex(1);
          if (argument.getType() == Token.STRING) {
            providedNames.add(argument.getString());
          } else {
            compiler.report(JSError.make(node.getSourceFileName(), node, WRONG_ARGUMENT_TYPE, GOOG_PROVIDE));
            return false;            
          }
        }
        return false;
      default:
        return false;
      }
    }

    @Override
    public void visit(NodeTraversal traversal, Node node, Node parent) {}
    
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
