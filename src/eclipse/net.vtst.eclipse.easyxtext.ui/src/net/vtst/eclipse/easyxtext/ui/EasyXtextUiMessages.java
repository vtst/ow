// EasyXtext
// (c) Vincent Simonet, 2011

package net.vtst.eclipse.easyxtext.ui;

import java.util.ResourceBundle;

import net.vtst.eclipse.easyxtext.util.EasyResourceBundle;

import com.google.inject.Singleton;

@Singleton
public class EasyXtextUiMessages extends EasyResourceBundle {
  public ResourceBundle getBundle() {
    return ResourceBundle.getBundle("net.vtst.eclipse.easyxtext.ui.EasyXtextUiMessages");
  }
}
