package net.vtst.ow.eclipse.js.closure.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.vtst.ow.closure.compiler.util.MultiHashMap;
import net.vtst.ow.eclipse.js.closure.util.Utils;
import net.vtst.ow.eclipse.js.closure.util.listeners.NullDocumentListener;
import net.vtst.ow.eclipse.js.closure.util.listeners.NullPartListener2;
import net.vtst.ow.eclipse.js.closure.util.listeners.NullWindowListener;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * A registry that monitors opening and closures of editors in order to:
 * 1. Add a document listener to editors.
 * 2. Provide a mapping from files to editors.
 * This class is implemented to be thread safe.
 * @author Vincent Simonet
 */
public abstract class AbstractEditorRegistry {

  // **************************************************************************
  // Abstract methods to be implemented in sub-classes

  /**
   * Filter for selecting editor which shall be handled by the registry.
   * @param editorPart  The editor to check.
   * @return  true if the editor shall be handled.
   */
  protected abstract boolean filterEditor(ITextEditor editorPart);
  
  /**
   * Create the document listener for a document.
   * @param document  The document to create the listener for.
   * @return  The document listener.
   */
  protected abstract DocumentListener makeDocumentListener(IDocument document);

  // **************************************************************************
  // Constructor

  private IWorkbench workbench;
  
  /**
   * Creates a new registry for a workbench.  Fill the registry with the
   * currently opened editor, and install the listeners.
   * @param workbench
   */
  public AbstractEditorRegistry(IWorkbench workbench) {
    this.workbench = workbench;
    workbench.addWindowListener(windowListener);
    for (IWorkbenchWindow window: workbench.getWorkbenchWindows()) {
      window.getPartService().addPartListener(partListener);
      for (IWorkbenchPage page: window.getPages()) {
        for (IEditorReference editorReference: page.getEditorReferences()) {
          addPartReference(editorReference);
        }
      }
    }
  }
  
  public void dispose () {
    workbench.removeWindowListener(windowListener);
    for (IWorkbenchWindow window: workbench.getWorkbenchWindows()) {
      window.getPartService().removePartListener(partListener);
      for (IWorkbenchPage page: window.getPages()) {
        for (IEditorReference editorReference: page.getEditorReferences()) {
          removePartReference(editorReference);
        }
      }
    }
  }
  
  // **************************************************************************
  // The registry

  private MultiHashMap<IFile, ITextEditor> fileToEditors = new MultiHashMap<IFile, ITextEditor>();  
  private MultiHashMap<IDocument, ITextEditor> documentToEditors = new MultiHashMap<IDocument, ITextEditor>();  
  private Map<ITextEditor, IFile> editorToFile = new HashMap<ITextEditor, IFile>();
  private Map<ITextEditor, IDocument> editorToDocument = new HashMap<ITextEditor, IDocument>();
  private Map<IDocument, DocumentListener> documentToListener = new HashMap<IDocument, DocumentListener>();
  private Map<IDocument, IFile> documentToFile = new HashMap<IDocument, IFile>();
  
  private synchronized void addPartReference(IWorkbenchPartReference partReference) {
    ITextEditor textEditor = getTextEditorFromPartReference(partReference);
    if (textEditor != null && filterEditor(textEditor)) addEditor(textEditor);
  }
  
  private synchronized void addEditor(ITextEditor textEditor) {
    IEditorInput editorInput = textEditor.getEditorInput();
    if (!(editorInput instanceof IFileEditorInput)) return;
    IFile file = ((IFileEditorInput) editorInput).getFile();
    IDocument document = textEditor.getDocumentProvider().getDocument(editorInput);
    if (file == null || document == null) return;
    editorToFile.put(textEditor, file);
    editorToDocument.put(textEditor, document);
    boolean newlyOpenedFile = fileToEditors.put(file, textEditor);
    if (documentToEditors.put(document, textEditor)) {
      DocumentListener listener = makeDocumentListener(document);
      documentToListener.put(document, listener);
      document.addDocumentListener(listener);
      documentToFile.put(document, file);
    }
    if (newlyOpenedFile) {
      triggerFileOpenListener(file);
    }
  }

  private synchronized void removePartReference(IWorkbenchPartReference partReference) {
    ITextEditor textEditor = getTextEditorFromPartReference(partReference);
    if (textEditor != null && filterEditor(textEditor)) removeEditor(textEditor);
  }
  
  private synchronized void removeEditor(ITextEditor textEditor) {
    IFile file = editorToFile.get(textEditor);
    if (file != null) fileToEditors.remove(file, textEditor);
    IDocument document = editorToDocument.get(textEditor);
    if (document != null) {
      if (documentToEditors.remove(document, textEditor)) {
        IDocumentListener listener = documentToListener.remove(document);
        if (listener != null) document.removeDocumentListener(listener);
        documentToFile.remove(document);
      }
    }
  }
  
  public static ITextEditor getTextEditorFromPartReference(IWorkbenchPartReference partReference) {
    if (!(partReference instanceof IEditorReference)) return null;
    IEditorPart editorPart = ((IEditorReference) partReference).getEditor(false);
    if (!(editorPart instanceof ITextEditor)) return null;
    return (ITextEditor) editorPart;
  }
  
  public synchronized ITextEditor getTextEditor(IFile file) {
    return Utils.getFirstElement(fileToEditors.get(file));
  }
  
  public synchronized IDocument getDocument(IFile file) {
    ITextEditor textEditor = Utils.getFirstElement(fileToEditors.get(file));
    if (textEditor == null) return null;
    return textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
  }
  
  public synchronized IFile getFile(IDocument document) {
    return documentToFile.get(document);
  }
  
  public synchronized long getLastModificationTime(IDocument document) {
    DocumentListener listener = documentToListener.get(document);
    return listener.getLastModificationTime();
  }
  
  // **************************************************************************
  // Part listener
  
  private IPartListener2 partListener = new PartListener();
  
  private class PartListener extends NullPartListener2 {

    @Override
    public void partClosed(IWorkbenchPartReference partReference) {
      removePartReference(partReference);
    }

    @Override
    public void partInputChanged(IWorkbenchPartReference partReference) {
      removePartReference(partReference);
      addPartReference(partReference);
    }

    @Override
    public void partOpened(IWorkbenchPartReference partReference) {
      addPartReference(partReference);
    }
    
  }

  // **************************************************************************
  // Window listener

  private IWindowListener windowListener = new WindowListener();
  
  private class WindowListener extends NullWindowListener {

    @Override
    public void windowClosed(IWorkbenchWindow window) {
      window.getPartService().removePartListener(partListener);      
    }

    @Override
    public void windowOpened(IWorkbenchWindow window) {
      window.getPartService().addPartListener(partListener);
    }
    
  }
  
  // **************************************************************************
  // Document listener
  
  /**
   * Document listener that manages a last modification time for the document.
   * For the sake of efficiency, the change event just set a boolean flag.  The timestamp
   * is computed when the last modification time is requested.  Hence, the last modification
   * @author Vincent Simonet
   */
  protected class DocumentListener extends NullDocumentListener {
    private long lastModificationTime = System.currentTimeMillis();
    private boolean notmodified = true;
    
    public void documentChanged(DocumentEvent event) {
      if (notmodified) notmodified = false;
    }
    
    public long getLastModificationTime() {
      if (!notmodified) {
        notmodified = true;
        lastModificationTime = System.currentTimeMillis();
      }
      return lastModificationTime;
    }
  }
  
  // **************************************************************************
  // FileOpenListener
  
  public interface IFileOpenListener {
    public void fileOpen(IFile file);
  }
  
  List<IFileOpenListener> fileOpenListeners = new ArrayList<IFileOpenListener>();
  
  public void addFileOpenListener(IFileOpenListener listener) {
    fileOpenListeners.add(listener);
  }
  
  private void triggerFileOpenListener(IFile file) {
    for (IFileOpenListener listener : fileOpenListeners) {
      listener.fileOpen(file);
    }
  }
}
