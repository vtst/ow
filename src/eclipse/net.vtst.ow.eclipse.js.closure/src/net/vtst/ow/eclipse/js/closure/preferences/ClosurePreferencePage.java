package net.vtst.ow.eclipse.js.closure.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Control;

import net.vtst.eclipse.easy.ui.properties.editors.ICompositeEditor;
import net.vtst.eclipse.easy.ui.properties.pages.EasyPreferencePage;
import net.vtst.ow.eclipse.js.closure.OwJsClosureMessages;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;

public class ClosurePreferencePage extends EasyPreferencePage {

  private OwJsClosureMessages messages = OwJsClosurePlugin.getDefault().getMessages();;

  @Override
  protected ICompositeEditor createEditor() {
    return new ClosurePreferenceEditor(this);
  }
  
  public IPreferenceStore getPreferenceStore() {
    return OwJsClosurePlugin.getDefault().getPreferenceStore();
  }

  @Override
  public String getMessage(String key) {
    return messages.getStringOrNull("ClosurePreferencePage_" + key);
  }

}
