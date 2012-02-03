package net.vtst.ow.closure.compiler.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * A map which provides lookups by keys or by values.
 * Partial implementation.
 * @author Vincent Simonet
 *
 * @param <K>  Type for keys, shall be hashable.
 * @param <V>  Type for values, shall be hashable.
 */
public class BidiHashMap<K, V> {

  private Map<K, V> map = new HashMap<K, V>();
  private Map<V, Collection<K>> reverseMap = new HashMap<V, Collection<K>>();
  
  private Collection<K> getKeysAsCollection(V value) {
    Collection<K> keys = reverseMap.get(value);
    if (keys == null) {
      keys = new HashSet<K>();
      reverseMap.put(value, keys);
    }
    return keys;
  }
  
  public Iterable<K> getKeys(V value) {
    return getKeysAsCollection(value);
  }
  
  public V put(K key, V value) {
    V previousValue = map.put(key, value);
    if (previousValue != null) {
      getKeysAsCollection(previousValue).remove(key);
    }
    getKeysAsCollection(value).add(key);
    return previousValue;
  }
  
  public V get(K key) {
    return map.get(key);
  }
  
  public void removeAllKeysFor(V value) {
    Collection<K> keys = getKeysAsCollection(value);
    for (K key: keys) {
      map.remove(key);
    }
    keys.clear();
  }

  public void addAllKeys(Collection<K> keys, V value) {
    for (K key: keys) {
      V previousValue = map.put(key, value);
      if (previousValue != null) {
        getKeysAsCollection(previousValue).remove(key);
      }
    }
    getKeysAsCollection(value).addAll(keys);
  }
  
}
