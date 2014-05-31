package net.vtst.eclipse.easyxtext.ui.editor.autoedit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ConfigureAutoEdit {
  /**
   * @return true if the check is configurable.
   */
  boolean configurable() default true;
  
  /**
   * @return The default state of the check.
   */
  boolean defaultState() default true;
  
  /**
   * @return Rank.
   */
  int order() default 0;
}
