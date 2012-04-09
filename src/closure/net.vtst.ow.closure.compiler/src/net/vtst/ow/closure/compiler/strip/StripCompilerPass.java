package net.vtst.ow.closure.compiler.strip;

import java.io.IOException;
import java.io.Writer;

import net.vtst.ow.closure.compiler.magic.MagicCodePrinterBuilder;

import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerPass;
import com.google.javascript.jscomp.NodeTraversal;
import com.google.javascript.rhino.JSDocInfo;
import com.google.javascript.rhino.JSDocInfo.Visibility;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

/**
 * Compiler pass that
 * <ul>
 *   <li>removes the top level definitions which are tagged as private,</li>
 *   <li>removes the body of functions,</li>
 *   <li>pretty-print the remaining code into a string.</li>
 * </ul>
 */
public class StripCompilerPass implements CompilerPass {

  /*
   * The typical structure of the root node is as follows:
   * BLOCK [synthetic: 1]
   *   BLOCK [synthetic: 1]
   *     SCRIPT [synthetic: 1] [source_file: {SyntheticVarsDeclar}] [input_id: InputId: {SyntheticVarsDeclar}]
   *       <<syntetic variable declarations>>
   *   BLOCK [synthetic: 1]
   *     SCRIPT 1 [synthetic: 1] [source_file: src/test.js] [input_id: InputId: src/test.js]
   *       EXPR_RESULT 12 [source_file: src/test.js]
   *         ASSIGN 12 [jsdoc_info: JSDocInfo] [source_file: src/test.js] : function (new:vtst.foo, number, number): number
   *           GETPROP 12 [source_file: src/test.js] : function (new:vtst.foo, number, number): number
   *             NAME vtst 12 [source_file: src/test.js] : ?
   *             STRING foo 12 [source_file: src/test.js] : string
   *           FUNCTION  12 [source_file: src/test.js] : function (new:vtst.foo, number, number): number
   *             NAME  12 [source_file: src/test.js]
   *             LP 12 [source_file: src/test.js]
   *               <<arguments>>
   *             BLOCK 12 [source_file: src/test.js]
   *               <<function body>>
   */

  private Compiler compiler;
  private Writer writer;
  private IOException exception = null;
  
  public StripCompilerPass(Compiler compiler, Writer writer) {
    this.compiler = compiler;
    this.writer = writer;
  }
  
  /**
   * Get the exception that has been raised by the writer, or null if no exception has been
   * raised.
   * @return  The exception, or null.
   */
  public IOException getException() {
    return exception;
  }

  /**
   * The node traversal used by the compiler pass.
   */
  private NodeTraversal traversal = new NodeTraversal(compiler, new NodeTraversal.Callback() {
    
    /**
     * Get the original JSDoc comment string for a node representing a top-level statement in
     * a JS file. 
     * @param node  The node to introspect.
     * @return  The JSDoc comment string, including the comment delimiters, or null.
     */
    private String getJSDocInfo(Node node) {
      JSDocInfo info = node.getJSDocInfo();
      if (info != null) return info.getOriginalCommentString();
      // The JSDoc may be hidden in one of the children of the node (but not deeper).
      for (Node child: node.children()) {
        JSDocInfo infoChild = child.getJSDocInfo();
        if (infoChild != null) return infoChild.getOriginalCommentString();
      }
      return null;
    }

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
    public void visit(NodeTraversal traversal, Node node, Node parent) {
      // In the ascending phase, we pretty-print scripts.
      switch (node.getType()) {
      case Token.SCRIPT:
        for (Node child: node.children()) {
          MagicCodePrinterBuilder builder = 
              new MagicCodePrinterBuilder(child, true, false);
          String jsDoc = getJSDocInfo(child);
          try {
            if (jsDoc != null && jsDoc.length() > 0) {
              writer.write(jsDoc);
              writer.write('\n');
            }
            String code = builder.build();
            writer.write(code);
            printSemiColonAndNewLineIfRequired(code);
          } catch (IOException e) {
            exception = e;
            return;
          }
        }
      }
    }

  });
  
  private void printSemiColonAndNewLineIfRequired(String code) throws IOException {
    int length = code.length();
    if (length == 0) return;
    char lastChar = code.charAt(length - 1);
    char secondLastChar = length >= 2 ? code.charAt(length - 2) : '\0';
    if (lastChar == ';') {
      writer.write('\n');
    } else if (secondLastChar != ';' || lastChar != '\n') {
      writer.write(";\n");
    }
  }
    
  @Override
  public void process(Node externs, Node root) {
    traversal.traverse(root);
  }
  
}
