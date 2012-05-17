package net.vtst.ow.eclipse.js.closure.builder;

import java.util.HashMap;
import java.util.Map;

import net.vtst.ow.closure.compiler.deps.JSUnit;
import net.vtst.ow.eclipse.js.closure.compiler.ErrorManagerGeneratingProblemMarkers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;

/**
 * An error manager which generates problem markers for reporting errors.
 * @author Vincent Simonet
 */
public class ErrorManagerForFileBuild extends ErrorManagerGeneratingProblemMarkers {
  
  private static final String PROBLEM = "net.vtst.ow.eclipse.js.closure.compiler-error";
  
  private Map<String, IFile> fileNameToFile = new HashMap<String, IFile>();
  
  public ErrorManagerForFileBuild(JSUnit unit, IFile file) {
    fileNameToFile.put(unit.getName(), file);
  }

  @Override
  protected void accept(IResourceVisitor visitor) throws CoreException {
    for (IFile file: fileNameToFile.values()) {
      file.accept(visitor);
    }
  }

  @Override
  protected IResource getResource(String sourceName) {
    return fileNameToFile.get(sourceName);
  }

  @Override
  public String getMarkerType() {
    return PROBLEM;
  }
}
