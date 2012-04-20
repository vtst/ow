package net.vtst.ow.eclipse.js.closure.properties.file;

import net.vtst.eclipse.easy.ui.properties.editors.ICompositeEditor;
import net.vtst.eclipse.easy.ui.properties.pages.EasyResourcePropertyPage;
import net.vtst.ow.eclipse.js.closure.OwJsClosureMessages;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;

public class ClosureFileMainPropertyPage extends EasyResourcePropertyPage {

  private OwJsClosureMessages messages = OwJsClosurePlugin.getDefault().getMessages();;

  @Override
  public String getMessage(String key) {
    return messages.getStringOrNull("ClosureFilePropertyPage_" + key);
  }

  @Override
  protected String getPropertyQualifier() {
    return OwJsClosurePlugin.PLUGIN_ID;
  }
  
  @Override
  protected ICompositeEditor createEditor() {
    return new ClosureFileMainPropertyEditor(this);
  }
  
}