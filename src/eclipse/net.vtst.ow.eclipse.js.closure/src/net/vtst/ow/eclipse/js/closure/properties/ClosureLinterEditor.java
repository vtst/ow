package net.vtst.ow.eclipse.js.closure.properties;

import net.vtst.eclipse.easy.ui.properties.editors.DefaultCompoundEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.properties.fields.BooleanField;
import net.vtst.eclipse.easy.ui.properties.fields.FlagListField;
import net.vtst.eclipse.easy.ui.properties.fields.StringField;
import net.vtst.eclipse.easy.ui.util.SWTFactory;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Group;

public class ClosureLinterEditor extends DefaultCompoundEditor {

  private ClosureProjectPropertyRecord record = new ClosureProjectPropertyRecord();

  public ClosureLinterEditor(IEditorContainer container) {
    super(container, 3);
    record.linter.strictClosureStyle.bindEditor(this);
    record.linter.missingJsdoc.bindEditor(this);
    record.linter.customJsdocTags.bindEditor(this);
    record.linter.lintErrorChecks.bindEditor(this);
    record.linter.ignoreLintErrors.bindEditor(this);
  }

}
