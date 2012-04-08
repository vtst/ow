package net.vtst.ow.eclipse.js.closure.properties;

import net.vtst.eclipse.easy.ui.properties.pages.EasyProjectPropertyPage;
import net.vtst.ow.eclipse.js.closure.OwJsClosureMessages;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;

public abstract class ClosureAsbtractPropertyPage extends EasyProjectPropertyPage {

  private OwJsClosureMessages messages = OwJsClosurePlugin.getDefault().getMessages();;

  @Override
  public String getMessage(String key) {
    return messages.getStringOrNull("ClosureProjectPropertyPage_" + key);
  }

  @Override
  protected String getPropertyQualifier() {
    return OwJsClosurePlugin.PLUGIN_ID;
  }

}
