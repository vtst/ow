package net.vtst.ow.closure.compiler.util;

import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.ErrorManager;
import com.google.javascript.jscomp.JSError;

/**
 * An error manager which discards all errors.  This is useful for running a silent
 * compilation.
 * @author Vincent Simonet
 */
public class NullErrorManager implements ErrorManager {
  
  public NullErrorManager() {}

  @Override
  public void generateReport() {}

  @Override
  public int getErrorCount() {
    return 0;
  }

  @Override
  public JSError[] getErrors() {
    return new JSError[0];
  }

  @Override
  public double getTypedPercent() {
    return 0;
  }

  @Override
  public int getWarningCount() {
    return 0;
  }

  @Override
  public JSError[] getWarnings() {
    return new JSError[0];
  }

  @Override
  public void report(CheckLevel level, JSError error) {}

  @Override
  public void setTypedPercent(double percent) {}

}
