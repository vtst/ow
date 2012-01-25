package net.vtst.ow.eclipse.soy.ui.launching;

import java.util.regex.Pattern;

import net.vtst.eclipse.easyxtext.ui.launching.EasyLaunchConfigurationHelper;
import net.vtst.eclipse.easyxtext.ui.launching.attributes.BooleanLaunchAttribute;
import net.vtst.eclipse.easyxtext.ui.launching.attributes.StringLaunchAttribute;
import net.vtst.eclipse.easyxtext.ui.launching.attributes.StringOptionsLaunchAttribute;
import net.vtst.eclipse.easyxtext.ui.launching.attributes.WorkspaceFileLaunchAttribute;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;

import com.google.inject.Singleton;

@Singleton
public class SoyCompilerLaunchConfigurationHelper extends EasyLaunchConfigurationHelper {
  
  // Options to configure the compilation
  public StringOptionsLaunchAttribute bidiGlobalDir = 
      new StringOptionsLaunchAttribute(new String[]{"ltr", "rtl"});
  public StringOptionsLaunchAttribute codeStyle =
      new StringOptionsLaunchAttribute(new String[]{"stringbuilder", "concat"});
  public StringOptionsLaunchAttribute cssHandlingScheme =
      new StringOptionsLaunchAttribute(new String[]{"literal", "reference", "goog"});
  public BooleanLaunchAttribute isUsingIjData = new BooleanLaunchAttribute(true);
  public BooleanLaunchAttribute shouldDeclareTopLevelNamespaces = new BooleanLaunchAttribute(true);
  public BooleanLaunchAttribute shouldGenerateJsdoc = new BooleanLaunchAttribute(true);
  public BooleanLaunchAttribute shouldProvideRequireSoyNamespaces = new BooleanLaunchAttribute(false);

  // Input and output files
  public WorkspaceFileLaunchAttribute inputFile = new WorkspaceFileLaunchAttribute("", Pattern.compile(".*\\.soy"));
  public WorkspaceFileLaunchAttribute compileTimeGlobalsFile = new WorkspaceFileLaunchAttribute("", Pattern.compile(".*"));
  public BooleanLaunchAttribute localize = new BooleanLaunchAttribute(false);
  public StringLaunchAttribute locales = new StringLaunchAttribute("");
  public StringLaunchAttribute messageFilePathFormat = new StringLaunchAttribute("{INPUT_DIRECTORY}/{INPUT_FILE_NAME_NO_EXT}_{LOCALE}.xlf");
  public StringLaunchAttribute outputPathFormat = new StringLaunchAttribute("{INPUT_DIRECTORY}/{INPUT_FILE_NAME_NO_EXT}.js");
  public StringLaunchAttribute outputPathFormatLocalized = new StringLaunchAttribute("{INPUT_DIRECTORY}/{INPUT_FILE_NAME_NO_EXT}_{LOCALE}.js");
  
  public BooleanLaunchAttribute useAsDefault = new BooleanLaunchAttribute(false);

  /**
   * Replace the variables {INPUT_DIRECTORY}, {INPUT_FILE_NAME} and {INPUT_FILE_NAME_NO_EXT}
   * from a path format.
   * @param config  The launch configuration to read (for getting the input file)
   * @param pathFormat  The path format.
   * @return  The substituted path format.
   */
  private String replaceVariables(ILaunchConfiguration config, String pathFormat) {
    IPath inputFilePath = new Path(inputFile.getValue(config));
    return pathFormat
        .replaceAll("\\{INPUT_DIRECTORY\\}", inputFilePath.removeLastSegments(1).toString())
        .replaceAll("\\{INPUT_FILE_NAME\\}", inputFilePath.lastSegment())
        .replaceAll("\\{INPUT_FILE_NAME_NO_EXT\\}", inputFilePath.removeFileExtension().lastSegment());    
  }
  
  /**
   * Compute the substituted message file format for a launch configuration.
   * @param config  The launch configuration to read.
   * @return  the substituted message file format. 
   */
  public String getMessageFileFormat(ILaunchConfiguration config) {
    return replaceVariables(config, messageFilePathFormat.getValue(config));
  }

  /**
   * Compute the substituted compile time globals file for a launch configuration.
   * @param config  The launch configuration to read.
   * @return  the substituted message file format. 
   */
  public String getCompileTimeGlobalsFile(ILaunchConfiguration config) {
    return replaceVariables(config, compileTimeGlobalsFile.getValue(config));
  }

  /**
   * Compute the output files from a launch configuration.
   * @param config  The launch configuration to read.
   * @return  the output files, as an array.
   */
  public IFile[] getOutputFiles(ILaunchConfiguration config) {
    boolean isLocalize = localize.getBooleanValue(config);
    String outputFilePath = replaceVariables(
        config, (isLocalize ? outputPathFormatLocalized : outputPathFormat).getValue(config));
    if (isLocalize) {
      String[] localesArray = locales.getValue(config).split(",");
      IFile[] outputFiles = new IFile[localesArray.length];
      for (int i = 0; i < localesArray.length; ++i) {
        String localizedOutputFilePath = outputFilePath
            .replaceAll("\\{LOCALE\\}", localesArray[i])
            .replaceAll("\\{LOCALE_LOWER_CASE\\}", localesArray[i].toLowerCase().replaceAll("-", "_"));
        outputFiles[i] = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(localizedOutputFilePath));
      }
      return outputFiles;
    } else {
      IFile outputFile = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(outputFilePath));
      return new IFile[]{outputFile};
    }
  }

}
