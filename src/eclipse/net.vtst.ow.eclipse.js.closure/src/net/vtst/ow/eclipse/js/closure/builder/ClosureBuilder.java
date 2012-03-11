package net.vtst.ow.eclipse.js.closure.builder;

import java.util.Map;

import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class ClosureBuilder extends IncrementalProjectBuilder {

  public static final String BUILDER_ID = "net.vtst.ow.eclipse.js.closure.closureBuilder";
  
  public ClosureBuilder() {
    super();
  }

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
	 *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map args, IProgressMonitor monitor) throws CoreException {
	  IProject project = getProject();
	  System.out.println("Building " + project.getName());
		if (kind == FULL_BUILD) {
      OwJsClosurePlugin.getDefault().getProjectRegistry().fullUpdate(project);
		} else {
			IResourceDelta delta = getDelta(project);
			if (delta == null) {
			  OwJsClosurePlugin.getDefault().getProjectRegistry().fullUpdate(project);
			} else {
        OwJsClosurePlugin.getDefault().getProjectRegistry().incrementalUpdate(project, delta);
			}
		}
		return null;
	}

}
