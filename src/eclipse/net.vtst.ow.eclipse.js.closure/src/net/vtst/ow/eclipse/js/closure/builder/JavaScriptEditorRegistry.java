package net.vtst.ow.eclipse.js.closure.builder;

import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.texteditor.ITextEditor;

public class JavaScriptEditorRegistry extends AbstractEditorRegistry {

  private static String EDITOR_CLASS_NAME = "org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitEditor";

  public JavaScriptEditorRegistry(IWorkbench workbench) {
    super(workbench);
  }

  protected boolean filterEditor(ITextEditor editorPart) {
    return EDITOR_CLASS_NAME.equals(editorPart.getClass().getName());
  }
  
  protected DocumentListener makeDocumentListener(IDocument document) {
    return new DocumentListener();
  }
}
