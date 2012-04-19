package net.vtst.ow.eclipse.js.closure.properties;

import net.vtst.eclipse.easy.ui.properties.editors.DefaultCompoundEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;

public class ClosureLinterChecksEditor extends DefaultCompoundEditor {

  private ClosureProjectPropertyRecord record = new ClosureProjectPropertyRecord();

  public ClosureLinterChecksEditor(IEditorContainer container) {
    super(container, 3);
    record.linterChecks.strictClosureStyle.bindEditor(this);
    record.linterChecks.missingJsdoc.bindEditor(this);
    record.linterChecks.customJsdocTags.bindEditor(this);
    record.linterChecks.lintErrorChecks.bindEditor(this);
    record.linterChecks.ignoreLintErrors.bindEditor(this);
  }

}
