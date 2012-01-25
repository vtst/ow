package net.vtst.eclipse.easyxtext.util;


/**
 * Interface for accessing to strings stored in a message bundle.
 * 
 * @author Vincent Simonet
 *
 */
public interface IEasyMessages {

  /**
   * Get a string resource from the bundle.  First look in the current bundle, and fallback
   * to the delegate.
   * @param key  The key of the string resource to get.
   * @return  The string resource (or "!key!" if the key was not found). 
   */
  public String getString(String key);
  
  /**
   * Get a string resource from the bundle, and format it.
   * @param key  The key of the string resource to get.
   * @param strings  The format arguments.
   * @return  Teh string resource, with the format arguments filled in.
   */
  public String format(String key, String... strings);

}
