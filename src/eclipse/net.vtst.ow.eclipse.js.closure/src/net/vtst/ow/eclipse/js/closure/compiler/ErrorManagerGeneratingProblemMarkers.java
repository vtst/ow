package net.vtst.ow.eclipse.js.closure.compiler;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import net.vtst.ow.closure.compiler.deps.JSUnit;

import com.google.javascript.jscomp.BasicErrorManager;
import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.JSError;

/**
 * An error manager which generates problem markers for reporting errors.
 * @author Vincent Simonet
 */
public class ErrorManagerGeneratingProblemMarkers extends BasicErrorManager {
  
  private static final String PROBLEM = "net.vtst.ow.eclipse.js.closure.compiler-error";
  
  private Map<String, IFile> fileNameToFile = new HashMap<String, IFile>();
  
  public ErrorManagerGeneratingProblemMarkers(JSUnit unit, IFile file) {
    fileNameToFile.put(unit.getName(), file);
  }
  
  private void clearProblemMarkers() throws CoreException {
    for (IFile file: fileNameToFile.values()) {
      for (IMarker marker : file.findMarkers(PROBLEM, false, 0)) {
        marker.delete();
      }
    }
  }

  public void generateReport() {
    try {
      clearProblemMarkers();
    } catch (CoreException e) {}
    super.generateReport();
  }
  
  @Override
  protected void printSummary() {
  }

  @Override
  public void println(CheckLevel level, JSError error) {
    IFile file = fileNameToFile.get(error.sourceName);
    if (file == null) return;
    try {
      IMarker marker = file.createMarker(PROBLEM);
      marker.setAttribute(IMarker.SEVERITY, checkLevelToSeverity(level));
      marker.setAttribute(IMarker.MESSAGE, error.description);
      marker.setAttribute(IMarker.LINE_NUMBER, error.lineNumber);
      // error.getCharno() for the char number in the line
    } catch (CoreException e) {
      e.printStackTrace();
    }
  }
  
  private int checkLevelToSeverity(CheckLevel level) {
    switch (level) {
    case ERROR: return IMarker.SEVERITY_ERROR;
    case WARNING: return IMarker.SEVERITY_WARNING;
    case OFF: default: return IMarker.SEVERITY_INFO;
    }
  }

}
