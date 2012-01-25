// EasyXtext
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.eclipse.easyxtext.guice;

import java.lang.reflect.Method;

import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

/**
 * Type listener for the annotation {@code PostInject}.
 * 
 * <p>
 * This type listener installs the injection listener for the methods which are
 * annotated by {@link PostInject}.
 * </p>
 * 
 * @author Vincent Simonet
 */
public class PostInjectTypeListener implements TypeListener {

  public <T> void hear(TypeLiteral<T> typeLiteral,
      TypeEncounter<T> typeEncounter) {
    for (Method method : typeLiteral.getRawType().getMethods()) {
      if (method.isAnnotationPresent(PostInject.class)
          && method.getParameterTypes().length == 0) {
        typeEncounter.register(new PostInjectInjectionListener<T>(method));
      }
    }
  }

}