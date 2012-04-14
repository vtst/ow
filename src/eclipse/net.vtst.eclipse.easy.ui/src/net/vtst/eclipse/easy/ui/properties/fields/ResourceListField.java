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
import org.eclipse.core.resources.IProject;
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

  public static interface IFilter<T extends IResource> {
    public boolean show(T resource) throws CoreException;
    public boolean select(T resource) throws CoreException;
  }
  
  public static class TrueFilter<T extends IResource> implements IFilter<T> {
    public boolean show(T resource) throws CoreException { return true; }
    public boolean select(T resource) throws CoreException { return show(resource); }
  }
  
  public static abstract class OperatorFilter<T extends IResource> implements IFilter<T> {
    protected IFilter<T>[] filters;
    public OperatorFilter(IFilter<T> filter0, IFilter<T> filter1) {
      this(new IFilter[]{filter0, filter1});
    }
    public OperatorFilter(IFilter<T> filter0, IFilter<T> filter1, IFilter<T> filter2) {
      this(new IFilter[]{filter0, filter1, filter2});
    }
    public OperatorFilter(IFilter<T>[] filters) {
      this.filters = filters;
    }
  }
  
  public static class Or<T extends IResource> extends OperatorFilter<T> {
    public Or(IFilter<T> filter0, IFilter<T> filter1) {
      super(filter0, filter1);
    }
    public Or(IFilter<T> filter0, IFilter<T> filter1, IFilter<T> filter2) {
      super(filter0, filter1, filter2);
    }
    public boolean show(T resource) throws CoreException {
      for (IFilter<T> filter: filters) {
        if (filter.show(resource)) return true;
      }
      return false;
    }
    public boolean select(T resource) throws CoreException {
      for (IFilter<T> filter: filters) {
        if (filter.select(resource)) return true;
      }
      return false;
    }
  }

  public static class And<T extends IResource> extends OperatorFilter<T> {
    public And(IFilter<T> filter0, IFilter<T> filter1) {
      super(filter0, filter1);
    }
    public boolean show(T resource) throws CoreException {
      for (IFilter<T> filter: filters) {
        if (filter.show(resource)) return true;
      }
      return false;
    }
    public boolean select(T resource) throws CoreException {
      for (IFilter<T> filter: filters) {
        if (filter.select(resource)) return true;
      }
      return false;
    }
  }
  
  public static class FileType<T extends IResource> extends TrueFilter<T> {
    private Pattern pattern;
    private IContentType contentType;

    public FileType(Pattern pattern, IContentType contentType) {
      this.pattern = pattern;
      this.contentType = contentType;
    }
    
    @Override
    public boolean show(T resource) {
      if (resource instanceof IFile) {
        IFile file = (IFile) resource;
        if (pattern != null && pattern.matcher(file.getName()).matches()) return true;
        try { if (contentType != null && file.getContentDescription().getContentType().isKindOf(contentType)) return true; }
        catch (CoreException e) {}
        return false;
      } else {
        return false;
      }
    }    
  }

  public static class ProjectNature<T extends IResource> extends TrueFilter<T> {
    private String nature;
    public ProjectNature(String nature) {
      this.nature = nature;
    }
    public boolean show(T resource) throws CoreException {
      if (resource instanceof IProject) {
        IProject project = (IProject) resource;
        return project.hasNature(nature);
      } else {
        return false;
      }
    }
  }
  
  public static class ResourceType<T extends IResource> extends TrueFilter<T> {
    private Class<? extends T> cls;
    public ResourceType(Class<? extends T> cls) {
      this.cls = cls;
    }
    public boolean show(T resource) throws CoreException {
      return cls.isInstance(resource);
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
      
      addOtherLibrary = SWTFactory.createPushButton(parent, getMessage("add", "ResourceListField_add"), null);
      addOtherLibrary.addSelectionListener(new NullSwtSelectionListener() {
        @Override public void widgetSelected(SelectionEvent event) {
          addResource();
        }
      });
      removeButton = SWTFactory.createPushButton(parent, getMessage("remove", "ResourceListField_remove"), null);
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
    
    private void addResource() {
      ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
          null,
          new WorkbenchLabelProvider(),
          new BaseWorkbenchContentProvider());
      dialog.setAllowMultiple(false);
      dialog.setTitle(getMessage("title", "ResourceListField_title"));
      dialog.setMessage(getMessage("message", "ResourceListField_message"));
      dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
      dialog.addFilter(new ViewerFilter() {
        public boolean select(Viewer viewer, Object parent, Object element) {
          T resource = field.castResource(element);
          if (resource == null) return false;
          try {
            return field.filter.show(resource);
          } catch (CoreException e) {
            return false;
          }
        }});
      try {
        dialog.setInitialSelection(ResourcesPlugin.getWorkspace().getRoot());
      } catch (IllegalArgumentException exn) {}  // Raised by new Path(...)
      dialog.setValidator(new ISelectionStatusValidator() {
        public IStatus validate(Object[] result) {
          if (getSelectedResource(result) == null)
            return new Status(IStatus.ERROR, EasyUiPlugin.PLUGIN_ID, getMessage("error", "ResourceListField_error"));
          else
            return new Status(IStatus.OK, EasyUiPlugin.PLUGIN_ID, "");
        }});
      dialog.open();
      T resource = getSelectedResource(dialog.getResult());
      if (resource != null) addResource((T) resource);
    }
    
    private T getSelectedResource(Object[] result) {
      if (result == null || result.length != 1) return null;
      T resource = field.castResource(result[0]);
      try {
        if (resource == null || !field.filter.select(resource)) return null;
        return resource;
      } catch (CoreException e) {
        return null;
      }
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
