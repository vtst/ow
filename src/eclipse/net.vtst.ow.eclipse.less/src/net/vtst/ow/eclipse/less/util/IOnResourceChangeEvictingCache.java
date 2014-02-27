package net.vtst.ow.eclipse.less.util;

import org.eclipse.emf.ecore.resource.Resource;

import com.google.inject.ImplementedBy;
import com.google.inject.Provider;

/**
 * A key/value map, whose entries get automatically cleared on resource changes.
 * 
 * @author Vincent Simonet
 */
@ImplementedBy(OnResourceChangeEvictingCache.class)
public interface IOnResourceChangeEvictingCache {

  /**
   * Get the current value of the key in the map.  If the value is not present,
   * compute it using the provider.  The key/value entry will automatically
   * get cleared when resource changes.
   * For a given key, this method must always be called with the same type T.
   */
  <T> T get(Object key, Resource resource, Provider<T> provider);

  /**
   * Put a given key/value pair in the map.  If there is already an entry with
   * this key, it is removed first.
   */
  <T> void put(Object key, Resource resource, T value);

  /**
   * Remove any value associated with key in the map.
   * @param key
   */
  void remove(Object key);

}
