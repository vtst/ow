package net.vtst.ow.eclipse.less.properties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

import com.google.inject.Singleton;

@Singleton
public class LessProjectProperty {

  public static String QUALIFIER = "net.vtst.ow.less";
  public static String INCLUDE_PATHS = "includePaths";
  
  private Map<IProject, Iterable<IContainer>> includePaths = new HashMap<IProject, Iterable<IContainer>>();
  
  public LessProjectProperty() {
    ResourcesPlugin.getWorkspace().addResourceChangeListener(new IResourceChangeListener(){
      public void resourceChanged(IResourceChangeEvent event) {
        IResource resource = event.getResource();
        if (resource instanceof IProject) {
          System.out.println("*** DELETE " + resource.getName());
          includePaths.remove((IProject) resource);
        }
      }});
  }
  
  public Iterable<IContainer> getIncludePaths(IProject project) {
    Iterable<IContainer> result = includePaths.get(project);
    if (result == null) {
      try {
        result = ResourceListProperty.<IContainer>get(IContainer.class, project, new QualifiedName(QUALIFIER, INCLUDE_PATHS));
      } catch (CoreException e) {
        return Collections.<IContainer>emptyList();
      }
      includePaths.put(project, result);
    }
    return result;
  }
  
}
