package net.vtst.eclipse.easyxtext.util;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Helper class for accessing to strings stored in a message bundle (e.g. to get
 * i18n user messages) via a Guice-injectable class.
 * 
 * <p>
 * In order to use this class, you should sub-class it, and implements the
 * method <code>getBundleName</code> and <code>getDelegate</code>. In order to
 * load the resource bundle only once, sub-classes should have the annotation
 * com.google.inject.Singleton.
 * </p>
 * 
 * @author vtst
 */
public abstract class EasyResourceBundle implements IEasyMessages {

  private ResourceBundle bundle;

  public EasyResourceBundle() {
    bundle = getBundle();
  }

  public abstract ResourceBundle getBundle();

  /**
   * This method is called by the constructor to get the delegate bundle.
   * 
   * @return  null.
   */
  public EasyResourceBundle getDelegate() {
    return null;
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
      if (getDelegate() == null)
        return "!" + key + "!";
      else
        return getDelegate().getString(key);
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
      if (getDelegate() == null)
        return "!" + key + "!";
      else
        return getDelegate().getString(key);
    }
  }

}
