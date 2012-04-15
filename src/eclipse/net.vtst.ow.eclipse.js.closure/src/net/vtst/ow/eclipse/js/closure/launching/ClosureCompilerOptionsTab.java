package net.vtst.ow.eclipse.js.closure.launching;

import net.vtst.eclipse.easy.ui.properties.editors.ICompositeEditor;
import net.vtst.eclipse.easy.ui.properties.pages.EasyLaunchConfigurationTab;
import net.vtst.ow.eclipse.js.closure.OwJsClosureMessages;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;

public class ClosureCompilerOptionsTab extends EasyLaunchConfigurationTab {

  private OwJsClosureMessages messages = OwJsClosurePlugin.getDefault().getMessages();
  
  @Override
  public String getMessage(String key) {
    return messages.getStringOrNull("ClosureCompilerOptionsTab_" + key);
  }

  @Override
  public String getName() {
    return messages.getString("ClosureCompilerOptionsTab");
  }

  @Override
  protected ICompositeEditor createEditor() {
    return new ClosureCompilerOptionsEditor(this);
  }

}
