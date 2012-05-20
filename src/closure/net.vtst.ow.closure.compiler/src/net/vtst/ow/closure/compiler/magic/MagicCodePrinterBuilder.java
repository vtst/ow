package net.vtst.ow.closure.compiler.magic;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.google.javascript.rhino.Node;

/**
 * Wrapper around {@code com.google.javascript.jscomp.CodePrinter}, to make it accessible
 * though it is not a public class.
 * @author Vincent Simonet
 */
public class MagicCodePrinterBuilder {
  
  public Object codePrinterBuilder;
  public static Constructor<?> constructor;
  public static Method method_build;
  public static Field field_prettyPrint;
  public static Field field_outputTypes;
  
  private static void initialize() {
    if (constructor != null) return;
    Class<?> cls = 
        Magic.getNestedClass(Magic.getClass("com.google.javascript.jscomp.CodePrinter"), "Builder");
    constructor = Magic.getDeclaredConstructor(cls, Node.class);
    method_build = Magic.getDeclaredMethod(cls, "build");
    field_prettyPrint = Magic.getDeclaredField(cls, "prettyPrint");
    field_outputTypes = Magic.getDeclaredField(cls, "outputTypes");
  }
  
  /**
   * @param node  The node to be printed.
   * @param prettyPrint  Whether to pretty print.
   * @param outputTypes  Whether to output types as JSDocStrings.
   */
  public MagicCodePrinterBuilder(Node node, boolean prettyPrint, boolean outputTypes) {
    initialize();
    try {
      codePrinterBuilder = constructor.newInstance(node);
      field_prettyPrint.setBoolean(codePrinterBuilder, prettyPrint);
      field_outputTypes.setBoolean(codePrinterBuilder, outputTypes);
    } catch (IllegalArgumentException e) {
      throw new MagicException(e);
    } catch (InstantiationException e) {
      throw new MagicException(e);
    } catch (IllegalAccessException e) {
      throw new MagicException(e);
    } catch (InvocationTargetException e) {
      Magic.catchInvocationTargetException(e);
    }
  }
  
  /**
   * Prints the node as a string.
   * @return  The printed representation.
   */
  public String build() {
    try {
      return (String) method_build.invoke(codePrinterBuilder);
    } catch (IllegalArgumentException e) {
      throw new MagicException(e);
    } catch (IllegalAccessException e) {
      throw new MagicException(e);
    } catch (InvocationTargetException e) {
      Magic.catchInvocationTargetException(e);
      return null;
    }
  }

}
