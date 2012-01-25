package net.vtst.ow.eclipse.soy.ui.launching;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.vtst.eclipse.easyxtext.ui.launching.EasyJavaProgramLaunchConfigurationDelegate;
import net.vtst.eclipse.easyxtext.ui.launching.EasyLaunchConfigurationDelegateUtils.IProcessListenerAcceptor;
import net.vtst.eclipse.easyxtext.ui.launching.EasyLaunchConfigurationDelegateUtils.IProcessTerminationListener;
import net.vtst.eclipse.easyxtext.ui.launching.EasyPatternMatchListener;
import net.vtst.eclipse.easyxtext.util.Misc;
import net.vtst.ow.eclipse.soy.ui.SoyUiMessages;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

import com.google.inject.Inject;

// return "com.google.template.soy.SoyToJsSrcCompiler";

public class SoyCompilerLaunchConfigurationDelegate extends EasyJavaProgramLaunchConfigurationDelegate<SoyCompilerLaunchConfigurationDelegate.Fixture> {
  
  private final static String MARKER_TYPE = IMarker.PROBLEM;
  private final static String MARKER_SOURCE_ID = "net.vtst.ow.eclipse.soy.ui.SoyCompilerMarker";

  @Inject
  SoyCompilerLaunchConfigurationHelper configHelper;
  
  @Inject
  SoyUiMessages messages;
  
  static class Fixture {
    IFile inputFile;
  }
  
  protected Fixture getFixture(ILaunchConfiguration config) throws CoreException {
    Fixture fixture = new Fixture();
    fixture.inputFile = configHelper.inputFile.getFileValue(config);
    return fixture;
  }

  @Override
  public String getMainTypeName(ILaunchConfiguration configuration) {
    return "com.google.template.soy.SoyToJsSrcCompiler";
  }
  
  @Override
  protected String[] getProgramArgumentsArray(ILaunchConfiguration config, Fixture fixture) throws CoreException {
    String[] superArguments = super.getProgramArgumentsArray(config, fixture);
    ArrayList<String> args = new ArrayList<String>();
    args.add("--bidiGlobalDir");
    args.add("rtl".equals(configHelper.bidiGlobalDir.getValue(config)) ? "-1" : "1");
    args.add("--codeStyle");
    args.add(configHelper.codeStyle.getValue(config));
    args.add("--cssHandlingScheme");
    args.add(configHelper.cssHandlingScheme.getValue(config));
    if (configHelper.isUsingIjData.getBooleanValue(config)) args.add("--isUsingIjData");
    if (configHelper.shouldDeclareTopLevelNamespaces.getBooleanValue(config)) args.add("--shouldDeclareTopLevelNamespaces");
    if (configHelper.shouldGenerateJsdoc.getBooleanValue(config)) args.add("--shouldGenerateJsdoc");
    if (configHelper.shouldProvideRequireSoyNamespaces.getBooleanValue(config)) args.add("--shouldProvideRequireSoyNamespaces");
    String compileTimeGlobalsFile = configHelper.getCompileTimeGlobalsFile(config);
    if (compileTimeGlobalsFile.length() > 0) {
      args.add("--compileTimeGlobalsFile");
      args.add(compileTimeGlobalsFile);
    }
    args.add("--outputPathFormat");
    if (configHelper.localize.getBooleanValue(config)) {
      args.add(configHelper.outputPathFormatLocalized.getValue(config));
      args.add("--locales");
      args.add(configHelper.locales.getValue(config));  // TODO What to do if empty?
      args.add("--messageFilePathFormat");
      args.add(configHelper.getMessageFileFormat(config));
    } else {
      args.add(configHelper.outputPathFormat.getValue(config));
    }
    
    args.add(fixture.inputFile.getLocation().makeAbsolute().toOSString());
    return Misc.addListToArray(superArguments, args);
  }
  
  @Override
  protected void addProcessListeners(
      final ILaunchConfiguration config, 
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
              stream.write(messages.getString("soy_compiler_success"));
              stream.write("\n");
              stream.flush();
              for (IFile file: configHelper.getOutputFiles(config)) {
                try { file.refreshLocal(0, null); } catch (CoreException e) {}
              }
            } catch (IOException e) {
              e.printStackTrace();
            }
          } else {
            try {
              stream.write(messages.getString("soy_compiler_error"));
              stream.write("\n");
              stream.flush();
            } catch (IOException e) {
              e.printStackTrace();
            }
          } 
        }
        //try { fixture.outputFile.refreshLocal(0, null); } catch (CoreException e) {}
      }});
    acceptor.acceptPatternMatchListener(new EasyPatternMatchListener(){
      private Pattern pattern = Pattern.compile(
          "Exception [^:]*com.google.template.soy.base.SoySyntaxException: " +
          "(In file ([^:]*)(:([0-9]+))?(, template [^:]*)?: )?(.*)");
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

      private void processLine(int offset, int length) throws BadLocationException {
        String line = document.get(offset, length);
        Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) {
          String lineNumberAsString = matcher.group(4);
          int lineNumber = (lineNumberAsString == null ? 1 : Integer.parseInt(lineNumberAsString));
          String errorMessage = matcher.group(6);
          IMarker marker;
          try {
            marker = fixture.inputFile.createMarker(MARKER_TYPE);
            marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
            marker.setAttribute(IMarker.MESSAGE, errorMessage);
            marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_NORMAL);
            marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
            marker.setAttribute(IMarker.SOURCE_ID, MARKER_SOURCE_ID);
          } catch (CoreException e) {
            e.printStackTrace();
          }
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
