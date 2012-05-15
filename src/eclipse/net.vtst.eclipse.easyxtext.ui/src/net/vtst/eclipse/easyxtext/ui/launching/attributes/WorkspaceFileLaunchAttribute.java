// EasyXtext
// (c) Vincent Simonet, 2011

package net.vtst.eclipse.easyxtext.ui.launching.attributes;

import java.util.regex.Pattern;

import net.vtst.eclipse.easyxtext.ui.launching.tab.IEasyLaunchConfigurationTab;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Launch attribute which designates a file in the workspace.  It is internally stored as
 * a string, and is represented in launch configuration by a text box with a "browse" button
 * opening a file picker.  Files in the file picker are filtered by a pattern and/or a content type.
 * @author Vincent Simonet
 *
 */
public class WorkspaceFileLaunchAttribute extends AbstractStringLaunchAttribute {

  private Pattern pattern;  // May be null
  private IContentType contentType;  // May be null
  
  public WorkspaceFileLaunchAttribute(String defaultValue, Pattern pattern, IContentType contentType) {
    super(defaultValue);
    this.pattern = pattern;
    this.contentType = contentType;
  }
  
  public WorkspaceFileLaunchAttribute(String defaultValue, Pattern pattern) {
    this(defaultValue, pattern, null);
  }
  
  public WorkspaceFileLaunchAttribute(String defaultValue, IContentType contentType) {
    this(defaultValue, null, contentType);
  }


  public Control createControl(IEasyLaunchConfigurationTab tab, Composite parent, int hspan) {
    return new Control(parent, hspan, tab);
  }
  
  public IFile getFileValue(ILaunchConfiguration config) {
    String value = getValue(config);
    try {
      return ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(value));
    } catch (IllegalArgumentException exn) {
      return null;
    }
  }

  public void setFileValue(ILaunchConfigurationWorkingCopy config, IFile selectedFile) {
    setValue(config, selectedFile.getFullPath().toString());
  }

  public class Control extends AbstractLaunchAttribute<String>.Control {

    private Text text;
    private Button button;
    private String labelKey = getLabelKey();
    
    @SuppressWarnings("restriction")
    public Control(Composite parent, int hspan, IEasyLaunchConfigurationTab tab) {
      super(tab, parent, hspan);
      if (hspan < 3) return;
      addWidget(SWTFactory.createLabel(parent, tab.getString(labelKey), 1));
      text = SWTFactory.createSingleText(parent, hspan - 2);
      text.addModifyListener(tab.getUpdateListener());
      addWidget(text);
      button = SWTFactory.createPushButton(parent, tab.getString(labelKey + "_browse"), null);
      addWidget(button);
      button.addSelectionListener(new SelectionListener(){
        public void widgetSelected(SelectionEvent arg0) {
          selectFile();
        }
        public void widgetDefaultSelected(SelectionEvent arg0) {}
      });
      tab.registerControl(this);
    }
    
    private void selectFile() {
      // See http://eclipse-tips.com/how-to-guides/5-selection-dialogs-in-eclipse
      ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
          null,
          new WorkbenchLabelProvider(),
          new BaseWorkbenchContentProvider());
      dialog.setAllowMultiple(false);
      dialog.setTitle(tab.getString(labelKey + "_title"));
      dialog.setMessage(tab.getString(labelKey + "_message"));
      dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
      dialog.addFilter(new ViewerFilter() {
        public boolean select(Viewer viewer, Object parent, Object element) {
          if (element instanceof IFile) {
            IFile file = (IFile) element;
            if (pattern != null && pattern.matcher(file.getName()).matches()) return true;
            try { 
              if (file.getContentDescription() != null) {
                IContentType fileContentType = file.getContentDescription().getContentType();
                if (contentType != null && 
                    fileContentType != null && 
                    fileContentType.isKindOf(contentType)) 
                  return true; 
              }
            }
            catch (CoreException e) {}
            return false;
          } else {
            return true;
          }
        }});
      try {
        IPath path = new Path(text.getText());
        dialog.setInitialSelection(ResourcesPlugin.getWorkspace().getRoot().getFile(path));
      } catch (IllegalArgumentException exn) {}  // Raised by new Path(...)
      dialog.setValidator(new ISelectionStatusValidator() {
        public IStatus validate(Object[] result) {
          if (result.length != 1 ||
              !(result[0] instanceof IFile)) {
            return new Status(IStatus.ERROR, WorkspaceFileLaunchAttribute.class.getName(), tab.getString(labelKey + "_error"));
          }
          return new Status(IStatus.OK, WorkspaceFileLaunchAttribute.class.getName(), "");
        }});
      dialog.open();
      Object[] result = dialog.getResult();
      if (result == null || result.length != 1) return;
      if (!(result[0] instanceof IFile)) return;
      IFile selectedFile = (IFile) result[0];
      text.setText(selectedFile.getFullPath().toString());
    }    
        
    public String getControlValue() {
      return text.getText();
    }

    public void setControlValue(String value) {
      text.setText(value);
    } 
    
    public void addModifyListener(ModifyListener listener) {
      text.addModifyListener(listener);
    }

  }

}
