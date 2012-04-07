// EasyXtext
// (c) Vincent Simonet, 2011

package net.vtst.eclipse.easy.ui.messages;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Helper class for accessing to bundle messages.  Sub-classes must implement {@code getBundle}.
 * @author Vincent Simonet
 */
public abstract class EasyBundleMessages {
  
  private ResourceBundle bundle;
  private EasyBundleMessages delegate;

  /**
   * Create a new instance.
   */
  public EasyBundleMessages() {
    bundle = getBundle();
  }
  
  /**
   * Create a new instance with a delegate.
   * @param delegate
   */
  public EasyBundleMessages(EasyBundleMessages delegate) {
    this();
    this.delegate = delegate;
  }
  
  /**
   * @return  The bundle where to look for messages.
   */
  protected abstract ResourceBundle getBundle();
  
  /**
   * Get the message associated with a key, or null if not found.
   * @param key
   * @return  the message, or null.
   */
  public String getStringOrNull(String key) {
    try {
      return bundle.getString(key);
    } catch (MissingResourceException e) {
      if (delegate == null) return null;
      return delegate.getStringOrNull(key);
    }    
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
   * @return The string resource, with the format arguments filled in.
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
