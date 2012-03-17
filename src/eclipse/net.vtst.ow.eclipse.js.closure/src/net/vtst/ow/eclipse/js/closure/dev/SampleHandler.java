package net.vtst.ow.eclipse.js.closure.dev;

import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class SampleHandler extends AbstractHandler {
	/**
	 * The constructor.
	 */
	public SampleHandler() {
	}

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
	  IPath path = new Path("test/test.js");
	  IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
	  //OwJsClosurePlugin.getDefault().getProjectRegistry().compile(file);
		return null;
	}
}
