package net.vtst.eclipse.easyxtext.ui.validation;

import java.util.ArrayList;
import java.util.Map;

import net.vtst.eclipse.easyxtext.validation.ConfigurableAbstractDeclarativeValidator;
import net.vtst.eclipse.easyxtext.validation.ConfigurableAbstractDeclarativeValidator.Group;

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

/**
 * This is an abstract class for implementing a property page for an
 * AbstractDeclarativeValidator.
 * 
 * @author Vincent Simonet
 */
public abstract class ValidationPropertyPage extends PropertyPage {

  private ArrayList<Group> groups;
  private String propertyQualifier;
  private IResource resource;

  private Table list;
  private Button checkbox;

  protected abstract AbstractDeclarativeValidator getValidator();
  protected abstract String getPropertyQualifier();

  /**
   * Initialize the private static fields.
   */
  private void init() {
    ConfigurableAbstractDeclarativeValidator configurableValidator = 
        new ConfigurableAbstractDeclarativeValidator(getValidator());
    groups = configurableValidator.getGroups(); 
    propertyQualifier = getPropertyQualifier();
    resource = getResource();
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
    boolean isCustomized = false;
    for (Group group : groups) {
      TableItem item = new TableItem(list, SWT.NONE);
      item.setText(new String[] {group.name});
      QualifiedName qualifiedName = getQualifiedName(group);
      if (!isCustomized && resource.getPersistentProperty(qualifiedName) != null) isCustomized = true;
      item.setChecked(getProperty(qualifiedName, group.enabledByDefault));
    }
    setCheckbox(isCustomized);
  }

  // **************************************************************************
  // Properties

  @Override
  protected void performDefaults() {
    setCheckbox(false);
    for (int i = 0; i < groups.size(); ++i) {
      list.getItem(i).setChecked(groups.get(i).enabledByDefault);
    }
    super.performDefaults();
  }
  
  @Override
  public boolean performOk() {
    try {
      if (checkbox.getSelection()) {
        for (int i = 0; i < groups.size(); ++i) {
          setProperty(getQualifiedName(groups.get(i)), list.getItem(i).getChecked());
        }
      } else {
        clearAllProperties();
      }
    } catch (CoreException e) {
      e.printStackTrace();
    }
    return super.performOk();
  }
  
  private boolean getProperty(QualifiedName name, boolean defaultValue) throws CoreException {
    String value = resource.getPersistentProperty(name);
    if (value == null) return defaultValue;
    return Boolean.parseBoolean(value);
  }
  
  private void setProperty(QualifiedName name, boolean value) throws CoreException {
    resource.setPersistentProperty(name, Boolean.toString(value));
  }
  
  private void clearAllProperties() throws CoreException {
    Map<QualifiedName, String> properties = resource.getPersistentProperties();
    for (QualifiedName propertyName : properties.keySet()) {
      if (propertyQualifier.equals(propertyName.getQualifier())) {
        resource.setPersistentProperty(propertyName, null);
      }
    }
  }
  
  private QualifiedName getQualifiedName(Group group) {
    return new QualifiedName(propertyQualifier, group.name);
  }
  
  protected IResource getResource() {
    IAdaptable element = getElement();
    if (element instanceof IResource) return (IResource) element;
    Object resource = element.getAdapter(IResource.class);
    if (resource instanceof IResource) return (IResource) resource;
    return null;
  }
}
