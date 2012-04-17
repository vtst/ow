package net.vtst.ow.eclipse.js.closure.properties;

import net.vtst.eclipse.easy.ui.properties.editors.DefaultCompoundEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.util.SWTFactory;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Group;

public class ClosureCompilerChecksEditor extends DefaultCompoundEditor {

  private ClosureProjectPropertyRecord record = new ClosureProjectPropertyRecord();

  public ClosureCompilerChecksEditor(IEditorContainer container) {
    super(container, 1);
    Group group1 = SWTFactory.createGroup(
        getComposite(), getMessage("warningAndChecksGroup"), 3, 1, GridData.FILL_HORIZONTAL);
    addControl(group1);
    record.checks.warningLevel.bindEditor(this, group1);
    record.checks.checkLevels.bindEditor(this, group1);
    
    Group group2 = SWTFactory.createGroup(
        getComposite(), getMessage("inputLanguageGroup"), 3, 1, GridData.FILL_HORIZONTAL);
    addControl(group2);
    record.checks.languageIn.bindEditor(this, group2);
    record.checks.thirdParty.bindEditor(this, group2);
    record.checks.processClosurePrimitives.bindEditor(this, group2);
    record.checks.processJQueryPrimitives.bindEditor(this, group2);
    record.checks.acceptConstKeyword.bindEditor(this, group2);
  }

}
