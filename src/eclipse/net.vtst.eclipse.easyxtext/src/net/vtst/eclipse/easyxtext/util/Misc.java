// EasyXtext
// (c) Vincent Simonet, 2011

package net.vtst.eclipse.easyxtext.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Miscellaneous utility functions used by other classes of <em>EasyXtext</em>.
 * This class is not intended to be extended or instantiated.
 * 
 * @author Vincent Simonet
 */
public class Misc {

  /**
   * Private constructor to disallow instantiation.
   */
  private Misc() {
  }

  /**
   * Join the string representation of an iterable of object, with inserting a
   * separator between objects.
   * 
   * @param iterable
   *          Iterable of objects whose string representation have to be joined.
   * @param separator
   *          The separator to insert between two objects.
   * @return The joined string.
   */
  public static String join(Iterable<? extends Object> iterable,
      String separator) {
    StringBuffer buf = new StringBuffer();
    boolean isFirst = true;
    for (Object obj : iterable) {
      if (isFirst)
        isFirst = false;
      else
        buf.append(separator);
      buf.append(obj);
    }
    return buf.toString();
  }

  public static <T> T[] addListToArray(T[] array, List<T> list) {
    T[] result = Arrays.copyOf(array, array.length + list.size());
    int index = 0;
    for (T obj : list) {
      result[array.length + index] = obj;
      ++index;
    }
    return result;
  }

  public static <V, K> HashMap<V, K> invertMap(Map<K, V> map) {
    HashMap<V, K> inv = new HashMap<V, K>();
    for (Entry<K, V> entry : map.entrySet())
      inv.put(entry.getValue(), entry.getKey());
    return inv;
  }

}
