package net.vtst.eclipse.easy.ui.properties.pages;

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
    setControl(editor.getComposite());
  }

  @Override
  public void initializeFrom(ILaunchConfiguration config) {
    try {
      editor.readValuesFrom(new LaunchConfigurationReadOnlyStore(config));
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
    if (editor != null) {
      editor.setValuesToDefault();
    }
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
   */
  @Override
  public boolean isValid(ILaunchConfiguration config) {
    if (editor == null) return true;
    return editor.isValid();
  }

  public String getErrorMessage() {
    if (editor == null) return null;
    return editor.getErrorMessage();
  }

  @Override
  public void addEditor(IEditor editor) {}

  @Override
  public void editorChanged(IEditorChangeEvent event) {
    updateLaunchConfigurationDialog();    
  }

  @Override
  public Composite getComposite() {
    return parent;
  }

}
