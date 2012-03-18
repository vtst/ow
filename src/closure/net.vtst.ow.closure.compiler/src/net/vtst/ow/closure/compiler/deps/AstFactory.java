package net.vtst.ow.closure.compiler.deps;

import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.JsAst;
import com.google.javascript.jscomp.SourceAst;
import com.google.javascript.jscomp.SourceFile;
import com.google.javascript.rhino.InputId;
import com.google.javascript.rhino.Node;

/**
 * This class extends {@code JsAst} by allowing clones of the AST to be generated without
 * re-parsing the source file if it has not been modified. <br/>
 * Instances of this class should never be passed to a compiler if their clones are used!
 * 
 * @author Vincent Simonet
 */
public class AstFactory extends JsAst {
  private static final long serialVersionUID = 1L;
  
  public AstFactory(SourceFile sourceFile) {
    super(sourceFile);
  }

  public JsAst getClone() {
    return new AstFactory.ClonedAst(this);
  }

  static class ClonedAst extends JsAst {
    private static final long serialVersionUID = 1L;

    private SourceAst fatherSourceAst;
    private Node root;

    public ClonedAst(SourceAst fatherSourceAst) {
      super(fatherSourceAst.getSourceFile());
      this.fatherSourceAst = fatherSourceAst;
    }
    
    @Override
    public void clearAst() {
      root = null;
    }

    @Override
    public Node getAstRoot(AbstractCompiler compiler) {
      if (root == null) {
        Node fatherRoot = fatherSourceAst.getAstRoot(compiler);
        root = fatherRoot.cloneTree();
      }
      return root;
    }

    @Override
    public InputId getInputId() {
      return fatherSourceAst.getInputId();
    }

    @Override
    public SourceFile getSourceFile() {
      return fatherSourceAst.getSourceFile();
    }

    @Override
    public void setSourceFile(SourceFile sourceFile) {
      throw new UnsupportedOperationException();    
    }
  }
}
