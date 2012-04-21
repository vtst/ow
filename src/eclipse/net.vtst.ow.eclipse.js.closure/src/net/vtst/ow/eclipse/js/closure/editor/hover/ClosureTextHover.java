package net.vtst.ow.eclipse.js.closure.editor.hover;

import java.util.LinkedList;

import net.vtst.ow.closure.compiler.compile.CompilableJSUnit;
import net.vtst.ow.closure.compiler.compile.CompilerRun;
import net.vtst.ow.eclipse.js.closure.builder.ResourceProperties;
import net.vtst.ow.eclipse.js.closure.editor.ClosureJSElementInfo;
import net.vtst.ow.eclipse.js.closure.editor.contentassist.ClosureCompletionProposalCollector;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;

import com.google.javascript.jscomp.Scope;
import com.google.javascript.jscomp.Scope.Var;
import com.google.javascript.rhino.JSDocInfo;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;
import com.google.javascript.rhino.jstype.JSType;
import com.google.javascript.rhino.jstype.ObjectType;

// TODO: Update web page to mention the configuration to do
public class ClosureTextHover extends AbstractTextHover {

  private static String THIS = "this";

  /**
   * @return The file edited in the current editor.
   */
  private IFile getFile() {
    IEditorInput editorInput = getEditor().getEditorInput();
    if (!(editorInput instanceof IFileEditorInput)) return null;
    return ((IFileEditorInput) editorInput).getFile();
  }
  
  private CompilableJSUnit getJSUnit() {
    IFile file = getFile();
    if (file == null) return null;
    return ResourceProperties.getJSUnitOrNullIfCoreException(file);
  }
  
  private CompilerRun getCompilerRun(CompilableJSUnit unit) {
    CompilerRun run = unit.getLastAvailableCompilerRun();
    if (run != null) run.fastCompile();
    // TODO: Is this thread safe with the completion proposal computer to access further the run?
    run.fastCompile();
    return run;
  }
  
  protected LinkedList<String> getQualifiedName(Node node) {
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
  
  protected ClosureJSElementInfo getInfo(Node node, Scope scope) {
    LinkedList<String> qualifiedName = getQualifiedName(node);
    if (qualifiedName.isEmpty()) return null;
    String name = qualifiedName.removeFirst();
    // TODO: "this" to be implemented
    Var var = scope.getVar(name);
    if (var == null) return null;
    if (qualifiedName.isEmpty()) {
      return new ClosureJSElementInfo(var.getNameNode(), var.getJSDocInfo(), var.getType());
    } else {
      Node nameNode = var.getNameNode();
      JSType nameNodeType = nameNode.getJSType();
      String propertyName = qualifiedName.removeFirst();
      while (!qualifiedName.isEmpty()) {
        if (nameNode == null || !(nameNodeType instanceof ObjectType)) return null;
        ObjectType objectType = (ObjectType) nameNodeType;
        nameNode = objectType.getPropertyNode(propertyName);
        nameNodeType = objectType.getPropertyType(propertyName);
        propertyName = qualifiedName.removeFirst();
      }
      // TODO: Code to share with completion proposal
      // TODO: Add the name and kind as title in the completion proposal
      if (nameNode == null || !(nameNodeType instanceof ObjectType)) return null;
      ObjectType objectType = (ObjectType) nameNodeType;
      JSDocInfo docInfo = ClosureCompletionProposalCollector.getJSDocInfoOfProperty(objectType, propertyName);
      return new ClosureJSElementInfo(objectType.getPropertyNode(propertyName), docInfo, objectType.getPropertyType(propertyName));
    }
  }
  
  @Override
  protected String getHoverHTML(ITextViewer viewer, IRegion region) {
    CompilableJSUnit unit = getJSUnit();
    if (unit == null) return null;
    CompilerRun run = getCompilerRun(unit);
    if (run == null) return null;
    Node node = run.getNode(unit, region.getOffset());
    Scope scope = run.getScope(node);
    if (node == null || scope == null) return null;
    ClosureJSElementInfo info = getInfo(node, scope);
    if (info == null) return null;
    return info.getHTMLStringForHover();
  }
}
