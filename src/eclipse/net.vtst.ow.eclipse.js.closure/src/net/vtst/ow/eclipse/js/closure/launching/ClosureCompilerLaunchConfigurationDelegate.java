package net.vtst.ow.eclipse.js.closure.launching;

import java.util.List;

import net.vtst.eclipse.easy.ui.properties.stores.IReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.LaunchConfigurationReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.LaunchConfigurationStore;
import net.vtst.ow.eclipse.js.closure.compiler.CompilerOptionsFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;

import com.google.javascript.jscomp.CompilerOptions;

public class ClosureCompilerLaunchConfigurationDelegate extends LaunchConfigurationDelegate {
  
  ClosureCompilerLaunchConfigurationRecord launchRecord = ClosureCompilerLaunchConfigurationRecord.getInstance();

  @Override
  public void launch(ILaunchConfiguration config, String mode, ILaunch launch, IProgressMonitor monitor)
      throws CoreException {
    IReadOnlyStore store = new LaunchConfigurationReadOnlyStore(config);
    List<IResource> resources = launchRecord.inputResources.get(store);
    if (resources.isEmpty()) return;
    // We arbitrarily take the first project as the master one.
    IProject project = resources.get(0).getProject();
    CompilerOptions options = CompilerOptionsFactory.makeForFullCompilation(project, config);
  }

}
