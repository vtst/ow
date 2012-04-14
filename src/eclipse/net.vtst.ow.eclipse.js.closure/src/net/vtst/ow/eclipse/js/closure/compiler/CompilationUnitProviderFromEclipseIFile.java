package net.vtst.ow.eclipse.js.closure.compiler;

import java.io.IOException;
import java.io.InputStreamReader;

import net.vtst.ow.closure.compiler.deps.JSUnitProvider;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.ITextEditor;

public class CompilationUnitProviderFromEclipseIFile implements JSUnitProvider.IProvider {
  
  private IFile file;
  private String code;

  public CompilationUnitProviderFromEclipseIFile(IFile file) {
    this.file = file;
  }

  @Override
  public long lastModified() {
    JavaScriptEditorRegistry editorRegistry = OwJsClosurePlugin.getDefault().getEditorRegistry(); 
    IDocument document = editorRegistry.getDocument(file);
    if (document == null) 
      return file.getModificationStamp();
    else {
      // TODO: If this is not in the same thread as the registry, we should be careful
      // that the editor and its document may have been deleted in the meantime!
      // Be also careful to the listener.
      return editorRegistry.getLastModificationTime(document);
    }
  }

  @Override
  public void prepareToGetCode() throws IOException {
    ITextEditor textEditor = OwJsClosurePlugin.getDefault().getEditorRegistry().getTextEditor(file);
    if (textEditor == null) prepareToGetCodeFromFile();
    else prepareToGetCodeFromEditor(textEditor);
   }
  
  private void prepareToGetCodeFromFile() throws IOException {
    try {
      char[] cbuf = new char[1024];
      InputStreamReader reader = new InputStreamReader(file.getContents());
      StringBuffer buffer = new StringBuffer();
      while (true) {
        int n = reader.read(cbuf);
        if (n <= 0) break;
        buffer.append(cbuf, 0, n);
      }
      code = buffer.toString();
    } catch (CoreException e) {
      code = "";
      throw new IOException(e);
    }    
  }
  
  private void prepareToGetCodeFromEditor(ITextEditor textEditor) {
    code = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput()).get();
  }

  @Override
  public String getCode() {
    return code;
  }

}
