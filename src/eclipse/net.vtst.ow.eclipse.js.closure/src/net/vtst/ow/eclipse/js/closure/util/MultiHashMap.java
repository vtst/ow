package net.vtst.ow.eclipse.js.closure.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Hash map from keys to multiple values.
 * @author Vincent Simonet
 *
 * @param <K>  Key type.
 * @param <V>  Value type.
 */
public class MultiHashMap<K, V> {
  
  private HashMap<K, HashSet<V>> map = new HashMap<K, HashSet<V>>();
  
  /**
   * Get the collection of values associated to a key in the map.  If the key is not present
   * to the map, put it in the map with an empty key.
   * @param key  The key to look for.
   * @return  The associated collection of values.
   */
  private Collection<V> getNotNull(K key) {
    HashSet<V> values = map.get(key);
    if (values == null) {
      values = new HashSet<V>();
      map.put(key, values);
    }
    return values;
  }
  
  /**
   * Add a value for a key.
   * @param key
   * @param value
   * @return  true if this is the first value for that key.
   */
  public boolean put(K key, V value) {
    Collection<V> values = getNotNull(key);
    boolean firstValueForKey = values.isEmpty();
    values.add(value);
    return firstValueForKey;
  }
  
  /**
   * Add several values associated with a single key in the map.
   * @param key  The key.
   * @param values  The collection of values to add.
   */
  public void addAll(K key, Collection<V> values) {
    Collection<V> existingValues = getNotNull(key);
    existingValues.addAll(values);
  }
  
  /**
   * Remove a value for a key.
   * @param key
   * @param value
   * @return  true if there is no more value for that key.
   */
  public boolean remove(K key, V value) {
    HashSet<V> values = map.get(key);
    if (values == null) return true;
    values.remove(value);
    return values.isEmpty();
  }
  
  /**
   * Remove all values associated with a given key.
   * @param key  The key to look for.
   */
  public void removeAll(K key) {
    HashSet<V> values = map.get(key);
    if (values != null) values.clear();
  }
  
  /**
   * Get the collection of values associated with a given key.
   * @param key  The key to look for.
   * @return  The collection of values associated with the key.  This might be empty, but
   *   this is never null.
   */
  public Collection<V> get(K key) {
    HashSet<V> values = map.get(key);
    if (values == null) return Collections.emptyList();
    else return Collections.unmodifiableCollection(values);
  }

}
