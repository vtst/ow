package net.vtst.eclipse.easy.ui.properties.fields;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import net.vtst.eclipse.easy.ui.EasyUiPlugin;
import net.vtst.eclipse.easy.ui.listeners.NullSwtSelectionListener;
import net.vtst.eclipse.easy.ui.properties.editors.AbstractFieldEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.properties.stores.IReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.IStore;
import net.vtst.eclipse.easy.ui.util.SWTFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * A field whose values are lists of files.
 * @author Vincent Simonet
 */
public class ResourceListField<T extends IResource> extends AbstractField<List<T>> {

  public static interface IFilter<T> {
    public boolean select(T resource);
  }
  
  public static class TrueFilter<T> implements IFilter<T> {
    public boolean select(T resource) { return true; }
  }
  
  public static abstract class OperatorFilter<T> implements IFilter<T> {
    protected IFilter<T>[] filters;
    public OperatorFilter(IFilter<T> filter0, IFilter<T> filter1) {
      this(new IFilter[]{filter0, filter1});
    }
    public OperatorFilter(IFilter<T>[] filters) {
      this.filters = filters;
    }
  }
  
  public static class Orfilter<T> extends OperatorFilter<T> {
    public Orfilter(IFilter<T> filter0, IFilter<T> filter1) {
      super(filter0, filter1);
    }
    public boolean select(T resource) {
      for (IFilter<T> filter: filters) {
        if (filter.select(resource)) return true;
      }
      return false;
    }
  }
  
  private Class<T> cls;
  private IFilter<T> filter;

  public ResourceListField(Class<T> cls, IFilter<T> filter) {
    super(Collections.<T>emptyList());
    this.cls = cls;
    this.filter = filter;
  }
  
  public ResourceListField(Class<T> cls) {
    this(cls, new TrueFilter<T>());
  }
  
  @Override
  public List<T> get(IReadOnlyStore store) throws CoreException {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    List<String> list = store.get(name, Collections.<String>emptyList());
    ArrayList<T> result = new ArrayList<T>(list.size());
    for (String s: list) {
      T resource = castResource(root.findMember(new Path(s)));
      if (resource != null) result.add(resource);
    }
    return result;
  }
  
  @SuppressWarnings("unchecked")
  private T castResource(Object resource) {
    if (cls.isInstance(resource)) return (T) resource;
    return null;
  }
  
  @Override
  public void set(IStore store, List<T> value) throws CoreException {
    ArrayList<String> list = new ArrayList<String>(value.size());
    for (IResource r: value) list.add(r.getFullPath().toPortableString());
    store.set(name, list);
  }

  @Override
  public AbstractFieldEditor<List<T>> createEditor(IEditorContainer container, Composite parent) {
    return new Editor<T>(container, parent, this);
  }
  
  public static class Editor<T extends IResource> extends AbstractFieldEditor<List<T>> {
    
    private ResourceListField<T> field;
    private Label label;
    private org.eclipse.swt.widgets.List list;
    private Button removeButton;
    private Button addOtherLibrary;
    private List<T> currentValue = new ArrayList<T>();

    public Editor(IEditorContainer container, Composite parent, ResourceListField<T> field) {
      super(container, field);
      this.field = field;
      int hspan = getColumnCount(parent);
      if (hspan < 3) return;  // TODO
      label = SWTFactory.createLabel(parent, getMessage(), 1);
      GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
      gd.verticalSpan = 3;
      label.setLayoutData(gd);
      
      list = new org.eclipse.swt.widgets.List(parent, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
      gd = new GridData(GridData.FILL_BOTH);
      gd.horizontalSpan = hspan - 2;
      gd.verticalSpan = 3;
      list.setLayoutData(gd);
      
      addOtherLibrary = SWTFactory.createPushButton(parent, getMessage("add", "FileListField_add"), null);
      addOtherLibrary.addSelectionListener(new NullSwtSelectionListener() {
        @Override public void widgetSelected(SelectionEvent event) {
          addResource();
        }
      });
      removeButton = SWTFactory.createPushButton(parent, getMessage("remove", "FileListField_remove"), null);
      removeButton.addSelectionListener(new NullSwtSelectionListener() {
        @Override public void widgetSelected(SelectionEvent event) {
          removeResource();
        }
      });
      SWTFactory.createLabel(parent, "", 1);
    }
    
    @Override
    public List<T> getCurrentValue() {
      return currentValue;
    }

    @Override
    public void setCurrentValue(List<T> value) {
      currentValue.clear();
      list.removeAll();
      for (T resource: value) addResource(resource);
    }

    @Override
    protected boolean computeIsValid() {
      return true;
    }

    @Override
    protected String computeErrorMessage() {
      return null;
    }
    
    private Pattern pattern = null;
    private IContentType contentType = null;
    
    @SuppressWarnings("unchecked")
    private void addResource() {
      ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
          null,
          new WorkbenchLabelProvider(),
          new BaseWorkbenchContentProvider());
      dialog.setAllowMultiple(false);
      dialog.setTitle(getMessage("_title"));
      dialog.setMessage(getMessage("_message"));
      dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
      dialog.addFilter(new ViewerFilter() {
        public boolean select(Viewer viewer, Object parent, Object element) {
          T resource = field.castResource(element);
          if (resource == null) return false;
          return field.filter.select(resource);
        }});
      try {
        dialog.setInitialSelection(ResourcesPlugin.getWorkspace().getRoot());
      } catch (IllegalArgumentException exn) {}  // Raised by new Path(...)
      dialog.setValidator(new ISelectionStatusValidator() {
        public IStatus validate(Object[] result) {
          if (getSelectedResource(result) == null)
            return new Status(IStatus.ERROR, EasyUiPlugin.PLUGIN_ID, getMessage("error"));
          else
            return new Status(IStatus.OK, EasyUiPlugin.PLUGIN_ID, "");
        }});
      dialog.open();
      T resource = getSelectedResource(dialog.getResult());
      if (resource != null) addResource((T) resource);
    }
    
    private T getSelectedResource(Object[] result) {
      if (result.length != 1) return null;
      T resource = field.castResource(result[0]);
      if (resource == null || !field.filter.select(resource)) return null;
      return resource;
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

    @Override
    public void setEnabled(boolean enabled) {
      label.setEnabled(enabled);
      list.setEnabled(enabled);
      addOtherLibrary.setEnabled(enabled);
      if (enabled) updateRemoveButton();
      else removeButton.setEnabled(false);
    }
    
  }
}
