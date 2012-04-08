package net.vtst.ow.eclipse.js.closure.properties;

import org.eclipse.core.runtime.CoreException;

import net.vtst.eclipse.easy.ui.properties.editors.DefaultCompoundEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorChangeEvent;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.properties.stores.IReadOnlyStore;
import net.vtst.eclipse.easy.ui.util.SWTFactory;

public class ClosureMainConfigurationEditor extends DefaultCompoundEditor {
  
  private ClosureProjectPropertyRecord record = new ClosureProjectPropertyRecord();
  
  public ClosureMainConfigurationEditor(IEditorContainer container) {
    super(container, 3);
    record.closureBasePath.bindEditor(this);
    record.otherLibraries.bindEditor(this);
    SWTFactory.createLabel(container.getComposite(), getMessage("help"), container.getColumnCount());
  }

  @Override
  public void readValuesFrom(IReadOnlyStore store) throws CoreException {
    super.readValuesFrom(store);
  }
  
  @Override
  public void editorChanged(IEditorChangeEvent event) {}

}
