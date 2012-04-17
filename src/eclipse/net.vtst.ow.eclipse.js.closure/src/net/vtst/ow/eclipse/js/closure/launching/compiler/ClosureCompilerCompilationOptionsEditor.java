package net.vtst.ow.eclipse.js.closure.launching.compiler;

import java.util.List;

import net.vtst.eclipse.easy.ui.properties.editors.DefaultCompoundEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.util.SWTFactory;
import net.vtst.ow.eclipse.js.closure.OwJsClosureMessages;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Group;
import org.eclipse.wst.jsdt.ui.Messages;

public class ClosureCompilerCompilationOptionsEditor extends DefaultCompoundEditor {
  
  private ClosureCompilerLaunchConfigurationRecord record = new ClosureCompilerLaunchConfigurationRecord(); 

  public ClosureCompilerCompilationOptionsEditor(IEditorContainer container) {
    super(container, 3);
    record.compilationLevel.bindEditor(this);
    record.generateExports.bindEditor(this);
    Group group1 = SWTFactory.createGroup(
        getComposite(), getMessage("formatting"), 3, 3, GridData.FILL_HORIZONTAL);
    addControl(group1);
    record.formattingPrettyPrint.bindEditor(this, group1);
    record.formattingPrintInputDelimiter.bindEditor(this, group1);
  }
  
}
