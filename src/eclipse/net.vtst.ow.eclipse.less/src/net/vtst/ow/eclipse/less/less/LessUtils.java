package net.vtst.ow.eclipse.less.less;

import org.eclipse.emf.ecore.EObject;

public class LessUtils {
  
  public static EObject getNthAncestor(EObject obj, int nth) {
    while (nth > 0 && obj != null) {
      obj = obj.eContainer();
    }
    return obj;
  }
  
}
