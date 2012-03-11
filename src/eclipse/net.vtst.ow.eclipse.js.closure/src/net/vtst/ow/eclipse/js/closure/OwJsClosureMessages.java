// EasyXtext
// (c) Vincent Simonet, 2011

package net.vtst.ow.eclipse.js.closure;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Provides built-in localized messages for <em>EasyXtext</em> classes.
 * @author Vincent Simonet
 */
public class OwJsClosureMessages {
  
  private ResourceBundle bundle;
  private OwJsClosureMessages delegate;

  public OwJsClosureMessages() {
    bundle = getBundle();
  }

  public ResourceBundle getBundle() {
    return ResourceBundle.getBundle("net.vtst.ow.eclipse.js.closure.OwJsClosureMessages");
  }
  
  /**
   * Get a string resource from the bundle. First look in the current bundle,
   * and fallback to the delegate.
   * 
   * @param key
   *          The key of the string resource to get.
   * @return The string resource (or "!key!" if the key was not found).
   */
  public String getString(String key) {
    try {
      return bundle.getString(key);
    } catch (MissingResourceException e) {
      if (delegate == null)
        return "!" + key + "!";
      else
        return delegate.getString(key);
    }
  }

  /**
   * Get a string resource from the bundle, and format it.
   * 
   * @param key
   *          The key of the string resource to get.
   * @param strings
   *          The format arguments.
   * @return Teh string resource, with the format arguments filled in.
   */
  public String format(String key, String... strings) {
    try {
      return String.format(bundle.getString(key), (Object[]) strings);
    } catch (MissingResourceException e) {
      if (delegate == null)
        return "!" + key + "!";
      else
        return delegate.getString(key);
    }
  }

}
