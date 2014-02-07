package net.vtst.ow.eclipse.less.ui.properties;

import java.util.ArrayList;
import java.util.List;

import net.vtst.eclipse.easyxtext.ui.util.NullSwtSelectionListener;
import net.vtst.eclipse.easyxtext.ui.util.SWTFactory;
import net.vtst.ow.eclipse.less.ui.LessUiMessages;
import net.vtst.ow.eclipse.less.ui.LessUiModule;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import com.google.inject.Inject;

/**
 * A field whose values are lists of files.
 * @author Vincent Simonet
 */
public class ResourceListControl<T extends IResource> {

  @Inject
  private LessUiMessages messages;

  public ResourceListControl() {}

  private ViewerFilter addFilter = new ViewerFilter() {
    public boolean select(Viewer viewer, Object parent, Object element) {
      return (cls.isAssignableFrom(element.getClass()) || element instanceof IContainer);
    }
  };
  
  public void setAddFilter(ViewerFilter addFilter) { this.addFilter = addFilter; }
    
  private ISelectionStatusValidator addValidator = new ISelectionStatusValidator() {
    public IStatus validate(Object[] result) {
      if (getSelectedResource(result) == null)
        return new Status(IStatus.ERROR, LessUiModule.PLUGIN_ID, "");
      else
        return new Status(IStatus.OK, LessUiModule.PLUGIN_ID, "");
    }
  };

  public void setAddValidator(ISelectionStatusValidator addValidator) { this.addValidator = addValidator; }

  private org.eclipse.swt.widgets.List list;
  private Button removeButton;
  private Button addButton;
  private List<T> currentValue = new ArrayList<T>();
  private Class<? extends T> cls;

  public Composite createContents(Class<? extends T> cls, Composite parent) {
    this.cls = cls;
    Composite composite = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH);
    list = new org.eclipse.swt.widgets.List(composite, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.horizontalSpan = 1;
    gd.verticalSpan = 3;
    list.setLayoutData(gd);
    
    addButton = SWTFactory.createPushButton(composite, messages.getString("FolderListControl_add"), null);
    addButton.addSelectionListener(new NullSwtSelectionListener() {
      @Override public void widgetSelected(SelectionEvent event) {
        addResource();
      }
    });
    removeButton = SWTFactory.createPushButton(composite, messages.getString("FolderListControl_remove"), null);
    removeButton.addSelectionListener(new NullSwtSelectionListener() {
      @Override public void widgetSelected(SelectionEvent event) {
        removeResource();
      }
    });
    SWTFactory.createLabel(composite, "", 1);
    return composite;
  }
  
  public List<T> getCurrentValue() {
    return currentValue;
  }

  public void setCurrentValue(List<T> value) {
    currentValue.clear();
    list.removeAll();
    for (T resource: value) addResource(resource);
  }
  
  private void addResource() {
    ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
        null,
        new WorkbenchLabelProvider(),
        new BaseWorkbenchContentProvider());
    dialog.setAllowMultiple(false);
    dialog.setTitle(messages.getString("FolderListControl_add"));
    dialog.setMessage("");
    dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
    dialog.addFilter(this.addFilter);
    try {
      dialog.setInitialSelection(ResourcesPlugin.getWorkspace().getRoot());
    } catch (IllegalArgumentException exn) {}  // Raised by new Path(...)
    dialog.setValidator(addValidator);
    dialog.open();
    T resource = getSelectedResource(dialog.getResult());
    if (resource != null) {
      addResource((T) resource);
    }
  }
  
  @SuppressWarnings("unchecked")
  private T getSelectedResource(Object[] result) {
    if (result == null || result.length != 1) return null;
    Object result0 = result[0];
    if (cls.isAssignableFrom(result0.getClass())) return (T) result0;
    return null;
  }

  private void addResource(T resource) {
    String path = resource.getFullPath().toOSString();
    int i = currentValue.size(); 
    for (; i > 0; --i) {
      if (currentValue.get(i - 1).getFullPath().toOSString().compareTo(path) < 0) break;
    }
    currentValue.add(i, resource);
    list.add(path, i);
  }
  
  private void select(int index) {
    list.select(index);
    updateRemoveButton();
  }
  
  private void removeResource() {
    int index = list.getSelectionIndex();
    list.remove(index);
    currentValue.remove(index);
    int numberOfRemainingElements = list.getItemCount();
    if (numberOfRemainingElements == 0) updateRemoveButton();
    else select(Math.min(index, numberOfRemainingElements - 1));
  }

  private void updateRemoveButton() {
    removeButton.setEnabled(list.getSelectionCount() > 0);
  }

}
