package net.vtst.eclipse.easyxtext.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for an AbstractDeclarativeValidator that can be configured by a
 * ValidationPropertyPage.
 * 
 * @author Vincent Simonet
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ConfigurableValidator {
  /**
   * @return If true, all checks are made configurable by default in the property page.
   */
  boolean makeConfigurableByDefault() default true;
  
  /**
   * @return The default state for a check in the property page.  DEFAULT or ENABLED
   * mean that the check is enabled by default.  DISABLED means that the check is
   * disabled by default.  This value can be overridden on a per check basis via the
   * same property of ConfigurableCheck. 
   */
  CheckState defaultState() default CheckState.DEFAULT;
}
