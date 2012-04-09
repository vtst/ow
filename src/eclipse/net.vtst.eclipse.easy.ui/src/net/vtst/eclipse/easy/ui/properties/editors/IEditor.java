package net.vtst.eclipse.easy.ui.properties.editors;

import net.vtst.eclipse.easy.ui.properties.stores.IReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.IStore;

import org.eclipse.core.runtime.CoreException;

/**
 * Interface for objects allowing to edit the value of one or several fields.
 * @author Vincent Simonet
 */
public interface IEditor {
  
  /**
   * Read the values from a store, and update the editor UI with these values.
   * @param store
   * @throws CoreException 
   */
  public void readValuesFrom(IReadOnlyStore store) throws CoreException;
  
  /**
   * Tests whether the current values of the editor have changed compared to those
   * from the store
   * @param store
   * @return true iff at least one value in the editor is different from that of the
   * store.
   * @throws CoreException
   */
  public boolean hasChanged(IReadOnlyStore store) throws CoreException;
  
  /**
   * Set the current values of the editor UI to the default values.
   */
  public void setValuesToDefault();
  
  /**
   * Write the current values of the editor UI to a store.
   * @param store
   * @throws CoreException 
   */
  public void writeValuesTo(IStore store) throws CoreException;
  
  
  /**
   * Test whether the current value of the editor UI is valid for the store.
   * @return  true iif the current value is valid.
   */
  public boolean isValid();
  
  /**
   * Get an error message to be shown to the user in case of invalid data.
   * @return  The error message, or null.
   */
  public String getErrorMessage();
  
  /**
   * Enable or disable the editor and all its sub-editors.
   * At creation, an editor is enabled by default.
   * @param enabled
   */
  public void setEnabled(boolean enabled);

}
