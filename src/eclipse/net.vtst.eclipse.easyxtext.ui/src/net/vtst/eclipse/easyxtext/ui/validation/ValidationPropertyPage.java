package net.vtst.eclipse.easyxtext.ui.validation;

import java.util.ArrayList;
import java.util.Map;

import net.vtst.eclipse.easyxtext.validation.config.ConfigurableValidationMessageAcceptor;
import net.vtst.eclipse.easyxtext.validation.config.DeclarativeValidatorInspector;
import net.vtst.eclipse.easyxtext.validation.config.DeclarativeValidatorInspector.Group;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.xtext.validation.AbstractDeclarativeValidator;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

/**
 * This is an abstract class for implementing a property page for an
 * {@code AbstractDeclarativeValidator}.  See
 * {@code ConfigurableDeclarativeValidator} for more information. 
 * 
 * @author Vincent Simonet
 */
public abstract class ValidationPropertyPage extends PropertyPage {

  private DeclarativeValidatorInspector inspector;
  private IResource resource;

  private Table list;
  private Button checkbox;

  /**
   * @return The validator configured by the property page.
   */
  protected abstract AbstractDeclarativeValidator getValidator();
  
  
  /**
   * Get the label displayed to the user for a group of checks.
   * @param name  The name of the group of check.
   * @return  The label displayed to the user for this group.  If null,
   *   the name given in the {@code ConfigurableCheck} annotation is used.
   */
  protected abstract String getGroupLabel(String name);

  /**
   * Initialize the private static fields.
   */
  private void init() {
    this.inspector = new DeclarativeValidatorInspector(getValidator());
    this.resource = getResource();
  }

  // **************************************************************************
  // User interface
  
  /* (non-Javadoc)
   * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createContents(Composite parent) {
    init();
    Composite composite = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH);
    checkbox = SWTFactory.createCheckButton(
        composite, "Customize errors and warnings:", null, false, 
        1);
    checkbox.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetDefaultSelected(SelectionEvent _) {}
      @Override
      public void widgetSelected(SelectionEvent _) { updateListStatus(); }
    });
    list = new Table(composite, SWT.V_SCROLL | SWT.CHECK | SWT.BORDER);
    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.horizontalSpan = 1;
    list.setLayoutData(gd);
    TableColumn column1 = new TableColumn(list, SWT.NONE);
    try {
      fillList();
    } catch (CoreException e) {
      this.setErrorMessage(e.toString());
    }
    column1.pack();
    return composite;
  }
  
  private void setCheckbox(boolean selected) {
    checkbox.setSelection(selected);
    updateListStatus();
  }
  
  private void updateListStatus() {
    list.setEnabled(checkbox.getSelection());
  }

  private void fillList() throws CoreException {
    for (Group group : inspector.getGroups()) {
      TableItem item = new TableItem(list, SWT.NONE);
      item.setText(new String[] {getGroupLabel(group)});
      item.setChecked(inspector.getEnabled(resource, group));
    }
    setCheckbox(inspector.hasProperty(resource));
  }
  
  private String getGroupLabel(Group group) {
    if (group.label != null) return group.label;
    String label = getGroupLabel(group.name);
    if (label != null) return label;
    return group.name;
  }

  // **************************************************************************
  // Properties

  @Override
  protected void performDefaults() {
    setCheckbox(false);
    for (int i = 0; i < inspector.getGroups().size(); ++i) {
      list.getItem(i).setChecked(inspector.getGroups().get(i).enabledByDefault);
    }
    super.performDefaults();
  }
  
  @Override
  public boolean performOk() {
    try {
      if (checkbox.getSelection()) {
        for (int i = 0; i < inspector.getGroups().size(); ++i) {
          inspector.setEnabled(resource, inspector.getGroups().get(i), list.getItem(i).getChecked());
        }
      } else {
        inspector.clearAllProperties(resource);
      }
    } catch (CoreException e) {
      e.printStackTrace();
    }
    resetValidatorCache();
    return super.performOk();
  }
    
  protected IResource getResource() {
    IAdaptable element = getElement();
    if (element instanceof IResource) return (IResource) element;
    Object resource = element.getAdapter(IResource.class);
    if (resource instanceof IResource) return (IResource) resource;
    return null;
  }
  
  private void resetValidatorCache() {
    ValidationMessageAcceptor messageAcceptor = getValidator().getMessageAcceptor();
    if (!(messageAcceptor instanceof ConfigurableValidationMessageAcceptor)) return;
    ConfigurableValidationMessageAcceptor configurableMessageAcceptor = (ConfigurableValidationMessageAcceptor) messageAcceptor;
    configurableMessageAcceptor.resetCache(resource.getProject());
  }


}
