package net.vtst.ow.eclipse.js.closure.editor.hover;

import java.util.LinkedList;

import net.vtst.ow.closure.compiler.compile.CompilableJSUnit;
import net.vtst.ow.closure.compiler.compile.CompilerRun;
import net.vtst.ow.eclipse.js.closure.builder.ResourceProperties;
import net.vtst.ow.eclipse.js.closure.editor.JSElementInfo;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;

import com.google.javascript.jscomp.Scope;
import com.google.javascript.jscomp.Scope.Var;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;
import com.google.javascript.rhino.jstype.JSType;

// TODO: Update web page to mention the configuration to do
public class ClosureTextHover extends AbstractTextHover {

  private static String THIS = "this";

  /**
   * @return The file edited in the current editor, or null.
   */
  private IFile getFile() {
    IEditorInput editorInput = getEditor().getEditorInput();
    if (!(editorInput instanceof IFileEditorInput)) return null;
    return ((IFileEditorInput) editorInput).getFile();
  }
  
  /**
   * @return The JavaScript unit edited in the current editor, or null.
   */
  private CompilableJSUnit getJSUnit() {
    IFile file = getFile();
    if (file == null) return null;
    return ResourceProperties.getJSUnitOrNullIfCoreException(file);
  }
  
  /**
   * Get the compiler run for a JavaScript unit, and update it.
   * @param unit  The JavaScript unit.
   * @return  The compiler run, or null.
   */
  private CompilerRun getCompilerRun(CompilableJSUnit unit) {
    CompilerRun run = unit.getLastAvailableCompilerRun();
    if (run != null) run.fastCompile();
    // TODO: Is this thread safe with the completion proposal computer to access further the run?
    return run;
  }
  
  /**
   * Get the qualified name for a node of a JavaScript AST.
   * @param node  The node.
   * @return  The qualified name (never null, but may be empty).
   */
  private LinkedList<String> getQualifiedName(Node node) {
    LinkedList<String> result = new LinkedList<String>();
    while (true) {
      switch (node.getType()) {
      case Token.STRING:
        node = node.getParent();
        break;
      case Token.NAME:
        result.addFirst(node.getString());
        return result;
      case Token.THIS:
        result.addFirst(THIS);
        return result;
      case Token.GETPROP:
        result.addFirst(node.getLastChild().getString());
        node = node.getFirstChild();
        break;
      default:
        return new LinkedList<String>();
      }
    }
  }
  
  /**
   * @return  The element info for a given node, or null.
   */
  private JSElementInfo getElementInfo(CompilerRun run, Node node) {
    Scope scope = run.getScope(node);
    if (scope == null) return null;
    LinkedList<String> qualifiedName = getQualifiedName(node);
    if (qualifiedName.isEmpty()) return null;
    String propertyName = qualifiedName.removeLast();
    if (qualifiedName.isEmpty()) {
      // This is a top level node
      Var var = scope.getVar(propertyName);
      if (var == null) return null;
      else return JSElementInfo.makeFromVar(run, var);
    } else {
      // This is a property
      JSType type = CompilerRun.getTypeOfQualifiedName(scope, qualifiedName);
      return JSElementInfo.makeFromPropertyOrNull(run, type, propertyName);
    }
  }
  
  /* (non-Javadoc)
   * @see net.vtst.ow.eclipse.js.closure.editor.hover.AbstractTextHover#getHoverHTML(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
   */
  @Override
  protected String getHoverHTML(ITextViewer viewer, IRegion region) {
    CompilableJSUnit unit = getJSUnit();
    if (unit == null) return null;
    CompilerRun run = getCompilerRun(unit);
    if (run == null) return null;
    Node node = run.getNode(unit, region.getOffset());
    if (node == null) return null;
    JSElementInfo info = getElementInfo(run, node);
    if (info == null) return null;
    return info.getHTMLStringForHover();
  }
}
