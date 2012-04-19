package net.vtst.ow.closure.compiler.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

/**
 * A list, which ignores addings of elements which are already in the list.
 * De-duping is performed thanks to a hash set.
 * @author Vincent Simonet
 *
 * @param <T>  The type of the elements of the list.
 */
public class ListWithoutDuplicates<T> {

  private ArrayList<T> list;
  private HashSet<T> set;

  public ListWithoutDuplicates() {
    this(16);
  }
  
  public ListWithoutDuplicates(int reservedSize) {
    this.list = new ArrayList<T>(reservedSize);
    this.set = new HashSet<T>(reservedSize);
  }
  
  public boolean add(T element) {
    if (set.add(element)) {
      list.add(element);
      return true;
    }
    else return false;
  }
  
  public void addAll(Iterable<T> elements) {
    for (T element: elements) add(element);
  }
  
  public ArrayList<T> asList() {
    return list;
  }
  
  public void sortList(Comparator<T> comparator) {
    Collections.sort(list, comparator);
  }
  
}
