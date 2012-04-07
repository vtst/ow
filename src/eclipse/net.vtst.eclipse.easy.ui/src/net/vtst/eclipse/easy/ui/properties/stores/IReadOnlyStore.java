package net.vtst.eclipse.easy.ui.properties.stores;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

/**
 * Interface for objects storing field values, with read-only access.
 * @author Vincent Simonet
 */
public interface IReadOnlyStore {

  /**
   * Get the value of a boolean field in the store.
   * @param name  The field name.
   * @param defaultValue  The default value for the field.
   * @return  The current value (which can be the default one).
   * @throws CoreException
   */
  public boolean get(String name, boolean defaultValue) throws CoreException;

  /**
   * Get the value of an integer field in the store.
   * @param name  The field name.
   * @param defaultValue  The default value for the field.
   * @return  The current value (which can be the default one).
   * @throws CoreException
   */
  public int get(String name, int defaultValue) throws CoreException;

  /**
   * Get the value of a double field in the store.
   * @param name  The field name.
   * @param defaultValue  The default value for the field.
   * @return  The current value (which can be the default one).
   * @throws CoreException
   */
  public double get(String name, double defaultValue) throws CoreException;

  /**
   * Get the value of a string field in the store.
   * @param name  The field name.
   * @param defaultValue  The default value for the field.
   * @return  The current value (which can be the default one).
   * @throws CoreException
   */
  public String get(String name, String defaultValue) throws CoreException;
  
  /**
   * Get the value of a string list field in the store.
   * @param name  The field name.
   * @param defaultValue  The default value for the field.
   * @return  The current value (which can be the default one).
   * @throws CoreException
   */
  public List<String> get(String name, List<String> defaultValue) throws CoreException;
  
  /**
   * Test wether the store has a given field.
   * @param name  The field name.
   * @return  true iif the store has the field name {@code name}.
   * @throws CoreException
   */
  public boolean has(String name) throws CoreException;

}
