package net.vtst.ow.eclipse.js.closure.properties;

import net.vtst.eclipse.easy.ui.properties.editors.DefaultCompoundEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.util.SWTFactory;

public class ClosureIncludesEditor extends DefaultCompoundEditor {

  private ClosureProjectPropertyRecord record = new ClosureProjectPropertyRecord();

  public ClosureIncludesEditor(IEditorContainer container) {
    super(container, 3);
    record.otherLibraries.bindEditor(this);
    SWTFactory.createLabel(container.getComposite(), getMessage("otherLibrariesHelp"), 3);
    record.externs.bindEditor(this);
    record.useOnlyCustomExterns.bindEditor(this);
  }

}
