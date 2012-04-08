package net.vtst.ow.eclipse.js.closure.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferencePageContainer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import net.vtst.eclipse.easy.ui.properties.pages.EasyProjectPropertyPage;
import net.vtst.ow.eclipse.js.closure.OwJsClosureMessages;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;
import net.vtst.ow.eclipse.js.closure.builder.ClosureNature;

public abstract class ClosureAsbtractPropertyPage extends EasyProjectPropertyPage {

  private OwJsClosureMessages messages = OwJsClosurePlugin.getDefault().getMessages();;
    
  public void setContainer(IPreferencePageContainer container) {
    getEditor().setEnabled(enableEditor());
    super.setContainer(container);
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
    return hasNature();
  }

  protected boolean hasNature() {
    try {
      return ((IProject) getElement()).hasNature(ClosureNature.NATURE_ID);
    } catch (CoreException e) {
      return false;
    }
  }
}
