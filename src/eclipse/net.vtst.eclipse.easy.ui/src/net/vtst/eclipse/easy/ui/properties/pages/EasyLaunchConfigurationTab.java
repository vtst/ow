package net.vtst.eclipse.easy.ui.properties.pages;

import net.vtst.eclipse.easy.ui.properties.editors.AllEditorChangeEvent;
import net.vtst.eclipse.easy.ui.properties.editors.ICompositeEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorChangeEvent;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.properties.stores.LaunchConfigurationReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.LaunchConfigurationStore;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.widgets.Composite;

/**
 * A launch configuration tab, implemented by a {@code ICompositeEditor}.
 * @author Vincent Simonet
 */
public abstract class EasyLaunchConfigurationTab extends AbstractLaunchConfigurationTab implements IEditorContainer {

  private ICompositeEditor editor;
  private Composite parent;

  protected abstract ICompositeEditor createEditor();
  
  @Override
  public void createControl(Composite parent) {
    this.parent = parent;
    editor = createEditor();
  }

  @Override
  public void initializeFrom(ILaunchConfiguration config) {
    try {
      editor.readValuesFrom(new LaunchConfigurationReadOnlyStore(config));
      editorChanged(new AllEditorChangeEvent());
    } catch (CoreException e) {}
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy config) {
    try {
      editor.writeValuesTo(new LaunchConfigurationStore(config));
    } catch (CoreException e) {}
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy config) {
    editor.setValuesToDefault();
    editorChanged(new AllEditorChangeEvent());
  }

  @Override
  public void addEditor(IEditor editor) {}

  @Override
  public void editorChanged(IEditorChangeEvent event) {}

  @Override
  public Composite getComposite() {
    return parent;
  }

}
