package net.vtst.eclipse.easyxtext.ui.util;

import java.util.List;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Miscellaneous utility functions used by other classes of <em>EasyXtext</em>.
 * This class is not intended to be extended or instantiated.
 * 
 * @author Vincent Simonet
 */
public class MiscUi {
  
  // TODO: This is duplicated in EasyUI. Should we keep?
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
  
  public static String getBaseName(Class<?> clazz) {
    String name = clazz.getName();
    int i = name.lastIndexOf('.');
    return name.substring(i + 1);
  }
  
  // TODO: This is duplicated in EasyUI. Should we keep?
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
