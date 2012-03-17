package net.vtst.ow.closure.compiler.deps;

import java.io.IOException;

import net.vtst.ow.closure.compiler.util.TimestampKeeper;

import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.DiagnosticType;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.JSSourceFile;
import com.google.javascript.rhino.Node;

/**
 * This class extends {@code AstFactory} by re-parsing the source code if it is modified
 * since the last parse.
 * 
 * @author Vincent Simonet
 */
public class AstFactoryFromModifiable extends AstFactory {
  private static final long serialVersionUID = 1L;

  static final DiagnosticType READ_ERROR = DiagnosticType.error(
      "OW_READ_ERROR", "Cannot read: {0}");

  private TimestampKeeper timestampKeeper;
  private String fileName;
  private JSUnitProvider.IProvider provider;

  public AstFactoryFromModifiable(String fileName, JSUnitProvider.IProvider provider) {
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
 
}
