// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.ui.launching;

import net.vtst.eclipse.easyxtext.ui.launching.EasyLaunchConfigurationHelper;
import net.vtst.eclipse.easyxtext.ui.launching.attributes.BooleanLaunchAttribute;
import net.vtst.eclipse.easyxtext.ui.launching.attributes.StringLaunchAttribute;
import net.vtst.eclipse.easyxtext.ui.launching.attributes.StringOptionsLaunchAttribute;
import net.vtst.eclipse.easyxtext.ui.launching.attributes.WorkspaceFileLaunchAttribute;
import net.vtst.ow.eclipse.less.LessRuntimeModule;

import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;

import com.google.inject.Singleton;

@Singleton
public class LessCompilerLaunchConfigurationHelper extends EasyLaunchConfigurationHelper {
  
  public StringLaunchAttribute command = new StringLaunchAttribute("lessc");
  public BooleanLaunchAttribute verbose = new BooleanLaunchAttribute(false);
  public BooleanLaunchAttribute compress = new BooleanLaunchAttribute(true);
  public StringOptionsLaunchAttribute optimization = new StringOptionsLaunchAttribute(new String[]{"0", "1", "2"});

  public WorkspaceFileLaunchAttribute inputFile = 
      new WorkspaceFileLaunchAttribute("", Platform.getContentTypeManager().getContentType(LessRuntimeModule.CONTENT_TYPE_ID));
  public StringLaunchAttribute outputFile = new StringLaunchAttribute("");
  public BooleanLaunchAttribute autoOutputFile = new BooleanLaunchAttribute(true);
  
  public BooleanLaunchAttribute useAsDefault = new BooleanLaunchAttribute(false);
  
  
  public static String LESS_EXTENSION = ".less";
  public static String CSS_EXTENSION = ".css";
  
  public String getAutoOutputFile(String inputFile) {
    if (inputFile.endsWith(LESS_EXTENSION)) inputFile = inputFile.substring(0, inputFile.length() - LESS_EXTENSION.length());
    return (inputFile + CSS_EXTENSION);
  }
  
  public String getAutoOutputFile(ILaunchConfiguration config) {
    return getAutoOutputFile(inputFile.getValue(config));
  }
  
}
