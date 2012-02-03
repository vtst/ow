package net.vtst.ow.closure.compiler.magic;

/**
 * An exception to be raised by magic classes, in case accesses by reflection do not work.
 * @author Vincent Simonet
 */
public class MagicException extends RuntimeException {

  private static final long serialVersionUID = 1L;
  
  private Exception exception;
  
  public MagicException() {}

  public MagicException(Exception e) {
    exception = e;
  }
  
  public void printStackTrace() {
    super.printStackTrace();
    if (exception != null) exception.printStackTrace();
  }

}
