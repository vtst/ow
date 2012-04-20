package net.vtst.ow.eclipse.js.closure.properties;

import net.vtst.eclipse.easy.ui.properties.pages.EasyResourcePropertyPage;
import net.vtst.ow.eclipse.js.closure.OwJsClosureMessages;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;
import net.vtst.ow.eclipse.js.closure.builder.ClosureNature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferencePageContainer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public abstract class ClosureAsbtractPropertyPage extends EasyResourcePropertyPage {

  private OwJsClosureMessages messages = OwJsClosurePlugin.getDefault().getMessages();;
    
  public void setContainer(IPreferencePageContainer container) {
    // TODO: This is not the right way to do!
    if (getEditor() != null) getEditor().setEnabled(enableEditor());
    super.setContainer(container);
  }  
  
  @Override
  protected Control createContents(Composite parent) {
    Control control = super.createContents(parent);
    getEditor().setEnabled(enableEditor());
    return control;
  }

  
  @Override
  public String getMessage(String key) {
    return messages.getStringOrNull("ClosureProjectPropertyPage_" + key);
  }

  @Override
  protected String getPropertyQualifier() {
    return OwJsClosurePlugin.PLUGIN_ID;
  }
  
  protected boolean enableEditor() {
    // We enable everything even if the nature is not selected, because it seems difficult
    // to do otherwise as we have several pages.
    // return hasNature();
    return true;
  }
  
  public boolean performOk() {
    boolean result = super.performOk();
    if (result) {
      IResource project = getResource();
      try {
        project.touch(null);
      } catch (CoreException e) {
        e.printStackTrace();
      }
    }
    return result;
  }

  protected boolean hasNature() {
    try {
      return ((IProject) getElement()).hasNature(ClosureNature.NATURE_ID);
    } catch (CoreException e) {
      return false;
    }
  }
}
