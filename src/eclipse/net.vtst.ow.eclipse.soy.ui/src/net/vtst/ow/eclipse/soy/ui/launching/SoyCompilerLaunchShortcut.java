package net.vtst.ow.eclipse.soy.ui.launching;

import java.util.ArrayList;
import java.util.List;

import net.vtst.eclipse.easyxtext.ui.launching.EasyLaunchShortcut;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import com.google.inject.Inject;

public class SoyCompilerLaunchShortcut extends EasyLaunchShortcut<IFile> {

  private final static String TYPE_ID = "net.vtst.ow.eclipse.soy.launching.compiler";

  @Inject
  private SoyCompilerLaunchConfigurationHelper configHelper;

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
    if (!selectedFile.equals(configHelper.inputFile.getFileValue(configwc))) {
      configHelper.inputFile.setFileValue(configwc, selectedFile);
    }
    return configwc;
  }
  
  private ILaunchConfigurationWorkingCopy castILaunchConfigurationWorkingCopy(ILaunchConfiguration config) throws CoreException {
    if (config instanceof ILaunchConfigurationWorkingCopy) return (ILaunchConfigurationWorkingCopy) config;
    return config.getWorkingCopy();
  }
}
