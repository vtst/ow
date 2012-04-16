package net.vtst.ow.eclipse.js.closure.launching.compiler;

import net.vtst.eclipse.easy.ui.properties.editors.DefaultCompoundEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.util.SWTFactory;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Group;

public class ClosureCompilerInputsAndOutputsEditor extends DefaultCompoundEditor {
  
  private ClosureCompilerLaunchConfigurationRecord record = new ClosureCompilerLaunchConfigurationRecord(); 

  public ClosureCompilerInputsAndOutputsEditor(IEditorContainer container) {
    super(container, 3);
    record.inputResources.bindEditor(this);
  }

}
