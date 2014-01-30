package net.vtst.ow.eclipse.less.ui.properties;

import java.util.Collections;

import net.vtst.eclipse.easyxtext.ui.util.SWTFactory;
import net.vtst.ow.eclipse.less.properties.ResourceListProperty;
import net.vtst.ow.eclipse.less.properties.LessProjectProperty;
import net.vtst.ow.eclipse.less.ui.LessUiMessages;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PropertyPage;

import com.google.inject.Inject;

public class LessProjectPropertyPage extends PropertyPage {

  @Inject
  private ResourceListControl<IContainer> folderList;
  
  @Inject
  private LessUiMessages messages;

  @Override
  protected Control createContents(Composite parent) {
    Composite composite = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH);
    Label label = SWTFactory.createLabel(composite, messages.getString("LessProjectPropertyPage_includePaths"), 1);
    label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
    folderList.createContents(IContainer.class, composite);
    try {
      folderList.setCurrentValue(ResourceListProperty.<IContainer>get(IContainer.class, getProject(), new QualifiedName(LessProjectProperty.QUALIFIER, LessProjectProperty.INCLUDE_PATHS)));
    } catch (CoreException e) {
      this.setErrorMessage(e.getMessage());
    }
    return composite;
  }

  @Override
  protected void performDefaults() {
    folderList.setCurrentValue(Collections.<IContainer> emptyList());
    super.performDefaults();
  }
  
  @Override
  public boolean performOk() {
    try {
      ResourceListProperty.<IContainer>set(
          getProject(),
          new QualifiedName(LessProjectProperty.QUALIFIER, LessProjectProperty.INCLUDE_PATHS),
          folderList.getCurrentValue());
    } catch (CoreException e) {
      this.setErrorMessage(e.getMessage());
      return false;
    };
    return super.performOk();
  }
  /**
   * @return  The project resource edited by this page.
   */
  protected IProject getProject() {
    IAdaptable element = getElement();
    if (element instanceof IProject) return (IProject) element;
    Object resource = element.getAdapter(IResource.class);
    if (resource instanceof IProject) return (IProject) resource;
    return null;
  }

}
