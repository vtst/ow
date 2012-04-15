package net.vtst.eclipse.easy.ui.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class Utils {
  
  private static final String SEPARATOR = "\n";
  
  private static String join(List<String> strings, String separator) {
    int n = separator.length() * Math.max(0, strings.size() - 1);
    for (String string: strings) {
      n += string.length();
    }
    StringBuffer buffer = new StringBuffer(n);
    boolean first = true;
    for (String string: strings) {
      if (first) first = false;
      else buffer.append(separator);
      buffer.append(string);
    }
    return buffer.toString();
  }
  
  public static String stringListToString(List<String> list) {
    return join(list, SEPARATOR);
  }
  
  public static List<String> stringToStringList(String value, List<String> defaultValue) {
    if (value == null) return defaultValue;
    if (value.length() == 0) return Collections.emptyList();
    return Arrays.asList(value.split(SEPARATOR));
  }
  
  /**
   * Returns the currently active workbench window shell or <code>null</code>
   * if none.
   * From {@code org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin}.
   * @return the currently active workbench window shell or <code>null</code>
   */
  public static Shell getShell() {
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (window == null) {
      IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
      if (windows.length > 0) {
        return windows[0].getShell();
      }
    } else {
      return window.getShell();
    }
    return null;
  }
  
  /**
   * Render a command line given as a string array into a single string with proper escaping
   * @param commandLine
   * @return  The single string representing the command line.
   * (From {@code org.eclipse.jdt.internal.launching.StandardVMRunner}.)
   */
  public static String renderCommandLine(List<String> commandLine) {
      if (commandLine.size() < 1)
          return ""; //$NON-NLS-1$
      StringBuffer buf = new StringBuffer();
      for (String item: commandLine) {
          buf.append(' ');
          char[] characters = item.toCharArray();
          StringBuffer command = new StringBuffer();
          boolean containsSpace = false;
          for (int j = 0; j < characters.length; j++) {
              char character = characters[j];
              if (character == '\"') {
                  command.append('\\');
              } else if (character == ' ') {
                  containsSpace = true;
              }
              command.append(character);
          }
          if (containsSpace) {
              buf.append('\"');
              buf.append(command.toString());
              buf.append('\"');
          } else {
              buf.append(command.toString());
          }
      }
      return buf.toString();
  }


}
