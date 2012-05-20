package net.vtst.ow.eclipse.js.closure.properties.project;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Label;

import net.vtst.eclipse.easy.ui.properties.editors.DefaultCompoundEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.util.SWTFactory;

public class ClosureMainConfigurationEditor extends DefaultCompoundEditor {
  
  ClosureProjectPropertyRecord record = new ClosureProjectPropertyRecord();
  
  public ClosureMainConfigurationEditor(IEditorContainer container) {
    super(container, 1);
    Label horizontalLine = new Label(getComposite(), SWT.SEPARATOR | SWT.HORIZONTAL);
    SWTFactory.createVerticalSpacer(getComposite(), 1);
    horizontalLine.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
    horizontalLine.setFont(getComposite().getFont());
    SWTFactory.createWrapLabel(getComposite(), getMessage("help"), 1, 10);
  }
}
