package net.vtst.eclipse.easyxtext.validation.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.vtst.eclipse.easyxtext.State;

/**
 * Annotation for a check of an AbstractDeclarativeValidator that can be
 * configured by a ValidationPropertyPage.
 * 
 * @author Vincent Simonet
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface ConfigurableCheck {
  /**
   * @return true if the check is configurable.
   */
  boolean configurable() default true;
  
  /**
   * @return The default state of the check.  DEFAULT means that the default
   * for the validator (as defined by ConfigurableValidator) is used.
   */
  State defaultState() default State.DEFAULT;
  
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
