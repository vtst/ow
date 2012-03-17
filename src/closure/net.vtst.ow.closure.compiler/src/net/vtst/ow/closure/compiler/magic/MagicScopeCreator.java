package net.vtst.ow.closure.compiler.magic;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.PassConfig;
import com.google.javascript.jscomp.Scope;
import com.google.javascript.rhino.Node;

/**
 * Wrapper around com.google.javascript.jscomp.PassConfig to access its typed scope creator,
 * though it is not public.
 * @author Vincent Simonet
 */
public class MagicScopeCreator {

  private Object memoizedScopeCreator = null;
  private Method memoizedScopeCreator_getScopeIfMemoized = null;
  private static Method compiler_getPassConfig = 
      Magic.getDeclaredMethod(Compiler.class, "getPassConfig");
  
  /**
   * WARNING! The object has to be created after the compilation!
   * @param passConfig  The passes configuration where to take the scope creator.
   */
  public MagicScopeCreator(PassConfig passConfig) {
    setPassConfig(passConfig);
  }
  
  /**
   * @param compiler  The compiler where to take the passes configuration.
   */
  public MagicScopeCreator(Compiler compiler) {
    try {
      Object object = compiler_getPassConfig.invoke(compiler);
      if (!(object instanceof PassConfig)) throw new MagicException();
      setPassConfig((PassConfig) object);
    } catch (IllegalArgumentException e) {
      throw new MagicException(e);
    } catch (IllegalAccessException e) {
      throw new MagicException(e);
    } catch (InvocationTargetException e) {
      throw new MagicException(e);
    }
  }
  
  private void setPassConfig(PassConfig passConfig) {
    try {
      Field field = PassConfig.class.getDeclaredField("typedScopeCreator");
      field.setAccessible(true);
      memoizedScopeCreator = field.get(passConfig);
      if (memoizedScopeCreator == null) return;
      memoizedScopeCreator_getScopeIfMemoized = 
          memoizedScopeCreator.getClass().getDeclaredMethod("getScopeIfMemoized", Node.class);
      memoizedScopeCreator_getScopeIfMemoized.setAccessible(true);
    } catch (SecurityException e) {
      throw new MagicException(e);
    } catch (NoSuchFieldException e) {
      throw new MagicException(e);
    } catch (IllegalAccessException e) {
      throw new MagicException(e);
    } catch (NoSuchMethodException e) {
      throw new MagicException(e);
    }
  }


  /**
   * Get the scope for a node in the scope creator.
   * @param node  The node to look for.
   * @return  The found scope, or null.
   */
  public Scope getScope(Node node) {
    if (memoizedScopeCreator_getScopeIfMemoized == null) return null;
    try {
      Object scope = memoizedScopeCreator_getScopeIfMemoized.invoke(memoizedScopeCreator, node);
      if (scope instanceof Scope) return (Scope) scope;
    } catch (IllegalArgumentException e) {
      throw new MagicException(e);
    } catch (IllegalAccessException e) {
      throw new MagicException(e);
    } catch (InvocationTargetException e) {
      throw new MagicException(e);
    }
    return null;
  }
    
}
