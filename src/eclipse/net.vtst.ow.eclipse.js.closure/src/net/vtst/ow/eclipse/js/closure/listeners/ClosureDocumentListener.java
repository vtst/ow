package net.vtst.ow.eclipse.js.closure.listeners;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Listener that monitors documents opened in editors for changes.
 * @author Vincent Simonet
 */
public class ClosureDocumentListener implements IDocumentListener {

  @Override
  public void documentAboutToBeChanged(DocumentEvent event) {}

  @Override
  public void documentChanged(DocumentEvent event) {
    System.out.println("DOCUMENT CHANGED");
  }

  // **************************************************************************
  // Static methods
  
  private static ClosureDocumentListener instance = new ClosureDocumentListener();
  
  public static ClosureDocumentListener get() {
    return instance;
  }
  
  public static void addTo(IDocument document) {
    document.addDocumentListener(get());
  }
  
  public static void addTo(IWorkbench workbench) {
    for (IWorkbenchWindow window: workbench.getWorkbenchWindows()) {
      for (IWorkbenchPage page: window.getPages()) {
        for (IEditorReference editorReference: page.getEditorReferences()) {
          addToIfApplicable(editorReference);
        }
      }
    }
  }
  
  private static String EDITOR_CLASS_NAME = "org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitEditor";

  public static IDocument getDocumentOfPart(IWorkbenchPartReference partReference) {
    if (!(partReference instanceof IEditorReference));
    IEditorPart editorPart = ((IEditorReference) partReference).getEditor(false);
    if (!(editorPart instanceof ITextEditor)) return null;
    ITextEditor textEditor = (ITextEditor) editorPart;
    if (!EDITOR_CLASS_NAME.equals(textEditor.getClass().getName())) return null;
    IDocumentProvider docProvider = textEditor.getDocumentProvider();
    if (docProvider == null) return null;
    return docProvider.getDocument(textEditor.getEditorInput());
  }
  
  public static void addToIfApplicable(IWorkbenchPartReference partReference) {
    IDocument document = getDocumentOfPart(partReference);
    if (document != null) addTo(document);
  }
  
  public static void removeFrom(IDocument document) {
    document.removeDocumentListener(get());
  }

  public static void removeFrom(IWorkbenchPartReference partReference) {
    IDocument document = getDocumentOfPart(partReference);
    if (document != null) removeFrom(document);    
  }

}
