package net.vtst.ow.eclipse.js.closure.properties;

import java.util.Arrays;
import java.util.Comparator;

import net.vtst.ow.eclipse.js.closure.OwJsClosureMessages;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;
import net.vtst.ow.eclipse.js.closure.builder.ClosureNature;
import net.vtst.ow.eclipse.js.closure.util.Utils;
import net.vtst.ow.eclipse.js.closure.util.listeners.NullSwtSelectionListener;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

// TODO: The property page made a misleading resize of the property dialog box.

@SuppressWarnings("restriction")
public class ClosureProjectPropertyPage extends PropertyPage {
	
	private OwJsClosureMessages messages = OwJsClosurePlugin.getDefault().getMessages();

	/**
	 * Constructor for SamplePropertyPage.
	 */
	public ClosureProjectPropertyPage() {
		super();
	}

	private Button enableClosureSupport;
  private Text closureBaseDir;
  private List otherLibrariesList;
  private Group libraries;

  private Button removeOtherLibrary;
	
  // **************************************************************************
  // Create contents
  
  protected Control createContents(Composite parent) {
    Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH);
    enableClosureSupport = SWTFactory.createCheckButton(comp, messages.getString("enable_closure_support"), null, false, 1);
    enableClosureSupport.addSelectionListener(new NullSwtSelectionListener() {
      @Override public void widgetSelected(SelectionEvent event) {
        updateEnableClosureSupport();
      }
    });
    
    libraries = SWTFactory.createGroup(comp, messages.getString("libraries"), 3, 1, GridData.FILL_BOTH);
    SWTFactory.createLabel(libraries, messages.getString("closure_base_dir"), 1);
    closureBaseDir = SWTFactory.createSingleText(libraries, 1);
    //closureBaseDirText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    Button buttonBrowse = SWTFactory.createPushButton(libraries, messages.getString("closure_base_dir_browse"), null);
    buttonBrowse.addSelectionListener(new NullSwtSelectionListener() {
      @Override public void widgetSelected(SelectionEvent arg0) {
        selectClosureBaseDir();
      }
    });
    
    Label label = SWTFactory.createLabel(libraries, messages.getString("other_libraries"), 1);
    GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
    gd.verticalSpan = 3;
    label.setLayoutData(gd);
    
    otherLibrariesList = new List(libraries, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
    gd = new GridData(GridData.FILL_BOTH);
    gd.verticalSpan = 3;
    otherLibrariesList.setLayoutData(gd);
    
    Button addOtherLibrary = SWTFactory.createPushButton(libraries, messages.getString("add_other_library"), null);
    addOtherLibrary.addSelectionListener(new NullSwtSelectionListener() {
      @Override public void widgetSelected(SelectionEvent arg0) {
        addOtherLibrary();
      }
    });
    removeOtherLibrary = SWTFactory.createPushButton(libraries, messages.getString("remove_other_library"), null);
    removeOtherLibrary.addSelectionListener(new NullSwtSelectionListener() {
      @Override public void widgetSelected(SelectionEvent arg0) {
        removeOtherLibrary();
      }
    });
    SWTFactory.createLabel(libraries, messages.getString("libraries_help_message"), 3);
    Button buttonTest = SWTFactory.createPushButton(libraries, "Test", null);
    buttonTest.addSelectionListener(new NullSwtSelectionListener() {
      @Override public void widgetSelected(SelectionEvent arg0) {
        selectClosureBaseDir();
      }
    });
    initializeControlValues();
    return comp;
  }

  // **************************************************************************
  // Event handlers

  private void updateEnableClosureSupport() {
    boolean enabled = enableClosureSupport.getSelection();
    libraries.setEnabled(enabled);
    for (Control control: libraries.getChildren()) {
      control.setEnabled(enabled);
    }
    updateRemoveOtherLibrary();
  }
  
  private void updateRemoveOtherLibrary() {
    removeOtherLibrary.setEnabled(enableClosureSupport.getSelection() && otherLibrariesList.getSelectionCount() > 0);
  }
  
  private void selectClosureBaseDir() {
    DirectoryDialog dialog = new DirectoryDialog(this.getShell());
    dialog.setText(messages.getString("select_closure_base_dir_title"));
    dialog.setMessage(messages.getString("select_closure_base_dir_message"));
    dialog.setFilterPath(closureBaseDir.getText());
    String newDir = dialog.open();
    if (newDir != null) closureBaseDir.setText(newDir);
  }
  
  private void addOtherLibrary() {
    DirectoryDialog dialog = new DirectoryDialog(this.getShell());
    dialog.setText(messages.getString("add_other_library_title"));
    dialog.setMessage(messages.getString("add_other_library_message"));
    String newDir = dialog.open();
    if (newDir == null) return;
    addOtherLibrary(newDir);
  }

  private void addOtherLibrary(String path) {
    String[] existingPaths = otherLibrariesList.getItems();
    int i = 0;
    for (i = 0; i < existingPaths.length; ++i) {
      int cmp = existingPaths[i].compareTo(path);
      if (cmp == 0) {
        selectOtherLibrary(i);
        return;
      }
      if (cmp > 0) break;
    }
    otherLibrariesList.add(path, i);
    selectOtherLibrary(i);
  }
  
  private void selectOtherLibrary(int index) {
    otherLibrariesList.select(index);
    updateRemoveOtherLibrary();
  }
  
  private void removeOtherLibrary() {
    int index = otherLibrariesList.getSelectionIndex();
    otherLibrariesList.remove(index);
    int numberOfRemainingElements = otherLibrariesList.getItemCount();
    if (numberOfRemainingElements == 0) updateRemoveOtherLibrary();
    else selectOtherLibrary(Math.min(index, numberOfRemainingElements - 1));
  }

  // **************************************************************************

	protected void performDefaults() {
		super.performDefaults();
	}
	
	public boolean performOk() {
    IProject project = (IProject) getElement();
	  ClosureProjectPersistentPropertyHelper helper = new ClosureProjectPersistentPropertyHelper(project);
	  try {
	    Utils.setProjectNature(project, ClosureNature.NATURE_ID, enableClosureSupport.getSelection());
	    helper.setClosureBaseDir(closureBaseDir.getText());
	    helper.setOtherLibraries(otherLibrariesList.getItems());
    } catch (CoreException e1) {
      return false;
    }
	  return true;
	}

	private void initializeControlValues() {
	  IProject project = (IProject) getElement();
    ClosureProjectPersistentPropertyHelper helper = new ClosureProjectPersistentPropertyHelper(project);
    try {
      enableClosureSupport.setSelection(project.hasNature(ClosureNature.NATURE_ID));
      String closureBaseDirValue = helper.getClosureBaseDir();
      if (closureBaseDirValue != null) closureBaseDir.setText(closureBaseDirValue);
      String[] otherLibrariesValue = helper.getOtherLibraries();
      Arrays.sort(otherLibrariesValue, new Comparator<String>(){
        @Override
        public int compare(String arg0, String arg1) {
          return arg0.compareTo(arg1);
        }});
      for (String otherLibrary: otherLibrariesValue) otherLibrariesList.add(otherLibrary);
      if (otherLibrariesList.getItemCount() > 0) otherLibrariesList.select(0);
    } catch (CoreException e) {
    }
    updateEnableClosureSupport();
	}
	
}