package net.vtst.ow.eclipse.js.closure.launching.linter;

import net.vtst.eclipse.easy.ui.properties.editors.DefaultCompoundEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;

public class ClosureLinterOptionsEditor extends DefaultCompoundEditor {
  
  private ClosureLinterLaunchConfigurationRecord record = new ClosureLinterLaunchConfigurationRecord(); 

  public ClosureLinterOptionsEditor(IEditorContainer container) {
    super(container, 3);
    record.inputResources.bindEditor(this);
    record.gjslintCommand.bindEditor(this);
    record.fixjsstyleCommand.bindEditor(this);
    record.fixLintErrors.bindEditor(this);
    record.useAsDefault.bindEditor(this);
  }
  
}
