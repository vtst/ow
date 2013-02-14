package net.vtst.eclipse.easyxtext.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ConfigurableValidator {
  boolean makeConfigurableByDefault() default true;
  CheckState defaultState() default CheckState.DEFAULT;
}
