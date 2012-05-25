package net.vtst.eclipse.easy.ui.properties.pages;

import net.vtst.eclipse.easy.ui.properties.editors.AllEditorChangeEvent;
import net.vtst.eclipse.easy.ui.properties.editors.ICompositeEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorChangeEvent;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.properties.stores.IStore;
import net.vtst.eclipse.easy.ui.properties.stores.NullStore;
import net.vtst.eclipse.easy.ui.properties.stores.ResourcePropertyStore;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * A resource property page, implemented by a {@code ICompositeEditor}.
 * @author Vincent Simonet
 */
public abstract class EasyResourcePropertyPage extends PropertyPage implements IEditorContainer {

  private ICompositeEditor editor;
  private Composite parent;
  
  public EasyResourcePropertyPage() {
    super();
  }

  /**
   * @return  The editor for the page.
   */
  protected abstract ICompositeEditor createEditor();
  
  /**
   * @return  The qualifier for property names.
   */
  protected abstract String getPropertyQualifier();
  
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
  public void addEditor(IEditor editor) {}

  @Override
  public void editorChanged(IEditorChangeEvent event) {
    this.setErrorMessage(editor.getErrorMessage());
    this.updateApplyButton();
  }

  @Override
  public Composite getComposite() {
    return parent;
  }
  
  @Override
  public boolean isValid() {
    return editor.isValid();
  }
  
  @Override
  protected void performDefaults() {
    editor.setValuesToDefault();
    editorChanged(new AllEditorChangeEvent());
    super.performDefaults();
  }
  
  @Override
  public boolean performOk() {
    try {
      editor.writeValuesTo(getStore());
      return true;
    } catch (CoreException e) {
      return false;
    }
  }
  
  private IStore getStore() {
    IResource resource = getResource();
    if (resource == null) return new NullStore();
    return new ResourcePropertyStore(resource, getPropertyQualifier());
  }
  
  /**
   * @return  The editor contained by this page.
   */
  protected IEditor getEditor() {
    return editor;
  }
  
  /**
   * @return  The project resource edited by this page.
   */
  protected IResource getResource() {
    IAdaptable element = getElement();
    if (element instanceof IResource) return (IResource) element;
    Object resource = element.getAdapter(IResource.class);
    if (resource instanceof IResource) return (IResource) resource;
    return null;
  }
}
