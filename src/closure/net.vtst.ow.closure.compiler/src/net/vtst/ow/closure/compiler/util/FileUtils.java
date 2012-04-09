package net.vtst.ow.closure.compiler.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility functions to manipulate File objects representing paths.
 * @author Vincent Simonet
 */
public class FileUtils {
  
  // This regex is used in split(...).
  private static String fileSeparatorRegex = (File.separatorChar == '\\' ? "\\\\" : File.separator);

  /**
   * Check whether a path is absolute. 
   * @param file  The checked path.
   * @return  true if {@code file} is an absolute path, false otherwise.
   */
  public static boolean isAbsolute(File file) {
    String path = file.getPath();
    for (File root: File.listRoots()) {
      if (path.startsWith(root.getPath())) return true;
    }
    return false;
  }
  
  /**
   * Check whether a path is relative. 
   * @param file  The checked path.
   * @return  true if {@code file} is a relative path, false otherwise.
   */
  public static boolean isRelative(File file) {
    return !isAbsolute(file);
  }
  
  private static String[] split(File file) {
    return file.getPath().split(fileSeparatorRegex);
  }
  
  private static void addSegments(List<String> addTo, String[] segments) {
    boolean first = true;
    for (String segment: segments) {
      if (segment.isEmpty() && !first || ".".equals(segment)) {
        // ignore
      } else if ("..".equals(segment) && !addTo.isEmpty()) {
        addTo.remove(addTo.size() - 1);
      } else {
        addTo.add(segment);
      }
      if (first) first = false;
    }
  }
  
  private static ArrayList<String> normalizeAsList(File file) {
    String[] segments = split(file);
    ArrayList<String> result = new ArrayList<String>(segments.length);
    addSegments(result, segments);
    return result;
  }
  
  public static File normalize(File file) {
    return new File(join(normalizeAsList(file)));
  }
  
  private static String join(List<String> segments) {
    StringBuffer buf = new StringBuffer();
    boolean first = true;
    for (String segment: segments) {
      if (first) first = false;
      else buf.append(File.separatorChar);
      buf.append(segment);
    }
    return buf.toString();
  }
  
  /**
   * Join two paths in the single path.
   * @param file1  The first path to join.
   * @param file2  The second path to join.
   * @return  The joined path.
   */
  public static File join(File file1, File file2) {
    String[] segments1 = split(file1);
    String[] segments2 = split(file2);
    ArrayList<String> segments = new ArrayList<String>(segments1.length + segments2.length);
    addSegments(segments, segments1);
    addSegments(segments, segments2);
    return new File(join(segments));
  }
  
  /**
   * Make a path relative to a reference.
   * @param reference  The reference path.
   * @param file  The path to make relative to the reference.
   * @return  A path which designates the same location as {@code file}, but relatively to {@code reference}.
   */
  public static File makeRelative(File reference, File file) {
    if (isRelative(file)) return join(reference, file);
    ArrayList<String> referenceSegments = normalizeAsList(reference);
    ArrayList<String> fileSegments = normalizeAsList(file);
    int referenceSize = referenceSegments.size();
    int fileSize = fileSegments.size();
    int i = 0;
    while (i < referenceSize && i < fileSize &&
        referenceSegments.get(i).equals(fileSegments.get(i))) {
      ++i;
    }
    ArrayList<String> segments = new ArrayList<String>();
    for (int j = i; j < referenceSize; ++j) {
      segments.add("..");
    }
    for (int j = i; j < fileSize; ++j) {
      segments.add(fileSegments.get(j));
    }
    return new File(join(segments));
  }
  
}
