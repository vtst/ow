package net.vtst.ow.eclipse.js.closure.properties;

import net.vtst.ow.eclipse.js.closure.util.Utils;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

public class ClosureProjectPersistentPropertyHelper {
  
  // TODO: Implement variables in paths.
  // TODO: Make paths independent of the filesystem?
  
  private static final String QUALIFIER = "net.vtst.ow.eclipse.js.closure";
  private static final String CLOSURE_BASE_DIR = "closureBaseDir";
  private static final String OTHER_LIBRARIES = "otherLibraries";
  private static final String LIST_SEPARATOR = "\n";

  private IResource resource;

  public ClosureProjectPersistentPropertyHelper(IResource resource) {
    this.resource = resource;
  }
  
  public void setClosureBaseDir(String value) throws CoreException {
    resource.setPersistentProperty(new QualifiedName(QUALIFIER, CLOSURE_BASE_DIR), value);
  }
  
  public String getClosureBaseDir() throws CoreException {
    return resource.getPersistentProperty(new QualifiedName(QUALIFIER, CLOSURE_BASE_DIR));
  }
  
  public void setOtherLibraries(String[] values) throws CoreException {
    resource.setPersistentProperty(
        new QualifiedName(QUALIFIER, OTHER_LIBRARIES), Utils.join(values, LIST_SEPARATOR));
  }

  public String[] getOtherLibraries() throws CoreException {
    String value = resource.getPersistentProperty(new QualifiedName(QUALIFIER, OTHER_LIBRARIES));
    if (value == null || value.isEmpty()) return new String[0];
    return value.split(LIST_SEPARATOR);
  }
  
}
