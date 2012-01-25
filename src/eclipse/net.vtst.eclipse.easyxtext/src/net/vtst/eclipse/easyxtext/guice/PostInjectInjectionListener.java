// EasyXtext
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.eclipse.easyxtext.guice;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.google.inject.spi.InjectionListener;

/**
 * Post-injection listener for the annotation {@code PostInject}.
 * 
 * <p>
 * This injection listener is installed by {@code PostInjectTypeListener} on
 * every method which is annotated by {@code PostInject}. It just call the
 * method once the class is initialized.
 * </p>
 * 
 * @author Vincent Simonet
 * 
 * @param <T>
 *          The class the method belongs to.
 */
public class PostInjectInjectionListener<T> implements InjectionListener<T> {

  private Method method;

  public PostInjectInjectionListener(Method method) {
    method.setAccessible(true);
    this.method = method;
  }

  @Override
  public void afterInjection(T instance) {
    try {
      method.invoke(instance);
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
  }

}
