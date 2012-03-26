package net.vtst.ow.eclipse.js.closure.dev;

public class OwJsDev {
  
  public static void log(String message, String...args) {
    System.out.println(String.format(message, (Object[]) args));
  }

}
