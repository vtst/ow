package net.vtst.ow.eclipse.js.closure.preferences;

import net.vtst.eclipse.easy.ui.properties.editors.DefaultCompoundEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorChangeEvent;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;

public class ClosurePreferenceEditor extends DefaultCompoundEditor {

  ClosurePreferenceRecord record = new ClosurePreferenceRecord();

  public ClosurePreferenceEditor(IEditorContainer container) {
    super(container, 3);
    record.closureBasePath.bindEditor(this);
    record.readStrippedLibraryFiles.bindEditor(this);
    record.writeStrippedLibraryFiles.bindEditor(this);
  }
  
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    if (!record.readStrippedLibraryFiles.editor().getCurrentValue())
      record.writeStrippedLibraryFiles.editor().setEnabled(false);
  }
  
  public void editorChanged(IEditorChangeEvent event) {
    triggerChangeEvent(event);
    record.writeStrippedLibraryFiles.editor().setEnabled(record.readStrippedLibraryFiles.editor().getCurrentValue());
  }

}
