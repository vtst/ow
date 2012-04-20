package net.vtst.ow.eclipse.js.closure.builder;

import java.util.Collections;

import net.vtst.ow.eclipse.js.closure.compiler.ErrorManagerGeneratingProblemMarkers;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

/**
 * An error manager which generates problem markers for reporting errors.
 * @author Vincent Simonet
 */
public class ErrorManagerForProjectBuild extends ErrorManagerGeneratingProblemMarkers {
  
  private static final String PROBLEM = "net.vtst.ow.eclipse.js.closure.builder-error";
  
  private IProject project;
  
  public ErrorManagerForProjectBuild(IProject project) {
    this.project = project;
  }

  @Override
  protected Iterable<? extends IResource> getResources() {
    return Collections.singleton(project);
  }

  @Override
  protected IResource getResource(String sourceName) {
    return project;
  }

  @Override
  public String getMarkerType() {
    return PROBLEM;
  }
}
