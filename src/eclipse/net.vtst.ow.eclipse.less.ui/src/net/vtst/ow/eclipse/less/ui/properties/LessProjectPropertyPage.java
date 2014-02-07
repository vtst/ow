package net.vtst.ow.eclipse.less.ui.properties;

import java.util.Collections;

import net.vtst.eclipse.easyxtext.ui.util.SWTFactory;
import net.vtst.ow.eclipse.less.LessRuntimeModule;
import net.vtst.ow.eclipse.less.properties.LessProjectProperty;
import net.vtst.ow.eclipse.less.properties.ResourceListProperty;
import net.vtst.ow.eclipse.less.ui.LessUiMessages;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PropertyPage;

import com.google.inject.Inject;

public class LessProjectPropertyPage extends PropertyPage {

  @Inject
  private ResourceListControl<IContainer> includePaths;

  @Inject
  private ResourceListControl<IFile> roots;

  @Inject
  private LessUiMessages messages;

  @Override
  protected Control createContents(Composite parent) {
    Composite composite = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH);
    Label label = SWTFactory.createLabel(composite, messages.getString("LessProjectPropertyPage_includePaths"), 1);
    label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
    includePaths.createContents(IContainer.class, composite);
    label = SWTFactory.createLabel(composite, messages.getString("LessProjectPropertyPage_roots"), 1);
    label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
    roots.createContents(IFile.class, composite);
    final IContentType lessContentType = Platform.getContentTypeManager().getContentType(LessRuntimeModule.CONTENT_TYPE_ID);
    final IProject project = this.getProject();
    roots.setAddFilter(new ViewerFilter() {
      public boolean select(Viewer viewer, Object parent, Object element) {
        if (element instanceof IProject) return project.equals(element);
        else if (element instanceof IContainer) return true;
        else if (element instanceof IFile) {
          IFile file = (IFile) element;
          try { 
            if (file.getContentDescription() == null) return false;
            IContentType fileContentType = file.getContentDescription().getContentType();
            if (fileContentType != null && fileContentType.isKindOf(lessContentType)) 
              return true; 
          } catch (CoreException e) {}          
        }
        return false;
      }
    });
    try {
      includePaths.setCurrentValue(ResourceListProperty.<IContainer>get(IContainer.class, getProject(), new QualifiedName(LessProjectProperty.QUALIFIER, LessProjectProperty.INCLUDE_PATHS)));
      roots.setCurrentValue(ResourceListProperty.<IFile>get(IFile.class, getProject(), new QualifiedName(LessProjectProperty.QUALIFIER, LessProjectProperty.ROOTS)));
    } catch (CoreException e) {
      this.setErrorMessage(e.getMessage());
    }
    return composite;
  }

  @Override
  protected void performDefaults() {
    includePaths.setCurrentValue(Collections.<IContainer>emptyList());
    roots.setCurrentValue(Collections.<IFile>emptyList());
    super.performDefaults();
  }
  
  @Override
  public boolean performOk() {
    try {
      ResourceListProperty.<IContainer>set(
          getProject(),
          new QualifiedName(LessProjectProperty.QUALIFIER, LessProjectProperty.INCLUDE_PATHS),
          includePaths.getCurrentValue());
      ResourceListProperty.<IFile>set(
          getProject(),
          new QualifiedName(LessProjectProperty.QUALIFIER, LessProjectProperty.ROOTS),
          roots.getCurrentValue());
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
