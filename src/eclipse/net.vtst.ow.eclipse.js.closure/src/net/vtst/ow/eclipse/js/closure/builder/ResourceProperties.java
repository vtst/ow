package net.vtst.ow.eclipse.js.closure.builder;

import java.util.Collection;

import net.vtst.ow.closure.compiler.compile.CompilableJSUnit;
import net.vtst.ow.closure.compiler.deps.JSProject;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

public class ResourceProperties {
  
  private static final QualifiedName JS_PROJECT = new QualifiedName(OwJsClosurePlugin.PLUGIN_ID, "JSProject");
  private static final QualifiedName JS_FILES = new QualifiedName(OwJsClosurePlugin.PLUGIN_ID, "Files");
  private static final QualifiedName JS_UNIT = new QualifiedName(OwJsClosurePlugin.PLUGIN_ID, "JSUnit");
  
  public static void setJSProject(IProject project, JSProject jsProject) throws CoreException {
    project.setSessionProperty(JS_PROJECT, jsProject);
  }
  
  public static JSProject getJSProject(IProject project) throws CoreException {
    Object obj = project.getSessionProperty(JS_PROJECT);
    if (obj instanceof JSProject) return (JSProject) obj;
    return null;
  }
  
  public static void setJavaScriptFiles(IProject project, Collection<IFile> files) throws CoreException {
    project.setSessionProperty(JS_FILES, files);
  }
  
  @SuppressWarnings("unchecked")
  public static Collection<IFile> getJavaScriptFiles(IProject project) throws CoreException {
    Object obj = project.getSessionProperty(JS_PROJECT);
    if (obj instanceof Collection<?>) return (Collection<IFile>) obj;
    return null;    
  }
  
  public static void setJSUnit(IFile file, CompilableJSUnit unit) throws CoreException {
    file.setSessionProperty(JS_UNIT, unit);
  }
  
  public static CompilableJSUnit getJSUnit(IFile file) throws CoreException {
    Object obj = file.getSessionProperty(JS_UNIT);
    if (obj instanceof CompilableJSUnit) return (CompilableJSUnit) obj;
    return null;
  }

  public static CompilableJSUnit getJSUnitOrNullIfCoreException(IFile file) {
    try {
      return getJSUnit(file);
    } catch (CoreException e) {
      return null;
    }
  }
}
