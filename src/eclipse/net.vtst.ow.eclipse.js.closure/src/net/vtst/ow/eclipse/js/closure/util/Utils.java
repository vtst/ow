package net.vtst.ow.eclipse.js.closure.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

public class Utils {
  
  /**
   * Get the first element of a collection.
   * @param collection  The collection.
   * @return  The first element, or null if the collection is empty.
   */
  public static <V> V getFirstElement(Collection<V> collection) {
    for (V element: collection) return element;
    return null;
  }

  /**
   * Join an array of string into a single string.
   * @param strings  The array of strings to join.
   * @param separator  The string to put between two joined strings.
   * @return  The joined string.
   */
  public static String join(String[] strings, String separator) {
    int n = separator.length() * Math.max(0, strings.length - 1);
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
  
  /**
   * @param flags  An array without duplicate strings.
   * @param flag  The flag to insert or remove.
   * @param status  If true, ensure the flag is present.  If false, ensure the flag is absent.
   * @return  A new array, or null if the array does not need to be changed.
   */
  private static String[] setFlagInArrayOfFlags(String[] flags, String flag, boolean status) {
    int i;
    for (i = 0; i < flags.length; ++i) {
      if (flags[i].equals(flag)) break;
    }
    if (status == (i < flags.length)) return null;
    String[] newNatures;
    if (status) {
      newNatures = new String[flags.length + 1];
      System.arraycopy(flags, 0, newNatures, 0, flags.length);
      newNatures[flags.length] = flag;
    } else {
      newNatures = new String[flags.length - 1];
      System.arraycopy(flags, 0, newNatures, 0, i);
      System.arraycopy(flags, i + 1, newNatures, i, flags.length - i - 1);
    }
    return newNatures;
  }
  
  /**
   * Set or remove a nature to a project.
   * @param project  The project.
   * @param nature  The ID of the nature.
   * @param status  true to set the nature, false to remove it.
   * @throws CoreException
   */
  public static void setProjectNature(IProject project, String nature, boolean status) throws CoreException {
    IProjectDescription description = project.getDescription();
    String[] newNatures = setFlagInArrayOfFlags(description.getNatureIds(), nature, status);
    if (newNatures != null) {
      description.setNatureIds(newNatures);
      project.setDescription(description, null);
    }
  }

  /**
   * Set or remove a builder to a project.
   * @param project  The project.
   * @param builderName  The ID of the builder.
   * @param status  true to set the builder, false to remove it.
   * @throws CoreException
   */
  public static void setProjectBuilder(IProject project, String builderName, boolean status) throws CoreException {
    IProjectDescription description = project.getDescription();    
    ICommand[] commands = description.getBuildSpec();
    int i;
    for (i = 0; i < commands.length; ++i) {
      if (commands[i].getBuilderName().equals(builderName)) break;
    }
    if (status == (i < commands.length)) return;
    ICommand[] newCommands;
    if (status) {
      newCommands = new ICommand[commands.length + 1];
      System.arraycopy(commands, 0, newCommands, 0, commands.length);
      ICommand command = description.newCommand();
      command.setBuilderName(builderName);
      newCommands[commands.length] = command;
    } else {
      newCommands = new ICommand[commands.length - 1];
      System.arraycopy(commands, 0, newCommands, 0, i);
      System.arraycopy(commands, i + 1, newCommands, i, commands.length - i - 1);
    }
    description.setBuildSpec(newCommands);
    project.setDescription(description, null);
  }
  
  /**
   * Get the function node for a node.
   * @param node
   * @return
   */
  public static Node getFunctionNode(Node node) {
    Node parent = node.getParent();
    if (node.getType() == Token.FUNCTION || parent == null) return node;
    if (parent.getType() == Token.FUNCTION) return parent;
    if (parent.getType() == Token.ASSIGN && parent.getChildCount() > 1) 
      return parent.getChildAtIndex(1);
    return null;
  } 
  
  public static boolean contains(char[] array, char c) {
    for (char d: array) {
      if (d == c) return true;
    }
    return false;
  }
  
  /**
   * Test whether a progress monitor has been canceled, and if so, throws an exception.
   * @param monitor
   * @throws OperationCanceledException
   */
  public static void checkCancel(IProgressMonitor monitor) {
    if (monitor.isCanceled()) throw new OperationCanceledException();
  }
  
  /**
   * @param resources  A series of resources.
   * @param files  A set of files.
   * @return  A set of all the files which are in <code>resources</code> or contained in a resource from <code>resources</code>
   *   and in <code>files</code>.
   * @throws CoreException
   */
  public static Set<IFile> getAllContainedFilesWhichAreInSet(Iterable<IResource> resources, final Set<IFile> files) throws CoreException {
    final Set<IFile> result = new HashSet<IFile>(files.size());
    for (IResource resource: resources) {
      resource.accept(new IResourceVisitor() {
        public boolean visit(IResource resource) throws CoreException {
          if (resource instanceof IFile) {
            IFile file = (IFile) resource;
            if (files.contains(file)) result.add(file);
            return false;
          } else {
            return true;
          }
        }});
    }
    return result;
  }
}
