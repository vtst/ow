package net.vtst.eclipse.easyxtext.nature;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

public class ProjectNatureUtil {
  
  public static boolean hasNature(String natureId, IProject project) throws CoreException {
    for (String n : project.getDescription().getNatureIds()) {
      if (natureId.equals(n)) return true;
    }
    return false;
  }
  
  public static void addNatures(Collection<String> natureIds, IProject project) throws CoreException {
    IProjectDescription description = project.getDescription();
    String[] currentNatureIds = description.getNatureIds();
    Set<String> newNatureIds = new HashSet<String>(natureIds.size() + currentNatureIds.length);
    for (String n : currentNatureIds) newNatureIds.add(n);
    newNatureIds.addAll(natureIds);
    description.setNatureIds(newNatureIds.toArray(new String[0]));
    project.setDescription(description, null);
  }
  
  public static void removeNatures(Collection<String> natureIds, IProject project) throws CoreException {
    IProjectDescription description = project.getDescription();
    String[] currentNatureIds = description.getNatureIds();
    Set<String> newNatureIds = new HashSet<String>(currentNatureIds.length);
    for (String n : currentNatureIds) newNatureIds.add(n);
    newNatureIds.removeAll(natureIds);
    description.setNatureIds(newNatureIds.toArray(new String[0]));
    project.setDescription(description, null);    
  }
  
  public static void toggleNature(String natureId, IProject project) throws CoreException {
    IProjectDescription description = project.getDescription();
    String[] natures = description.getNatureIds();

    for (int i = 0; i < natures.length; ++i) {
      if (natureId.equals(natures[i])) {
        // Remove the nature
        String[] newNatures = new String[natures.length - 1];
        System.arraycopy(natures, 0, newNatures, 0, i);
        System.arraycopy(natures, i + 1, newNatures, i, natures.length - i - 1);
        description.setNatureIds(newNatures);
        project.setDescription(description, null);
        return;
      }
    }

    // Add the nature
    String[] newNatures = new String[natures.length + 1];
    System.arraycopy(natures, 0, newNatures, 0, natures.length);
    newNatures[natures.length] = natureId;
    description.setNatureIds(newNatures);
    project.setDescription(description, null);
  }
}
