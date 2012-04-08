package net.vtst.eclipse.easy.ui.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

}
