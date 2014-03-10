package net.vtst.ow.eclipse.less.properties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

import com.google.inject.Singleton;

@Singleton
public class LessProjectProperty {

  public static String QUALIFIER = "net.vtst.ow.less";
  public static String INCLUDE_PATHS = "includePaths";
  public static String ROOTS = "roots";
  
  private Map<IProject, Iterable<IContainer>> includePaths = new HashMap<IProject, Iterable<IContainer>>();
  private Map<IProject, Iterable<IFile>> roots = new HashMap<IProject, Iterable<IFile>>();
  
  public LessProjectProperty() {
    ResourcesPlugin.getWorkspace().addResourceChangeListener(new IResourceChangeListener(){
      public void resourceChanged(IResourceChangeEvent event) {
        IResource resource = event.getResource();
        if (resource instanceof IProject) {
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
  
  public Iterable<IFile> getRoots(IProject project) {
    Iterable<IFile> result = roots.get(project);
    if (result == null) {
      try {
        result = ResourceListProperty.<IFile>get(IFile.class, project, new QualifiedName(QUALIFIER, ROOTS));
      } catch (CoreException e) {
        return Collections.<IFile>emptyList();
      }
      roots.put(project, result);
    }
    return result;    
  }
  
  /**
   * Convert EMF URI to Eclipse file
   */
  @SuppressWarnings("deprecation")
  public static IFile getFile(URI uri) {
    if (uri.isPlatform()) {
      return ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(uri.toPlatformString(true)));
    } else if (uri.isFile()) {
      for (IFile file: ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(new Path(uri.toFileString()))) {
        return file;
      }
    }
    return null;
  }

  public static IProject getProject(Resource resource) {
    IFile file = getFile(resource.getURI());
    if (file != null) return file.getProject();
    else return null;
  }
  
}
