package net.vtst.ow.closure.compiler.magic;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * This class implements static functions to access non-public methods and fields from classes
 * of the Google Closure Compiler.
 * @author Vincent Simonet
 */
public class Magic {
      
  public static Class<?> getClass(String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new MagicException(e);
    }
  }
    
  public static Constructor<?> getDeclaredConstructor(Class<?> cls, Class<?>... parameterTypes) {
    try {
      Constructor<?> constructor = cls.getDeclaredConstructor(parameterTypes);
      constructor.setAccessible(true);
      return constructor;
    } catch (SecurityException e) {
      throw new MagicException(e);
    } catch (NoSuchMethodException e) {
      throw new MagicException(e);
    }
  }

  public static Field getDeclaredField(Class<?> cls, String name) {
    try {
      Field field = cls.getDeclaredField(name);
      field.setAccessible(true);
      return field;
    } catch (SecurityException e) {
      throw new MagicException(e);
    } catch (NoSuchFieldException e) {
      throw new MagicException(e);
    }
  }
  
  public static Method getDeclaredMethod(Class<?> cls, String name, Class<?>... parameterTypes) {
    try {
      Method method = cls.getDeclaredMethod(name, parameterTypes);
      method.setAccessible(true);
      return method;
    } catch (SecurityException e) {
      throw new MagicException(e);
    } catch (NoSuchMethodException e) {
      throw new MagicException(e);
    }
  }
  
  public static Class<?> getNestedClass(Class<?> cls, String nestedClassName) {
    for (Class<?> nestedClass: cls.getDeclaredClasses()) {
      if (nestedClass.getSimpleName().equals(nestedClassName))
        return nestedClass;
    }
    throw new MagicException();
  }

}
