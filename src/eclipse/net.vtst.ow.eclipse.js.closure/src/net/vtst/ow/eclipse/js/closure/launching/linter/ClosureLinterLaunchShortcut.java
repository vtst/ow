package net.vtst.ow.eclipse.js.closure.launching.linter;

import java.util.Collections;
import java.util.List;

import net.vtst.eclipse.easy.ui.launching.EasyLaunchShortcut;
import net.vtst.eclipse.easy.ui.properties.stores.IReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.IStore;
import net.vtst.eclipse.easy.ui.properties.stores.LaunchConfigurationReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.LaunchConfigurationStore;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

public class ClosureLinterLaunchShortcut extends EasyLaunchShortcut<IResource> {
  
  ClosureLinterLaunchConfigurationRecord record = new ClosureLinterLaunchConfigurationRecord();

  @Override
  protected IResource getSelection(Iterable<IResource> selection) throws CoreException {
    for (IResource resource: selection) return resource;
    return null;
  }

  @Override
  protected String getLaunchConfigurationTypeID() {
    return ClosureLinterLaunchConfigurationDelegate.TYPE_ID;
  }

  @Override
  protected List<ILaunchConfiguration> findLaunchConfigurations(IResource resource, String mode) throws CoreException {
    // This returns the first configuration that contains the selected resource, or otherwise
    // the first config which has the default bit.
    List<ILaunchConfiguration> defaultConfig = Collections.emptyList();
    for (ILaunchConfiguration config: getAllLaunchConfigurations()) {
      IReadOnlyStore store = new LaunchConfigurationReadOnlyStore(config);
      if (isSelectedResource(record.inputResources.get(store), resource)) {
        return Collections.singletonList(config);
      } else if (defaultConfig.isEmpty() && record.useAsDefault.get(store)) {
        defaultConfig = Collections.singletonList(config);
      }
    }
    return defaultConfig;
  }

  @Override
  protected ILaunchConfiguration adaptLaunchConfiguration(ILaunchConfiguration config, IResource selectedResource, String mode) throws CoreException {
    IReadOnlyStore store = new LaunchConfigurationReadOnlyStore(config);
    List<IResource> resources = record.inputResources.get(store);
    if (isSelectedResource(resources, selectedResource)) return config;
    ILaunchConfigurationWorkingCopy configwc = castILaunchConfigurationAsWorkingCopy(config);
    IStore storewc = new LaunchConfigurationStore(configwc);
    record.inputResources.set(storewc, Collections.singletonList(selectedResource));
    return configwc;
  }

  private boolean isSelectedResource(List<IResource> resources, IResource selectedResource) {
    if (resources.size() != 1) return false;
    return resources.get(0).equals(selectedResource);
  }

}
