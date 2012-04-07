package net.vtst.eclipse.easy.ui.properties.stores;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

/**
 * Interface for objects storing field values, with read and write access.
 * @author Vincent Simonet
 */
public interface IStore extends IReadOnlyStore {

  /**
   * Set the value of a boolean field in the store.
   * @param name  The field name
   * @param value  The value to set.
   * @throws CoreException
   */
  public void set(String name, boolean value) throws CoreException;

  /**
   * Set the value of an integer field in the store.
   * @param name  The field name
   * @param value  The value to set.
   * @throws CoreException
   */
  public void set(String name, int value) throws CoreException;

  /**
   * Set the value of a double field in the store.
   * @param name  The field name
   * @param value  The value to set.
   * @throws CoreException
   */
  public void set(String name, double value) throws CoreException;
  
  /**
   * Set the value of a string field in the store.
   * @param name  The field name
   * @param value  The value to set.
   * @throws CoreException
   */
  public void set(String name, String value) throws CoreException;

  /**
   * Set the value of a string list field in the store.
   * @param name  The field name
   * @param value  The value to set.
   * @throws CoreException
   */
  public void set(String name, List<String> defaultValue) throws CoreException;

}
