package net.vtst.ow.eclipse.js.closure.preferences;

import org.eclipse.jface.preference.IPreferenceStore;

import net.vtst.eclipse.easy.ui.properties.editors.ICompositeEditor;
import net.vtst.eclipse.easy.ui.properties.pages.EasyPreferencePage;
import net.vtst.ow.eclipse.js.closure.OwJsClosureMessages;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;

public class ClosurePreferencePage extends EasyPreferencePage {

  private OwJsClosureMessages messages = OwJsClosurePlugin.getDefault().getMessages();;

  @Override
  public String getMessage(String key) {
    return messages.getString(key);
  }

  @Override
  protected ICompositeEditor createEditor() {
    return new ClosurePreferenceEditor(this);
  }
  
  public IPreferenceStore getPreferenceStore() {
    return OwJsClosurePlugin.getDefault().getPreferenceStore();
  }

}
