package net.vtst.ow.eclipse.js.closure.launching.linter;

import java.util.Collections;
import java.util.List;

import net.vtst.eclipse.easy.ui.launching.EasyLaunchShortcut;
import net.vtst.eclipse.easy.ui.properties.stores.IReadOnlyStore;
import net.vtst.eclipse.easy.ui.properties.stores.LaunchConfigurationReadOnlyStore;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;

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
    for (ILaunchConfiguration config: getAllLaunchConfigurations()) {
      IReadOnlyStore store = new LaunchConfigurationReadOnlyStore(config);
      if (record.useAsDefault.get(store) && isSelectedResource(record.inputResources.get(store), resource)) {
        return Collections.singletonList(config);
      }
    }
    return null;
  }
  
  private boolean isSelectedResource(List<IResource> resources, IResource selectedResource) {
    if (resources.size() != 1) return false;
    return resources.get(0).equals(selectedResource);
  }

}
