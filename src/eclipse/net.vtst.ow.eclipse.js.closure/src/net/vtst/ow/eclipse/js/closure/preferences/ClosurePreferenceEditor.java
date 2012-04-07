package net.vtst.ow.eclipse.js.closure.preferences;

import net.vtst.eclipse.easy.ui.properties.editors.DefaultCompoundEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;

public class ClosurePreferenceEditor extends DefaultCompoundEditor {

  ClosurePreferenceRecord record = new ClosurePreferenceRecord();

  public ClosurePreferenceEditor(IEditorContainer container) {
    super(container, 3);
    record.closureBasePath.bindEditor(this);
    record.readStrippedLibraryFiles.bindEditor(this);
    record.writeStrippedLibraryFiles.bindEditor(this);
  }

}
