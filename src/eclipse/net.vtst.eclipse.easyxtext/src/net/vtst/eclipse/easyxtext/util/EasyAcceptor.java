package net.vtst.eclipse.easyxtext.util;

import java.util.Collection;

import org.eclipse.xtext.util.IAcceptor;

public class EasyAcceptor {
  
  private static class FromCollection<T> implements IAcceptor<T> {

    private Collection<T> collection;

    public FromCollection(Collection<T> collection) {
      this.collection = collection;
    }
    
    @Override
    public void accept(T t) {
      collection.add(t);
    }
    
  }
  
  public static <T> IAcceptor<T> fromCollection(Collection<T> collection) {
    return new FromCollection<T>(collection);
  }
  
}
