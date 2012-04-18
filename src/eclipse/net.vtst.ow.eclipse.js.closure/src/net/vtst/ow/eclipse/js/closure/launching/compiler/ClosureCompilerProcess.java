package net.vtst.ow.eclipse.js.closure.launching.compiler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.console.FileLink;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IPatternMatchListener;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

import com.google.javascript.jscomp.BasicErrorManager;
import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.ErrorFormat;
import com.google.javascript.jscomp.ErrorManager;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.MessageFormatter;

public class ClosureCompilerProcess implements IProcess {
  
  private ILaunch launch;
  private boolean terminated = false;

  public ClosureCompilerProcess(ILaunch launch) {
    this.launch = launch;
    launch.addProcess(this);
    IConsole console = DebugUITools.getConsole(this);
    if (console instanceof TextConsole)
      ((TextConsole) console).addPatternMatchListener(patternMatchListener);
    else
      System.out.println("CONSOLE NOT FOUND");
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Object getAdapter(Class arg0) { return null; }

  @Override
  public boolean canTerminate() { return false; }

  @Override
  public boolean isTerminated() {
    return terminated;
  }

  @Override
  public void terminate() throws DebugException {}

  @Override
  public String getAttribute(String arg0) { return null; }

  @Override
  public int getExitValue() throws DebugException { return 0; }

  @Override
  public String getLabel() {
    return "closure-compiler-output";
  }

  @Override
  public ILaunch getLaunch() { return launch; }

  @Override
  public IStreamsProxy getStreamsProxy() { return streamsProxy; }
  
  public ErrorManager getErrorManager() { return errorManager; }
  
  public void setTerminated() {
    terminated = true;
  }

  @Override
  public void setAttribute(String name, String value) {}

  private static class ErrorInfo {
    JSError error;
    IFile file;
    String fileName;
    ErrorInfo(JSError error) {
      IFile[] errorFiles = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI((new File(error.sourceName)).toURI());
      if (errorFiles.length > 0) {
        this.file = errorFiles[0];
        this.fileName = errorFiles[0].getFullPath().toOSString();
        this.error = JSError.make(this.fileName, error.lineNumber, error.getCharno(), error.getDefaultLevel(), error.getType(), error.description);
      } else {
        this.file = null;
        this.fileName = null;
        this.error = error;
      }
    }
  }
  
  private class StreamMonitorErrorManager extends BasicErrorManager implements IStreamMonitor {
    
    private MessageFormatter formatter;
    private ArrayList<ErrorInfo> errorInfos = new ArrayList<ErrorInfo>();

    StreamMonitorErrorManager(MessageFormatter formatter) {
      this.formatter = formatter;
    }
    
    StreamMonitorErrorManager() {
      this(ErrorFormat.SOURCELESS.toFormatter(null, false));
    }
    
    public ErrorInfo getErrorInfo(int i) {
      if (i < errorInfos.size()) return errorInfos.get(i);
      return null;
    }
    
    // Implementation of BasicErrorManager
    
    @Override
    protected void printSummary() {
      append(
          String.format("%d error(s), %d warning(s), %.1f%% typed%n",
          getErrorCount(), getWarningCount(), getTypedPercent()));      
    }

    @Override
    public void println(CheckLevel level, JSError error) {
      ErrorInfo info = new ErrorInfo(error);
      errorInfos.add(info);
      append(info.error.format(level, formatter));
    }
    
    // Implementation of IStreamMonitor
    
    private Set<IStreamListener> listeners = new HashSet<IStreamListener>();
    private StringBuffer buffer = new StringBuffer();

    @Override
    public void addListener(IStreamListener listener) { listeners.add(listener); }

    @Override
    public String getContents() {
      return buffer.toString();
    }

    @Override
    public void removeListener(IStreamListener listener) { listeners.remove(listener); }
    
    private void append(String text) {
      buffer.append(text);
      for (IStreamListener listener: listeners) listener.streamAppended(text, this);
    }
  }
  
  private StreamMonitorErrorManager errorManager = new StreamMonitorErrorManager();

  private IStreamsProxy streamsProxy = new IStreamsProxy() {
  
    @Override
    public IStreamMonitor getErrorStreamMonitor() {
      return null;
    }
  
    @Override
    public IStreamMonitor getOutputStreamMonitor() {
      return errorManager;
    }
  
    @Override
    public void write(String input) throws IOException {}

  };
  
  private IPatternMatchListener patternMatchListener = new IPatternMatchListener(){

    private TextConsole console;
    private int lineIndex = 0;

    @Override
    public void connect(TextConsole console) {
      this.console = console;
    }

    @Override
    public void disconnect() {}

    @Override
    public void matchFound(PatternMatchEvent event) {
      ErrorInfo info = errorManager.getErrorInfo(lineIndex);
      if (info != null && info.file != null) {
        FileLink link = new FileLink(info.file, null, -1, -1, info.error.lineNumber);
        try {
          console.addHyperlink(link, event.getOffset(), info.fileName.length());
        } catch (BadLocationException e) {}        
      }
      ++lineIndex;
    }

    @Override
    public int getCompilerFlags() {
      return 0;
    }

    @Override
    public String getLineQualifier() {
      return null;
    }

    @Override
    public String getPattern() {
      return ".+";
    }};
  
}
