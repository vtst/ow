package net.vtst.eclipse.easyxtext.ui.validation.config;

import net.vtst.eclipse.easyxtext.ui.validation.config.ValidatorPageHelper.IStore;
import net.vtst.eclipse.easyxtext.validation.config.DeclarativeValidatorInspector.Group;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.PropertyPage;

import com.google.inject.Inject;

/**
 * This is an abstract class for implementing a property page for an
 * {@code AbstractDeclarativeValidator}.  See
 * {@code ConfigurableDeclarativeValidator} for more information. 
 * 
 * @author Vincent Simonet
 */
public class ValidatorPropertyPage extends PropertyPage {

  @Inject
  private ValidatorPageHelper helper;
  
  public ValidatorPropertyPage() {
  }
  
  @Override
  protected Control createContents(Composite parent) {
    final IResource resource = getResource();
    helper.init(getResource().getProject(), this.getShell(), new IStore() {
      public boolean getEnabled(Group group) throws CoreException {
        return helper.getInspector().getEnabled(resource, group);
      }

      public void setEnabled(Group group, boolean enabled) throws CoreException {
        helper.getInspector().setEnabled(resource, group, enabled);        
      }

      @Override
      public boolean getCustomized() throws CoreException {
        return helper.getInspector().getCustomized(resource);
      }

      @Override
      public void setCustomized(boolean customized) throws CoreException {
        helper.getInspector().setCustomized(resource, customized);
      }
    });
    return this.helper.createContents(parent);
  }
  
  @Override
  protected void performDefaults() {
    helper.performDefaults();
    super.performDefaults();
  }
  
  @Override
  public boolean performOk() {
    return helper.performOk() && super.performOk();
  }
  
  protected IResource getResource() {
    IAdaptable element = getElement();
    if (element instanceof IResource) return (IResource) element;
    Object resource = element.getAdapter(IResource.class);
    if (resource instanceof IResource) return (IResource) resource;
    return null;
  }

}
