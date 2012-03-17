package net.vtst.ow.eclipse.js.closure.contentassist;

import java.util.Collections;

import net.vtst.ow.closure.compiler.compile.CompilableJSUnit;
import net.vtst.ow.closure.compiler.compile.CompilerRun;
import net.vtst.ow.closure.compiler.deps.JSSet;
import net.vtst.ow.closure.compiler.deps.JSUnit;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.wst.jsdt.ui.text.java.ContentAssistInvocationContext;

import com.google.javascript.jscomp.Scope;
import com.google.javascript.jscomp.Scope.Var;
import com.google.javascript.rhino.Node;

/**
 * Wrapper around {@code ContentAssistInvocationContext}.  The context distinguishes a path and
 * a prefix. For instance:
 * <pre>
 *   foo.bar.zo
 *   ^       ^ ^
 *   |       | Invocation offset
 *   |       Prefix offset   
 *   Path offset
 * </pre>
 * 
 * @author Vincent Simonet
 */
public class ClosureContentAssistIncovationContext implements IContentAssistInvocationContext {
  
  private ContentAssistInvocationContext context;

  /**
   * The offset of the first character of the path.
   */
  private int pathOffset;
  
  /**
   * The offset of the first character of the prefix.
   */
  private int prefixOffset;
  
  /**
   * The offset at which content assist is invoked.
   */
  private int invocationOffset;

  /**
   * @param context  The {@code ContentAssistInvocationContext} to wrap.
   */
  public ClosureContentAssistIncovationContext(ContentAssistInvocationContext context) {
    this.context = context;
    computePrefixAndPathOffsets();
  }
  
  // **************************************************************************
  // Access to properties of the included context
  
  /**
   * Returns the document that content assist is invoked on.
   * @return  The document, or null if unknown.
   */
  public IDocument getDocument() {
    return context.getDocument();
  }
  
  /**
   * @return  The invocation offset.
   */
  public int getInvocationOffset() {
    return context.getInvocationOffset();
  }

  /**
   * Returns the viewer that content assist is invoked in.
   * @return  The viewer, or null if unknown.
   */
  public ITextViewer getViewer() {
    return context.getViewer();
  }

  // **************************************************************************
  // Prefix and path

  /**
   * Compute the prefix already present in the document at the invocation offset.
   */
  private void computePrefixAndPathOffsets() {
    IDocument document = context.getDocument();
    invocationOffset = context.getInvocationOffset();
    try {
      prefixOffset = invocationOffset;
      while (prefixOffset > 0 && isCharForPrefix(document.getChar(prefixOffset - 1))) --prefixOffset;
      pathOffset = prefixOffset;
      while (pathOffset > 0 && isCharForPath(document.getChar(pathOffset - 1))) -- pathOffset;
    } catch (BadLocationException e) {
      assert false;
    }
  }

  /**
   * Test whether a char can be part of the prefix
   * @param c  The char to test.
   * @return  true if the char can be part of the prefix.
   */
  private boolean isCharForPrefix(char c) {
    return (
        c == '_' || 
        c >= 'a' && c <= 'z' ||
        c >= 'A' && c <= 'Z' ||
        c >= '0' && c <= '9');
  }
  
  private boolean isCharForPath(char c) {
    return (c == '.' || isCharForPrefix(c));
  }

  /**
   * Returns the length of the prefix.
   * @return  The length of the prefix.
   */
  public int getPrefixLength() {
    return (invocationOffset - prefixOffset);
  }
  
  /**
   * Returns the prefix.
   * @return  The prefix.
   */
  public String getPrefix() {
    try {
      return getDocument().get(getPrefixOffset(), getPrefixLength());
    } catch (BadLocationException e) {
      assert false;
      return null;
    }
  }
  
  public String[] getPath() {
    if (prefixOffset == pathOffset) return new String[]{};
    try {
      return getDocument().get(pathOffset, prefixOffset - pathOffset - 1).split("\\.");
    } catch (BadLocationException e) {
      assert false;
      return null;
    }
  }
  
  /**
   * Returns the offset of the first character of the prefix in the document.
   * @return
   */
  public int getPrefixOffset() {
    return prefixOffset;
  }

  // **************************************************************************
  // Compilation

  /**
   * Returns the file for the document that content assist is invoked on, as returned by the
   * editor registry.
   * @return  The file, or null if unknown.
   */
  private IFile getFile() {
    return OwJsClosurePlugin.getDefault().getEditorRegistry().getFile(getDocument());
  }
  
  private CompilerRun run = null;
  private Node node;
  
  private void lazyCompile() {
    if (run != null) return;
    IFile file = getFile();
    if (file == null) return;
    JSSet<IFile> compilationSet = OwJsClosurePlugin.getDefault().getProjectRegistry().getCompilationSet(file.getProject());
    if (compilationSet == null) return;
    JSUnit unit = compilationSet.getCompilationUnit(file);
    if (!(unit instanceof CompilableJSUnit)) return;
    run = ((CompilableJSUnit) unit).getLastAvailableCompilerRun();
    if (run == null) return;
    run.incrementalCompile();
    node = run.getNode(getPrefixOffset());
  }
  
  /**
   * Returns all symbols which are defined at the location where content assist is invoked.
   * @return  An iterable over all symbols, empty if the symbols cannot be computed for whatever reason.
   */
  public Iterable<Var> getAllSymbols() {
    lazyCompile();
    if (run == null || node == null) return Collections.emptyList();
    return run.getAllSymbols(node);
  }
  
  /**
   * Returns the scope at the location where content assist is invoked.
   * @return  The scope, or null if it cannot be retrieved for whatever reason.
   */
  public Scope getScope() {
    lazyCompile();
    if (run == null) return null;
    return run.getScope(node);
  }

}
