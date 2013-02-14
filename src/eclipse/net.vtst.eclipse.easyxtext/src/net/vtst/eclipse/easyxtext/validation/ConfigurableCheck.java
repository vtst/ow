package net.vtst.eclipse.easyxtext.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ConfigurableCheck {
  CheckState defaultState() default CheckState.DEFAULT;
  String group() default "";
  String label() default "";
}
