package net.vtst.eclipse.easy.ui.properties.fields;

import net.vtst.eclipse.easy.ui.properties.stores.IReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.IStore;

import org.eclipse.core.runtime.CoreException;

public interface IField<T> {

  /**
   * Bind the field within a {@code IRecord}. 
   * @param name
   */
  public void bind(String name);
  
  /**
   * Get the current value of the field in a store.
   * @param store  The store.
   * @return  The current value, or the default one.
   * @throws CoreException
   */
  public T get(IReadOnlyStore store) throws CoreException;
  
  /**
   * Set the current value of the field in a store.
   * @param store  The store to update.
   * @param value  The value to set.
   * @throws CoreException
   */
  public void set(IStore store, T value) throws CoreException;

  /**
   * @return  The name of the field.
   */
  public String getName();

  /**
   * @return  The default value for the field.
   */
  public T getDefault();
  
  /**
   * Test whether two values for the field are equal.
   * @param value1
   * @param value2
   * @return  true iif value1 == value2.
   */
  public boolean valueEqual(T value1, T value2);
}
