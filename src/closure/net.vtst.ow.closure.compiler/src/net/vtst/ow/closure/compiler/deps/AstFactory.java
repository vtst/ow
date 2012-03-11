package net.vtst.ow.closure.compiler.deps;

import java.io.IOException;

import net.vtst.ow.closure.compiler.util.TimestampKeeper;

import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.DiagnosticType;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.JSSourceFile;
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

  static final DiagnosticType READ_ERROR = DiagnosticType.error(
      "OW_READ_ERROR", "Cannot read: {0}");

  private TimestampKeeper timestampKeeper;
  private String fileName;
  private CompilationUnitProvider.Interface provider;

  public AstFactory(String fileName, CompilationUnitProvider.Interface provider) {
    super(JSSourceFile.fromGenerator(fileName, provider));
    this.timestampKeeper = new TimestampKeeper(provider);
    this.fileName = fileName;
    this.provider = provider;
  }

  public Node getAstRoot(AbstractCompiler compiler) {
    if (timestampKeeper.hasChanged()) {
      try {
        provider.prepareToGetCode();
      } catch (IOException e) {
        compiler.report(JSError.make(READ_ERROR, fileName));
      }
      super.clearAst();
      // There is a bug in the implementation of SourceFile.clearCachedSource(), which
      // is called by clearAst(), because it does not reset the private fields of SourceFile.
      // To work around this, we create a new fresh source file object.
      super.setSourceFile(JSSourceFile.fromGenerator(fileName, provider));
    }
    return super.getAstRoot(compiler);
  }
  
  public SourceAst getClone() {
    return new ClonedAst(this);
  }
  
  private static class ClonedAst implements SourceAst {
    private static final long serialVersionUID = 1L;

    private SourceAst fatherSourceAst;
    private Node root;

    public ClonedAst(SourceAst fatherSourceAst) {
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
