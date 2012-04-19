package net.vtst.ow.closure.compiler.magic;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.Scope;
import com.google.javascript.rhino.Node;

/**
 * Wrapper around {@code com.google.javascript.jscomp.PassConfig} to access its typed scope creator,
 * though it is not public.
 * @author Vincent Simonet
 */
public class MagicScopeCreator {

  private Object memoizedScopeCreator = null;
  private Method memoizedScopeCreator_getScopeIfMemoized = null;
  
  /**
   * WARNING! The object has to be created after the compilation!
   * @param compiler  The compiler where to take the passes configuration.
   */
  public MagicScopeCreator(Compiler compiler) {
    setMemoizedScopeCreator(compiler.getTypedScopeCreator());
  }
  
  private void setMemoizedScopeCreator(Object object) {
    if (object == null) return;
    memoizedScopeCreator = object;
    try {
      memoizedScopeCreator_getScopeIfMemoized = 
          memoizedScopeCreator.getClass().getDeclaredMethod("getScopeIfMemoized", Node.class);
      memoizedScopeCreator_getScopeIfMemoized.setAccessible(true);
    } catch (SecurityException e) {
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
