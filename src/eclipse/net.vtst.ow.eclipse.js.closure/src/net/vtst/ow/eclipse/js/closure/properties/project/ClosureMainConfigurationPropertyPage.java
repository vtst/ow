package net.vtst.ow.eclipse.js.closure.properties.project;

import net.vtst.eclipse.easy.ui.listeners.NullSwtSelectionListener;
import net.vtst.eclipse.easy.ui.properties.editors.ICompositeEditor;
import net.vtst.eclipse.easy.ui.util.SWTFactory;
import net.vtst.ow.eclipse.js.closure.builder.ClosureNature;
import net.vtst.ow.eclipse.js.closure.util.Utils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class ClosureMainConfigurationPropertyPage extends ClosureAsbtractPropertyPage {

  private Button enableClosureSupport;
  
  @Override
  protected Control createContents(Composite parent) {
    Composite composite = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH);
    enableClosureSupport = SWTFactory.createCheckButton(
        composite, getMessage("enableClosureSupport"), null, false, 1);
    enableClosureSupport.addSelectionListener(new NullSwtSelectionListener() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        getEditor().setEnabled(enableClosureSupport.getSelection());
      }
      @Override
      public void widgetDefaultSelected(SelectionEvent event) {}
    });
    super.createContents(composite);
    boolean hasNature = hasNature();
    enableClosureSupport.setSelection(hasNature);
    getEditor().setEnabled(hasNature);
    return composite;
  }
  
  @Override
  protected ICompositeEditor createEditor() {
    return new ClosureMainConfigurationEditor(this);
  }
  
  protected boolean enableEditor() {
    return true;
  }

  
  public boolean performOk() {
    try {
      IProject project = getProject();
      if (project != null) Utils.setProjectNature(project, ClosureNature.NATURE_ID, enableClosureSupport.getSelection());
    } catch (CoreException e) {
      return false;
    }
    return super.performOk();
  }
  
  protected IProject getProject() {
	IAdaptable element = getElement();
	if (element instanceof IProject) return (IProject) element;
	Object project = element.getAdapter(IProject.class);
	if (project instanceof IProject) return (IProject) project;
	return null;
  }

}