package net.vtst.ow.closure.compiler.util;

public class UnhandledException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private Throwable exception;
  
  public UnhandledException(Throwable exception) {
    this.exception = exception;
  }

  public UnhandledException(String message, Throwable exception) {
    this.exception = exception;
  }

  public String toString() {
    return super.toString() + "\n" + exception.toString();
  }
  
}
