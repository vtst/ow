// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.ui.launching;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.vtst.eclipse.easyxtext.ui.launching.EasyExtProgramLaunchConfigurationDelegate;
import net.vtst.eclipse.easyxtext.ui.launching.EasyLaunchConfigurationDelegateUtils.IProcessListenerAcceptor;
import net.vtst.eclipse.easyxtext.ui.launching.EasyLaunchConfigurationDelegateUtils.IProcessTerminationListener;
import net.vtst.eclipse.easyxtext.ui.launching.EasyPatternMatchListener;
import net.vtst.ow.eclipse.less.ui.LessUiMessages;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.console.FileLink;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

import com.google.inject.Inject;

public class LessCompilerLaunchConfigurationDelegate 
    extends EasyExtProgramLaunchConfigurationDelegate<LessCompilerLaunchConfigurationDelegate.Fixture> {

  private final static String MARKER_TYPE = IMarker.PROBLEM;
  private final static String MARKER_SOURCE_ID = "net.vtst.ow.eclipse.less.ui.LessCompilerMarker";

  @Inject 
  private LessCompilerLaunchConfigurationHelper launchConfigHelper;
  
  @Inject
  private LessUiMessages messages;

  // Use helper function
  protected IFile getInputFile(ILaunchConfiguration config) throws CoreException {
    try {
      return ResourcesPlugin.getWorkspace().getRoot().getFile(
          new Path(launchConfigHelper.inputFile.getValue(config)));
    } catch (IllegalArgumentException exn) {
      throw super.createCoreException(messages.getString("illegal_input_file"), exn);
    }
  }

  protected IFile getOutputFile(ILaunchConfiguration config) throws CoreException {
    try {
      String outputFile;
      if (launchConfigHelper.autoOutputFile.getBooleanValue(config)) {
        outputFile = launchConfigHelper.getAutoOutputFile(launchConfigHelper.inputFile.getValue(config));
      } else {
        outputFile = launchConfigHelper.outputFile.getValue(config);
      }
      return ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(outputFile));
    } catch (IllegalArgumentException exn) {
      throw super.createCoreException(messages.getString("illegal_output_file"), exn);
    }
  }
  
  public static class Fixture {
    public IFile inputFile;
    public IFile outputFile;
  }

  protected Fixture getFixture(ILaunchConfiguration config) throws CoreException {
    Fixture fixture = new Fixture();
    fixture.inputFile = getInputFile(config);
    fixture.outputFile = getOutputFile(config);
    return fixture;
  }

  protected ProcessBuilder getProcessBuilder(ILaunchConfiguration config, Fixture fixture) throws CoreException {
    ProcessBuilder pb = super.getProcessBuilder(config, fixture);
    List<String> list = new ArrayList<String>();
    list.add(launchConfigHelper.command.getValue(config));
    list.add(fixture.inputFile.getRawLocation().makeAbsolute().toOSString());
    list.add(fixture.outputFile.getRawLocation().makeAbsolute().toOSString());
    if (launchConfigHelper.verbose.getBooleanValue(config)) list.add("--verbose");
    if (launchConfigHelper.compress.getBooleanValue(config)) list.add("--compress");
    if (launchConfigHelper.yuiCompress.getBooleanValue(config)) list.add("--yui-compress");
    if (launchConfigHelper.strictImports.getBooleanValue(config)) list.add("--strict-imports");
    list.add("-O" + launchConfigHelper.optimization.getValue(config));
    list.add("--no-color");
    pb.command(list);
    return pb;
  }
  
  protected void addProcessListeners(
      ILaunchConfiguration config, 
      final Fixture fixture, 
      IProcessListenerAcceptor acceptor) {
    acceptor.acceptTerminationListener(new IProcessTerminationListener(){
      public void terminated(IProcess process, int exitValue) {
        IConsole console = DebugUITools.getConsole(process);
        if (console instanceof IOConsole) {
          IOConsole ioConsole = (IOConsole) console;
          IOConsoleOutputStream stream = ioConsole.newOutputStream();
          if (exitValue == 0) {
            try {
              stream.write(messages.getString("less_compiler_success"));
              stream.write("\n");
              stream.flush();
            } catch (IOException e) {
              e.printStackTrace();
            }
            /*
            FileLink link = new FileLink(destinationFile, null, -1, -1, -1);
            console.defaultStream.hyperlink(link, messages.getString("less_link_to_output"));
            console.defaultStream.println();
            */
          } else {
            try {
              stream.write(messages.getString("less_compiler_error"));
              stream.write("\n");
              stream.flush();
            } catch (IOException e) {
              e.printStackTrace();
            }
          } 
        }
        try { fixture.outputFile.refreshLocal(0, null); } catch (CoreException e) {}
      }});
    acceptor.acceptPatternMatchListener(new EasyPatternMatchListener(){
      private Pattern pattern = Pattern.compile("([0-9]+)( .*)");
      private int numberOfLines = 0;
      private int errorLineNumber = 0;
      private String errorMessage = null;
      @Override
      public void connect(TextConsole console) {
        super.connect(console);
        try {
          for (IMarker marker: fixture.inputFile.findMarkers(MARKER_TYPE, true, IResource.DEPTH_ZERO)) {
            if (MARKER_SOURCE_ID.equals(marker.getAttribute(IMarker.SOURCE_ID))) {
              marker.delete();
            }
          }
        } catch (CoreException e) {}
      }
      @Override
      public void disconnect() {
        if (errorLineNumber != 0 && errorMessage != null) {
          try {
            IMarker marker = fixture.inputFile.createMarker(MARKER_TYPE);
            marker.setAttribute(IMarker.LINE_NUMBER, this.errorLineNumber);
            marker.setAttribute(IMarker.MESSAGE, this.errorMessage);
            marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_NORMAL);
            marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
            marker.setAttribute(IMarker.SOURCE_ID, MARKER_SOURCE_ID);
          } catch (CoreException e) { e.printStackTrace(); }
        }
      }

      private void processLine(int offset, int length) throws BadLocationException {
        String line = document.get(offset, length);
        Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) {
          this.numberOfLines++;
          int lineNumber = Integer.parseInt(matcher.group(1));
          if (this.numberOfLines <= 2) errorLineNumber = lineNumber;
          FileLink link = new FileLink(fixture.inputFile, null, -1, -1, lineNumber);
          console.addHyperlink(link, offset, matcher.group(1).length());
        } else {
          if (this.numberOfLines == 0) errorMessage = line;
        }
      }
      
      @Override
      public void matchFound(PatternMatchEvent matchEvent) {
        try {
          processLine(matchEvent.getOffset(), matchEvent.getLength());
        } catch (BadLocationException e) {
          // This should never arise if the code is correct
        }
      }

      @Override
      public String getPattern() {
        return ".+";
      }});
  }
  
}
