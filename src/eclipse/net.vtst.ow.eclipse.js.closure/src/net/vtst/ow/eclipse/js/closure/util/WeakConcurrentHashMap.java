package net.vtst.ow.eclipse.js.closure.util;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A concurrent hash map containing weak references.
 * @author vtst
 *
 * @param <K> Type for keys.
 * @param <V> Type for values.
 */
public class WeakConcurrentHashMap<K, V> {

  private ConcurrentHashMap<K, WeakReference<V>> map = new ConcurrentHashMap<K, WeakReference<V>>(); 
  
  public V get(K key) {
    WeakReference<V> ref = map.get(key);
    if (ref == null) return null;
    else return ref.get();
  }
  
  public void put(K key, V value) {
    map.put(key, new WeakReference<V>(value));
  }

  public void clear() {
    map.clear();
  }

}
