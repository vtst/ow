package net.vtst.ow.eclipse.js.closure.builder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import net.vtst.ow.eclipse.js.closure.dev.OwJsDev;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspace.ProjectOrder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

/**
 * Helper class for managing a global order for JavaScript projects having the closure
 * nature.  This order is shared between all builders.
 * <br>
 * <b>Thread safety:</b>  This class is synchronized, in order to ensure safe 
 * concurrent accesses.
 * @author Vincent Simonet
 */
public class ProjectOrderManager implements IResourceChangeListener {
  
  public ProjectOrderManager() {
  }
  
  public static class State {
    private HashMap<IProject, Integer> projectToIndex;
    private boolean dirty = false;
    private long modificationStamp;
    private State(HashMap<IProject, Integer> projectToIndex) {
      this.projectToIndex = projectToIndex;
      this.modificationStamp = System.nanoTime();
    }
    
    public int getProjectIndex(IProject project) { 
      Integer index = projectToIndex.get(project);
      if (index == null) return -1;
      return index.intValue();
    }
    
    private boolean containsProject(IProject project) {
      return projectToIndex.containsKey(project);
    }

    public Comparator<IProject> reverseOrderComparator() {
      return new Comparator<IProject>() {
        @Override
        public int compare(IProject project0, IProject project1) {
          return getProjectIndex(project1) - getProjectIndex(project0);
        }};
    }
    
    public long getModificationStamp() { return modificationStamp; }
  }
  
  private State state;

  public synchronized State update() throws CoreException {
    State localState = this.state;
    if (localState != null && !localState.dirty) return localState;
    OwJsDev.log("Computing project order");
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IProject[] projects = workspace.getRoot().getProjects();
    ArrayList<IProject> projectsWithNature = new ArrayList<IProject>(projects.length);
    for (IProject project: projects) {
      if (project.hasNature(ClosureNature.NATURE_ID)) {
        projectsWithNature.add(project);
      }
    }
    ProjectOrder order = workspace.computeProjectOrder(projectsWithNature.toArray(projects));
    HashMap<IProject, Integer> projectToIndex = new HashMap<IProject, Integer>();
    for (int i = 0; i < order.projects.length; ++i) {
      projectToIndex.put(order.projects[i], i);
    }
    localState = new State(projectToIndex);
    this.state = localState;
    return localState;
  }
  
  public synchronized void clear() {
    state = null;
  }
  
  public State get() throws CoreException {
    State state = this.state;
    if (state == null || state.dirty) return update();
    else return state;
  }
  
  // **************************************************************************
  // Resource change listener
  
  /**
   * Resource delta visitor that sets the dirty bit of a state if needed.
   */
  private static class ResourceDeltaVisitor implements IResourceDeltaVisitor {
    
    private State state;

    private ResourceDeltaVisitor(State state) {
      this.state = state;
    }

    @Override
    public boolean visit(IResourceDelta delta) throws CoreException {
      IResource resource = delta.getResource();
      if (resource instanceof IProject) {
        IProject project = (IProject) resource;
        int kind = delta.getKind();
        if ((kind & IResourceDelta.CHANGED) != 0) {
          int flags = delta.getFlags();
          if ((flags & (IResourceDelta.DESCRIPTION | IResourceDelta.OPEN | IResourceDelta.REPLACED | IResourceDelta.LOCAL_CHANGED)) != 0)
            this.state.dirty = true;
        } else if ((kind & IResourceDelta.ADDED) != 0) {
          if (project.hasNature(ClosureNature.NATURE_ID)) this.state.dirty = true;
        } else if ((kind & IResourceDelta.REMOVED) != 0) {
          if (this.state.containsProject(project)) this.state.dirty = true;
        }
        return false;
      } else {
        return true;
      }
    }
    
  }

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    State localState = state;
    if (localState.dirty) return;
    ResourceDeltaVisitor visitor = new ResourceDeltaVisitor(localState);
    try {
      event.getDelta().accept(visitor);
      if (localState.dirty) OwJsDev.log("Project order state is dirty");
    } catch (CoreException e) {}
  }
  
}
