package net.vtst.ow.eclipse.less.scoping;

import java.util.ArrayList;
import java.util.Collection;

import net.vtst.ow.eclipse.less.less.HashOrClass;
import net.vtst.ow.eclipse.less.less.HashOrClassRef;
import net.vtst.ow.eclipse.less.less.HashOrClassRefTarget;
import net.vtst.ow.eclipse.less.less.MixinUtils;
import net.vtst.ow.eclipse.less.less.SimpleSelector;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

public class MixinPath {
  private ArrayList<String> path;
  
  public MixinPath(EList<HashOrClassRef> selectors) {
    this.path = new ArrayList<String>(selectors.size());
    for (HashOrClassRef hashOrClass : selectors) {
      path.add(MixinUtils.getIdent(hashOrClass));
    }
  }
  
  // Constructor useful for completion proposals.
  public MixinPath(String element) {
    this.path = new ArrayList<String>(1);
    this.path.add(element);
  }

  // Constructor useful for completion proposals.
  public MixinPath(Collection<String> selectors) {
    this.path = new ArrayList<String>(selectors.size());
    this.path.addAll(selectors);
  }

  public int size() { return path.size(); }
  public String get(int index) { return path.get(index); }

  public boolean isMatching(int position, HashOrClassRefTarget obj) {
    String pattern = this.path.get(position);
    return pattern.isEmpty() || pattern.equals(MixinUtils.getIdent(obj));
  }

  public boolean isMatching(int position, String ident) {
    String pattern = this.path.get(position);
    return pattern.isEmpty() || pattern.equals(ident);
  }

  public int isMatching(int position, EList<SimpleSelector> selectors) {
    if (selectors.size() == 1 && selectors.get(0).getCriteria().size() == 1) {
      // Only one piece of selector, partial match is possible
      EObject criteria = selectors.get(0).getCriteria().get(0);
      if (criteria instanceof HashOrClass && this.isMatching(position, (HashOrClass) criteria))
        return position + 1;
      else
        return -1;
    } else {
      // Several pieces of selector, only complete match is possible
      for (SimpleSelector selector : selectors) {
        for (EObject criteria : selector.getCriteria()) {
          if (position >= this.path.size()) return -1;
          if (!(criteria instanceof HashOrClass && this.isMatching(position, (HashOrClass) criteria)))
            return -1;
          ++position;
        }
        if (position >= selectors.size()) return position;
        else return -1;
      }
    }
    return -1;
  }
  
  @Override
  public boolean equals(Object other) {
    if (other instanceof MixinPath)
      return this.path.equals(((MixinPath) other).path);
    else
      return false;
  }
  
  @Override
  public int hashCode() {
    return this.path.hashCode();
  }

  
  public String toString() {
    StringBuffer buf = new StringBuffer();
    boolean first = true;
    for (String item : this.path) {
      if (first) first = false;
      else buf.append(", ");
      buf.append(item);
    }
    return buf.toString();
  }

}
