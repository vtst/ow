package net.vtst.eclipse.easy.ui.properties.pages;

import net.vtst.eclipse.easy.ui.properties.editors.AllEditorChangeEvent;
import net.vtst.eclipse.easy.ui.properties.editors.ICompositeEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorChangeEvent;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.properties.stores.IStore;
import net.vtst.eclipse.easy.ui.properties.stores.PluginPreferenceStore;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * A preference page, implemented by a {@code ICompositeEditor}.
 * @author Vincent Simonet
 */
public abstract class EasyPreferencePage extends PreferencePage implements IWorkbenchPreferencePage, IEditorContainer {

  private ICompositeEditor editor;
  private Composite parent;

  /**
   * @return  The editor for the page.
   */
  protected abstract ICompositeEditor createEditor();
    
  @Override
  protected Control createContents(Composite parent) {
    this.parent = parent;
    editor = createEditor();
    try {
      editor.readValuesFrom(getStore());
      editorChanged(new AllEditorChangeEvent());
    } catch (CoreException e) {}
    return editor.getComposite();
  }

  @Override
  public void init(IWorkbench workbench) {}
  
  @Override
  protected void performDefaults() {
    editor.setValuesToDefault();
    editorChanged(new AllEditorChangeEvent());
    super.performDefaults();
  }
  
  private IStore getStore() {
    return new PluginPreferenceStore(getPreferenceStore());
  }

  public boolean performOk() {
    try {
      editor.writeValuesTo(getStore());
    } catch (CoreException e) {
      return false;
    }
    return super.performOk();
  }
  
  @Override
  public void addEditor(IEditor editor) {}

  @Override
  public void editorChanged(IEditorChangeEvent event) {}

  @Override
  public Composite getComposite() {
    return parent;
  }

  @Override
  public int getColumnCount() {
    return 0;
  }
}
