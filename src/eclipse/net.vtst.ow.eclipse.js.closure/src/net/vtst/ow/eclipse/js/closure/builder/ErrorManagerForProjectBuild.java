package net.vtst.ow.eclipse.js.closure.builder;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.vtst.ow.eclipse.js.closure.compiler.ClosureCompiler;
import net.vtst.ow.eclipse.js.closure.compiler.ErrorManagerGeneratingProblemMarkers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

/**
 * An error manager which generates problem markers for reporting errors.
 * @author Vincent Simonet
 */
public class ErrorManagerForProjectBuild extends ErrorManagerGeneratingProblemMarkers {
  
  private static final String PROBLEM = "net.vtst.ow.eclipse.js.closure.builder-error";
  
  private IProject project;
  // This map is intended to cache files for successive calls to getResource, in order to avoid
  // doing multiple resolutions.
  private Map<String, IFile> cachedFiles = new HashMap<String, IFile>();
  
  public ErrorManagerForProjectBuild(IProject project) {
    this.project = project;
  }

  @Override
  protected void accept(IResourceVisitor visitor) throws CoreException {
    project.accept(visitor);
  }

  @Override
  protected IResource getResource(String sourceName) {
    if (cachedFiles.containsKey(sourceName)) return cachedFiles.get(sourceName);
    IFile file = ClosureCompiler.getFileFromSourceName(sourceName);
    cachedFiles.put(sourceName, file);
    return file;
  }

  @Override
  public String getMarkerType() {
    return PROBLEM;
  }
}
