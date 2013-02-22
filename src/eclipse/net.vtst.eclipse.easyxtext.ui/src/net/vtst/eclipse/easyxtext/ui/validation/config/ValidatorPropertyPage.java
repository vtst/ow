package net.vtst.eclipse.easyxtext.ui.validation.config;

import java.util.ArrayList;
import java.util.Collection;

import net.vtst.eclipse.easyxtext.ui.EasyXtextUiPlugin;
import net.vtst.eclipse.easyxtext.ui.util.MiscUi;
import net.vtst.eclipse.easyxtext.util.IEasyMessages;
import net.vtst.eclipse.easyxtext.util.Misc;
import net.vtst.eclipse.easyxtext.validation.config.ConfigurableValidationMessageAcceptor;
import net.vtst.eclipse.easyxtext.validation.config.DeclarativeValidatorInspector;
import net.vtst.eclipse.easyxtext.validation.config.DeclarativeValidatorInspector.Group;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.jface.dialogs.ErrorDialog;
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
import org.eclipse.xtext.validation.CompositeEValidator;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import com.google.inject.Inject;

/**
 * This is an abstract class for implementing a property page for an
 * {@code AbstractDeclarativeValidator}.  See
 * {@code ConfigurableDeclarativeValidator} for more information. 
 * 
 * @author Vincent Simonet
 */
public class ValidatorPropertyPage extends PropertyPage {

  private AbstractDeclarativeValidator validator;
  private String validatorClassName;
  private DeclarativeValidatorInspector inspector;
  private IResource resource;

  private Table list;
  private Button checkbox;

  /**
   * Initialize the private static fields.
   */
  private boolean init() {
    validator = getValidator();
    if (validator == null) return false;
    validatorClassName = MiscUi.getBaseName(validator.getClass());
    inspector = new DeclarativeValidatorInspector(validator);
    resource = getResource();
    return true;
  }

  // **************************************************************************
  // Configuration

  @Inject
  private EValidator.Registry eValidatorRegistry;
  
  @Inject(optional=true)
  private EPackage ePackage;

  /**
   * @return The validator configured by the property page.
   * The default package looks for an AbstractDeclarativeValidator associated with the package.
   */
  protected AbstractDeclarativeValidator getValidator() {
    if (ePackage == null) {
      showErrorMessageDuringInit("EPackage not injected");
      return null;
    }
    EValidator validator = eValidatorRegistry.getEValidator(ePackage);
    if (validator == null) {
      showErrorMessageDuringInit("No validator found for the current package");
      return null;
    }
    ArrayList<AbstractDeclarativeValidator> declarativeValidators = new ArrayList<AbstractDeclarativeValidator>(1);
    getValidatorRec(validator, declarativeValidators);
    if (declarativeValidators.size() != 1) {
      showErrorMessageDuringInit("Found the following declarative validators: ");
      for (AbstractDeclarativeValidator v : declarativeValidators)
        showErrorMessageDuringInit(v.getClass().getName());
      return null;
    }
    return declarativeValidators.get(0);
  }
  
  private void getValidatorRec(EValidator validator, Collection<AbstractDeclarativeValidator> declarativeValidators) {
    if (validator instanceof AbstractDeclarativeValidator) {
      declarativeValidators.add((AbstractDeclarativeValidator) validator);
    } else if (validator instanceof CompositeEValidator) {
      for (CompositeEValidator.EValidatorEqualitySupport equalitySupport : ((CompositeEValidator) validator).getContents()) {
        getValidatorRec(equalitySupport.getDelegate(), declarativeValidators);
      }      
    }
  }

  @Inject(optional=true)
  private IEasyMessages messages;
  
  /**
   * Get the label displayed to the user for a group of checks.
   * @param name  The name of the group of check.
   * @return  The label displayed to the user for this group.  If null,
   *   the name given in the {@code ConfigurableCheck} annotation is used.
   */
  protected String getGroupLabel(String name) {
    if (messages == null) return null;
    return messages.getString(validatorClassName + "_" + name);
  }
  
  // **************************************************************************
  // User interface
      
  /* (non-Javadoc)
   * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createContents(Composite parent) {
    if (!init()) return null;
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
      showErrorMessage("Cannot read properties from the current project", e);
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
  // Error messages
  
  private void showErrorMessage(String message, CoreException exn) {
    if (exn.getStatus() != null) {
      showErrorMessage(message, exn.getStatus());
    } else {
      showErrorMessage(message, exn.getMessage());
    }
  }
  
  private void showErrorMessage(String message, IStatus status) {
    ErrorDialog.openError(getShell(), this.getClass().getName(), message, status); 
  }
  
  private void showErrorMessage(String message, String cause) {
    ErrorDialog.openError(
        getShell(), this.getClass().getName(), message, 
        new Status(Status.ERROR, EasyXtextUiPlugin.PLUGIN_ID, cause));
  }
  
  private void showErrorMessageDuringInit(String cause) {
    showErrorMessage("Cannot initialize the property page", cause);
  }

  // **************************************************************************
  // Properties

  @Override
  protected void performDefaults() {
    if (validator == null) return;
    setCheckbox(false);
    for (int i = 0; i < inspector.getGroups().size(); ++i) {
      list.getItem(i).setChecked(inspector.getGroups().get(i).enabledByDefault);
    }
    super.performDefaults();
  }
  
  @Override
  public boolean performOk() {
    if (validator == null) return false;
    try {
      if (checkbox.getSelection()) {
        for (int i = 0; i < inspector.getGroups().size(); ++i) {
          inspector.setEnabled(resource, inspector.getGroups().get(i), list.getItem(i).getChecked());
        }
      } else {
        inspector.clearAllProperties(resource);
      }
    } catch (CoreException e) {
      showErrorMessage("Cannot apply changes to project", e);
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
