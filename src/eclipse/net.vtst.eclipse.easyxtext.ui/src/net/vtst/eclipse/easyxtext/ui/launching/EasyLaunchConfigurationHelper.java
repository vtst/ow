// EasyXtext
// (c) Vincent Simonet, 2011

package net.vtst.eclipse.easyxtext.ui.launching;

import java.lang.reflect.Field;

import net.vtst.eclipse.easyxtext.guice.PostInject;
import net.vtst.eclipse.easyxtext.ui.launching.attributes.AbstractLaunchAttribute;

/**
 * Definition of launch configurations, using reflection.
 * 
 * <p>
 * Base class for implementing helper class to access to the contents of a launch configuration,
 * and to generate launch configuration tabs.  Sub-classes shall define one field per attribute
 * of the launch configuration, which shall be instanciated by a sub-class of
 * {@link net.vtst.eclipse.easyxtext.ui.launching.attributes.AbstractLaunchAttribute}.
 * </p>
 * 
 * @author Vincent Simonet
 */
public abstract class EasyLaunchConfigurationHelper {
  
  @SuppressWarnings("rawtypes")
  @PostInject
  public final void initializeByReflection() {
    for (Field field: this.getClass().getFields()) {
      try {
        Object fieldValue = field.get(this);
        if (fieldValue instanceof AbstractLaunchAttribute) {
          ((AbstractLaunchAttribute) fieldValue).setName(field.getName());
        }
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
  }
  
}
