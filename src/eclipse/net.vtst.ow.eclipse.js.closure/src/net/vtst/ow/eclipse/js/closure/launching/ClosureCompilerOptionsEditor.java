package net.vtst.ow.eclipse.js.closure.launching;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Group;

import net.vtst.eclipse.easy.ui.properties.editors.DefaultCompoundEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.util.SWTFactory;

public class ClosureCompilerOptionsEditor extends DefaultCompoundEditor {
  
  private ClosureCompilerLaunchConfigurationRecord record = new ClosureCompilerLaunchConfigurationRecord(); 

  public ClosureCompilerOptionsEditor(IEditorContainer container) {
    super(container, 3);
    record.compilationLevel.bindEditor(this);
    record.generateExports.bindEditor(this);
    Group group1 = SWTFactory.createGroup(
        getComposite(), getMessage("formatting"), 3, 3, GridData.FILL_HORIZONTAL);
    record.formattingPrettyPrint.bindEditor(this, group1);
    record.formattingPrintInputDelimiter.bindEditor(this, group1);
    record.inputResources.bindEditor(this);
  }

}
