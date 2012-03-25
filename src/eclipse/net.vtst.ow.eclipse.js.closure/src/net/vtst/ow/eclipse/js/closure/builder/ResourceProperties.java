package net.vtst.ow.eclipse.js.closure.builder;

import java.util.Collection;

import net.vtst.ow.closure.compiler.compile.CompilableJSUnit;
import net.vtst.ow.closure.compiler.deps.JSProject;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

/**
 * The plugin stores references to its own objects in session properties of resources,
 * in order to allow easy and quick retrieval.  This class provides abstract methods for
 * accessing these properties in a type safe way.
 * @author Vincent Simonet
 */
public class ResourceProperties {
  
  private static final QualifiedName JS_PROJECT = new QualifiedName(OwJsClosurePlugin.PLUGIN_ID, "JSProject");
  private static final QualifiedName JS_FILES = new QualifiedName(OwJsClosurePlugin.PLUGIN_ID, "Files");
  private static final QualifiedName JS_UNIT = new QualifiedName(OwJsClosurePlugin.PLUGIN_ID, "JSUnit");
  
  /**
   * Set the {@code JSProject} associated with a resource project.
   */
  public static void setJSProject(IProject project, JSProject jsProject) throws CoreException {
    project.setSessionProperty(JS_PROJECT, jsProject);
  }
  
  /**
   * Get the {@code JSProject} associated with a resource project, or null if there is no.
   */
  public static JSProject getJSProject(IProject project) throws CoreException {
    Object obj = project.getSessionProperty(JS_PROJECT);
    if (obj instanceof JSProject) return (JSProject) obj;
    return null;
  }
  
  /**
   * Set the collection of JavaScript files which are associated with a project. This is
   * a subset of the project's files that the plugin's builder handles as JavaScript files.
   */
  public static void setJavaScriptFiles(IProject project, Collection<IFile> files) throws CoreException {
    project.setSessionProperty(JS_FILES, files);
  }
  
  /**
   * Get the collection of JavaScript files which are associated with a project.  This
   * might be {@code null} or empty.
   */
  @SuppressWarnings("unchecked")
  public static Collection<IFile> getJavaScriptFiles(IProject project) throws CoreException {
    Object obj = project.getSessionProperty(JS_PROJECT);
    if (obj instanceof Collection<?>) return (Collection<IFile>) obj;
    return null;    
  }
  
  /**
   * Set the collection of JavaScript files which are associated with a project.
   */
  public static void setJSUnit(IFile file, CompilableJSUnit unit) throws CoreException {
    file.setSessionProperty(JS_UNIT, unit);
  }
  
  /**
   * Get the {@code CompilableJSUnit} which is associated with a (JavaScript) file,
   * or null if there is no.
   */
  public static CompilableJSUnit getJSUnit(IFile file) throws CoreException {
    Object obj = file.getSessionProperty(JS_UNIT);
    if (obj instanceof CompilableJSUnit) return (CompilableJSUnit) obj;
    return null;
  }

  /**
   * Set the {@code CompilableJSUnit} which is associated with a (JavaScript) file.
   */
  public static CompilableJSUnit getJSUnitOrNullIfCoreException(IFile file) {
    try {
      return getJSUnit(file);
    } catch (CoreException e) {
      return null;
    }
  }
}
