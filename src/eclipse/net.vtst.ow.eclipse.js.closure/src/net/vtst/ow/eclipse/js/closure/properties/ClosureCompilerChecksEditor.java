package net.vtst.ow.eclipse.js.closure.properties;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Group;

import net.vtst.eclipse.easy.ui.properties.editors.DefaultCompoundEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.util.SWTFactory;

public class ClosureCompilerChecksEditor extends DefaultCompoundEditor {

  private ClosureProjectPropertyRecord record = new ClosureProjectPropertyRecord();

  public ClosureCompilerChecksEditor(IEditorContainer container) {
    super(container, 1);
    Group group1 = SWTFactory.createGroup(
        getComposite(), getMessage("warningAndChecksGroup"), 3, 1, GridData.FILL_HORIZONTAL);
    addControl(group1);
    record.warningLevel.bindEditor(this, group1);
    record.checkLevels.bindEditor(this, group1);
    
    Group group2 = SWTFactory.createGroup(
        getComposite(), getMessage("inputLanguageGroup"), 3, 1, GridData.FILL_HORIZONTAL);
    addControl(group1);
    record.languageIn.bindEditor(this, group2);
    record.thirdParty.bindEditor(this, group2);
    record.processClosurePrimitives.bindEditor(this, group2);
    record.processJQueryPrimitives.bindEditor(this, group2);
    record.acceptConstKeyword.bindEditor(this, group2);
    // TODO Remove
    // record.closureEntryPoints.bindEditor(this);
  }

}
