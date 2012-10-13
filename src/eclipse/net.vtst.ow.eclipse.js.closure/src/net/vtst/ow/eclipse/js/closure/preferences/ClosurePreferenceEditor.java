package net.vtst.ow.eclipse.js.closure.preferences;

import net.vtst.eclipse.easy.ui.properties.editors.DefaultCompoundEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.util.SWTFactory;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Group;

public class ClosurePreferenceEditor extends DefaultCompoundEditor {

  ClosurePreferenceRecord record = new ClosurePreferenceRecord();

  public ClosurePreferenceEditor(IEditorContainer container) {
    super(container, 1);
    Group group0 = SWTFactory.createGroup(
        getComposite(), getMessage("globalGroup"), 3, 1, GridData.FILL_HORIZONTAL);
    record.closureBasePath.bindEditor(this, group0);
    Group group1 = SWTFactory.createGroup(
        getComposite(), getMessage("libraryCacheGroup"), 3, 1, GridData.FILL_HORIZONTAL);
    SWTFactory.createLabel(group1, getMessage("libraryCacheGroup_help"), 3);
    record.cacheLibraryDepsFiles.bindEditor(this, group1);
    record.cacheLibraryStrippedFiles.bindEditor(this, group1);
    Group group2 = SWTFactory.createGroup(getComposite(), getMessage("otherOptionsGroup"), 3, 1, GridData.FILL_HORIZONTAL);
    record.stripProjectFiles.bindEditor(this, group2);
    record.doNotKeepCompilationResultsOfClosedFilesInMemory.bindEditor(this, group2);
    record.doNotCompileClosedFiles.bindEditor(this, group2);
  }
  
}
