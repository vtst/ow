package net.vtst.ow.eclipse.js.closure.properties;

import net.vtst.eclipse.easy.ui.properties.editors.DefaultCompoundEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;

public class ClosureCompilerChecksEditor extends DefaultCompoundEditor {

  private ClosureProjectPropertyRecord record = new ClosureProjectPropertyRecord();

  public ClosureCompilerChecksEditor(IEditorContainer container) {
    super(container, 3);
    // TODO First group
    record.warningLevel.bindEditor(this);
    record.checkLevels.bindEditor(this);
    // TODO Second group
    record.thirdParty.bindEditor(this);
    record.processClosurePrimitives.bindEditor(this);
    record.processJQueryPrimitives.bindEditor(this);
    record.acceptConstKeyword.bindEditor(this);
    record.languageIn.bindEditor(this);
    // TODO Third group
    record.closureEntryPoints.bindEditor(this);
  }

}
