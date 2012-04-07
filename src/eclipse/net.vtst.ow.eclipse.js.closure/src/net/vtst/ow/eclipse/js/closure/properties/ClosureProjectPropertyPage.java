package net.vtst.ow.eclipse.js.closure.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import net.vtst.eclipse.easy.ui.listeners.NullSwtSelectionListener;
import net.vtst.eclipse.easy.ui.properties.editors.ICompositeEditor;
import net.vtst.eclipse.easy.ui.properties.pages.EasyProjectPropertyPage;
import net.vtst.eclipse.easy.ui.util.SWTFactory;
import net.vtst.ow.eclipse.js.closure.OwJsClosureMessages;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;
import net.vtst.ow.eclipse.js.closure.builder.ClosureNature;
import net.vtst.ow.eclipse.js.closure.util.Utils;

public class ClosureProjectPropertyPage extends EasyProjectPropertyPage {

  private OwJsClosureMessages messages = OwJsClosurePlugin.getDefault().getMessages();;
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
    messages = OwJsClosurePlugin.getDefault().getMessages();
    return new ClosurePropertyEditor(this);
  }

  @Override
  public String getMessage(String key) {
    return messages.getStringOrNull("ClosureProjectPropertyPage_" + key);
  }

  @Override
  protected String getPropertyQualifier() {
    return "Closure";
  }
  
  private boolean hasNature() {
    try {
      return ((IProject) getElement()).hasNature(ClosureNature.NATURE_ID);
    } catch (CoreException e) {
      return false;
    }
  }
  
  public boolean performOk() {
    try {
      Utils.setProjectNature((IProject) getElement(), ClosureNature.NATURE_ID, enableClosureSupport.getSelection());
    } catch (CoreException e) {
      return false;
    }
    return super.performOk();
  }
}