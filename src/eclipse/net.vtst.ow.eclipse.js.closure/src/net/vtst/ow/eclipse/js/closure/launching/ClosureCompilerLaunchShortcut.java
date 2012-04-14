package net.vtst.ow.eclipse.js.closure.launching;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;

import net.vtst.eclipse.easy.ui.launching.EasyLaunchShortcut;

public class ClosureCompilerLaunchShortcut extends EasyLaunchShortcut<IFile> {

  @Override
  protected IFile getSelection(Iterable<IResource> selection)
      throws CoreException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected String getLaunchConfigurationTypeID() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected List<ILaunchConfiguration> findLaunchConfigurations(
      IFile selection, String mode) throws CoreException {
    // TODO Auto-generated method stub
    return null;
  }

}
