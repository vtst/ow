package net.vtst.ow.eclipse.js.closure.launching.linter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.vtst.eclipse.easy.ui.launching.EasyExtProgramLaunchConfigurationDelegate;
import net.vtst.eclipse.easy.ui.launching.EasyLaunchConfigurationDelegateUtils.IProcessListenerAcceptor;
import net.vtst.eclipse.easy.ui.launching.EasyPatternMatchListener;
import net.vtst.eclipse.easy.ui.properties.stores.IReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.LaunchConfigurationReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.ProjectPropertyStore;
import net.vtst.ow.eclipse.js.closure.OwJsClosureMessages;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;
import net.vtst.ow.eclipse.js.closure.compiler.ClosureCompiler;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.console.FileLink;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.console.PatternMatchEvent;

public class ClosureLinterLaunchConfigurationDelegate extends EasyExtProgramLaunchConfigurationDelegate<ClosureLinterLaunchConfigurationDelegate.Fixture> {
  
  // TODO: No proper error message if the gjslint command is not correct.
  
  public static final String TYPE_ID = "net.vtst.ow.eclipse.js.closure.launching.linter";
  // TODO: Need to put a description text in plugin.xml!
  private static final String PROBLEM = "net.vtst.ow.eclipse.js.closure.linter-error";

  final OwJsClosureMessages messages = OwJsClosurePlugin.getDefault().getMessages();
  private ClosureLinterLaunchConfigurationRecord record = ClosureLinterLaunchConfigurationRecord.getInstance();
  
  class Fixture {}
  
  protected ProcessBuilder getProcessBuilder(ILaunchConfiguration config, Fixture fixture) throws CoreException {
    IReadOnlyStore store = new LaunchConfigurationReadOnlyStore(config);
    ProcessBuilder pb = super.getProcessBuilder(config, fixture);
    List<String> list = new ArrayList<String>();

    // Command
    if (record.fixLintErrors.get(store)) {
      list.add(record.fixjsstyleCommand.get(store));
    } else {
      list.add(record.gjslintCommand.get(store));
    }

    // Options
    List<IResource> resources = record.inputResources.get(store);
    if (resources.isEmpty()) {
      throw new CoreException(new Status(Status.ERROR, OwJsClosurePlugin.PLUGIN_ID, messages.getString("ClosureLinterLaunchConfigurationDelegate_noInputResource")));
    }
    IReadOnlyStore storeForLinterOptions = store;
    if (record.useProjectPropertiesForLinterChecks.get(store)) {
      IProject project = ClosureCompiler.getCommonProject(resources);
      if (project == null) throw new CoreException(new Status(Status.ERROR, OwJsClosurePlugin.PLUGIN_ID, messages.getString("ClosureLinterLaunchConfigurationDelegate_differentProjects")));
      store = new ProjectPropertyStore(project, OwJsClosurePlugin.PLUGIN_ID);
    }
    String customJsDocTags = record.linterChecks.customJsdocTags.get(storeForLinterOptions);
    if (customJsDocTags.length() > 0) {
      list.add("--custom_jsdoc_tags");
      list.add(customJsDocTags);
    }
    Set<String> lintErrorChecks = record.linterChecks.lintErrorChecks.get(storeForLinterOptions);
    for (String lintErrorCheck: lintErrorChecks) {
      list.add("--jslint_error");
      list.add(lintErrorCheck);
    }
    list.add(record.linterChecks.strictClosureStyle.get(storeForLinterOptions) ? "--strict" : "--nostrict");
    list.add(record.linterChecks.missingJsdoc.get(storeForLinterOptions) ? "--jsdoc" : "--nojsdoc");
    list.add("--nobeep");  // Otherwise we have a special character in the output
    
    // Files
    for (IFile file: ClosureCompiler.getJavaScriptFiles(resources)) {
      list.add(file.getLocation().toOSString());
      clearProblemMarkers(file);
    }
    pb.command(list);
    return pb;
  }
  
  private void clearProblemMarkers(IFile file) throws CoreException {
    for (IMarker marker : file.findMarkers(PROBLEM, false, 0)) {
      marker.delete();
    }
  }
  
  static private class PatternMatchListener extends EasyPatternMatchListener {

    private IFile file = null;
    private Pattern patternForFile = Pattern.compile("----- FILE  :  (.*) -----");
    private Pattern patternForError = Pattern.compile("(Line ([0-9]+), ([^:]*:[^:]*)): (.*)");
    private String message;
    
    private PatternMatchListener() {
      OwJsClosureMessages messages = OwJsClosurePlugin.getDefault().getMessages();
      message = messages.getString("ClosureLinterLaunchConfigurationDelegate_problemMarkerMessage");
    }
    
    @Override
    public void matchFound(PatternMatchEvent event) {
      try {
        processLine(event.getOffset(), event.getLength());
      } catch (BadLocationException e) {
        // This should never arise if the code is correct
      }
    }
    
    private void processLine(int offset, int length) throws BadLocationException {
      String line = document.get(offset, length);
      // We match errors first, because they should be the majority
      Matcher matcherForError = patternForError.matcher(line);
      if (matcherForError.matches()) {
        if (file != null) {
          int lineNumber = Integer.parseInt(matcherForError.group(2));
          String errorCode = matcherForError.group(3);
          String errorMessage = matcherForError.group(4);
          FileLink link = new FileLink(file, null, -1, -1, lineNumber);
          console.addHyperlink(link, offset, matcherForError.group(1).length());
          addProblemMarker(file, errorCode, errorMessage, lineNumber);
        }
        return;
      }
      Matcher matcherForFile = patternForFile.matcher(line);
      if (matcherForFile.matches()) {
        String fileName = matcherForFile.group(1);
        IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI((new File(fileName)).toURI());
        if (files.length == 0) file = null;
        else file = files[0];
        return;
      }
    } 
    
    private void addProblemMarker(IFile file, String errorCode, String errorMessage, int lineNumber) {
      try {
        IMarker marker = file.createMarker(PROBLEM);
        marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
        marker.setAttribute(IMarker.MESSAGE, String.format(message, errorCode, errorMessage));
        marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
      } catch (CoreException e) {
        e.printStackTrace();
      }
    }
    
    @Override
    public String getPattern() {
      return ".+";
    }
    
  }

  protected void addProcessListeners(ILaunchConfiguration config, Fixture fixture, IProcessListenerAcceptor acceptor) {
    acceptor.acceptPatternMatchListener(new PatternMatchListener());
  }
}
