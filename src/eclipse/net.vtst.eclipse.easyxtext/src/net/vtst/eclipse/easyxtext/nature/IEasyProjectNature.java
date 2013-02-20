package net.vtst.eclipse.easyxtext.nature;

import org.eclipse.core.resources.IProjectNature;

/**
 * An interface for injecting project natures.  This interface extends the
 * standard interface {@code IProjectNature} which two methods which are useful
 * for manipulating the project nature in classes with dependency injection.
 * 
 * <p>
 * Note that an instance of {@code IProjectNature} can theoretically be used
 * several times in a plugin.xml file, while an instance of
 * {@code IEasyProjectNature} cannot.  This is due to the additional method
 * {@code getId()}, and this is intentional (to make the injection
 * unambiguous).
 * </p>
 * 
 * @author Vincent Simonet
 */
public interface IEasyProjectNature extends IProjectNature {
  /**
   * @return The ID for this project nature.
   */
  public String getId();
  
  /**
   * @return The display name for this project nature.
   */
  public String getName();
}
