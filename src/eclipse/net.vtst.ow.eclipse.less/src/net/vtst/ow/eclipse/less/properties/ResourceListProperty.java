package net.vtst.ow.eclipse.less.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

public class ResourceListProperty {
  @SuppressWarnings("unchecked")
  public static <T extends IResource> List<T> get(Class<? extends T> cls, IProject project, QualifiedName qualifiedName) throws CoreException {
    ArrayList<T> result = new ArrayList<T>();
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    for (String s: stringToStringList(project.getPersistentProperty(qualifiedName))) {
      IResource resource = root.findMember(s);
      if (cls.isAssignableFrom(resource.getClass())) result.add((T) resource);
    }
    return result;
  }
  
  public static <T extends IResource> void set(IProject project, QualifiedName qualifiedName, List<T> folders) throws CoreException {
    ArrayList<String> paths = new ArrayList<String>();
    for (T folder: folders) {
      paths.add(folder.getFullPath().toString());
    }
    project.setPersistentProperty(qualifiedName, stringListToString(paths));
  }

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
  
  public static List<String> stringToStringList(String value) {
    if (value == null || value.length() == 0) return Collections.emptyList();
    else return Arrays.asList(value.split(SEPARATOR));
  }

}
