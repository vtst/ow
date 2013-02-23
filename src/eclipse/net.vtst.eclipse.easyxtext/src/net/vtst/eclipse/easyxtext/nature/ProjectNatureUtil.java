package net.vtst.eclipse.easyxtext.nature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
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

  // TODO: Could we re-use the constant from XText?
  private static String XTEXT_NATURE_ID = "org.eclipse.xtext.ui.shared.xtextNature";

  public static void addNatureRequiringXtext(String natureId, IProject project) throws CoreException {
    Collection<String> natureIds = new ArrayList<String>(2);
    natureIds.add(XTEXT_NATURE_ID);
    natureIds.add(natureId);
    addNatures(natureIds, project);
  }

  public static void removeNatureRequiringXtext(String natureId, boolean alsoRemoveXtextNature, IProject project) throws CoreException {
    Collection<String> natureIds = new ArrayList<String>(2);
    if (alsoRemoveXtextNature) natureIds.add(XTEXT_NATURE_ID);
    natureIds.add(natureId);
    removeNatures(natureIds, project);
  }
}
