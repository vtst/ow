// EasyXtext
// (c) Vincent Simonet, 2011

package net.vtst.eclipse.easyxtext;

import java.util.ResourceBundle;

import net.vtst.eclipse.easyxtext.util.EasyResourceBundle;

import com.google.inject.Singleton;

/**
 * Provides built-in localized messages for <em>EasyXtext</em> classes.
 * @author Vincent Simonet
 */
@Singleton
public class EasyXtextMessages extends EasyResourceBundle {
  public ResourceBundle getBundle() {
    return ResourceBundle.getBundle("net.vtst.eclipse.easyxtext.EasyXtextMessages");
  }
}
