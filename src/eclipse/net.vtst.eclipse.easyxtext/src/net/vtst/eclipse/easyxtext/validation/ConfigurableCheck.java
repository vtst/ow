package net.vtst.eclipse.easyxtext.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for a check of an AbstractDeclarativeValidator that can be
 * configured by a ValidationPropertyPage.
 * 
 * @author Vincent Simonet
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ConfigurableCheck {
  /**
   * @return The default state of the check.  DEFAULT means that the default
   * for the validator (as defined by ConfigurableValidator) is used.
   */
  CheckState defaultState() default CheckState.DEFAULT;
  
  /**
   * @return The name of the configuration group for this check.  By default,
   * this is the name of the method.
   */
  String group() default "";
  
  /**
   * @return The label displayed to the user to identify the check.
   */
  String label() default "";
}
