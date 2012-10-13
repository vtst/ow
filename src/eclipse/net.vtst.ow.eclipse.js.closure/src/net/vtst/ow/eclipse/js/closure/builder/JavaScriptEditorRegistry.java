package net.vtst.ow.eclipse.js.closure.builder;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.texteditor.ITextEditor;

public class JavaScriptEditorRegistry extends AbstractEditorRegistry {

  private static String EDITOR_CLASS_NAME = "org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitEditor";
  
  private Set<IFile> newlyOpenedFiles = Collections.synchronizedSet(new HashSet<IFile>());

  public JavaScriptEditorRegistry(IWorkbench workbench) {
    super(workbench);
    this.addFileOpenListener(new AbstractEditorRegistry.IFileOpenListener() {
      @Override
      public void fileOpen(IFile file) {
        newlyOpenedFiles.add(file);
        try {
          file.getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);
        } catch (CoreException e) {
        }
      }
    });
  }
  
  public boolean isNewlyOpenedFile(IFile file) {
    return newlyOpenedFiles.remove(file);
  }

  protected boolean filterEditor(ITextEditor editorPart) {
    return EDITOR_CLASS_NAME.equals(editorPart.getClass().getName());
  }
  
  protected DocumentListener makeDocumentListener(IDocument document) {
    return new DocumentListener();
  }
}
