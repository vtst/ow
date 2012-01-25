// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.ui.launching;

import java.util.ArrayList;
import java.util.List;

import net.vtst.eclipse.easyxtext.ui.launching.EasyLaunchShortcut;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import com.google.inject.Inject;

public class LessCompilerLaunchShortcut extends EasyLaunchShortcut<IFile> {

  private final static String TYPE_ID = "net.vtst.ow.eclipse.less.launching.compiler";
  
  @Inject
  private LessCompilerLaunchConfigurationHelper configHelper;

  /* This launch configuration shortcut is enabled only for selections containing exactly one file.
   * @see net.vtst.ow.easyxtext.ui.launching.EasyLaunchShortcut#getSelection(java.lang.Iterable)
   */
  @Override
  protected IFile getSelection(Iterable<IResource> selection) throws CoreException {
    for (IResource resource: selection) {
      if (resource instanceof IFile) return (IFile) resource;
    }
    return null;
  }
  
  @Override
  protected String getLaunchConfigurationTypeID() {
    return TYPE_ID;
  }
  
  @Override
  protected List<ILaunchConfiguration> findLaunchConfigurations(IFile selectedFile, String mode) throws CoreException {
    ILaunchConfiguration[] configs = getAllLaunchConfigurations();
    List<ILaunchConfiguration> list = new ArrayList<ILaunchConfiguration>();
    // First we try to find a launch configuration which for the selected file
    for (ILaunchConfiguration config: configs) {
      if (selectedFile.equals(configHelper.inputFile.getFileValue(config))) list.add(config);
    }
    // Second, we try to find a launch configuration which is marked as default
    if (list.isEmpty()) {
      for (ILaunchConfiguration config: configs) {
        if (configHelper.useAsDefault.getBooleanValue(config)) list.add(config);
      }
    }
    return list;
  }

  protected ILaunchConfiguration adaptLaunchConfiguration(ILaunchConfiguration config, IFile selectedFile, String mode) throws CoreException { 
    if (selectedFile.equals(configHelper.inputFile.getFileValue(config))) return config;
    ILaunchConfigurationWorkingCopy configwc = castILaunchConfigurationWorkingCopy(config);
    configHelper.inputFile.setFileValue(configwc, selectedFile);
    configHelper.outputFile.setValue(configwc, configHelper.getAutoOutputFile(configwc));
    return configwc;
  }
  
  private ILaunchConfigurationWorkingCopy castILaunchConfigurationWorkingCopy(ILaunchConfiguration config) throws CoreException {
    if (config instanceof ILaunchConfigurationWorkingCopy) return (ILaunchConfigurationWorkingCopy) config;
    return config.getWorkingCopy();
  }
}
