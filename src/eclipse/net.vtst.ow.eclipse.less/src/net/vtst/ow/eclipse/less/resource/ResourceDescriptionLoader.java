package net.vtst.ow.eclipse.less.resource;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.resource.IResourceDescription.Manager;
import org.eclipse.xtext.resource.IResourceServiceProvider;

import com.google.inject.Inject;

/**
 * A simplified version of LoadOnDemandResourceDescriptions.  Loading only ABSOLUTE URIs.
 * @author Vincent Simonet
 */
public class ResourceDescriptionLoader {
  
  @Inject
  private IResourceServiceProvider.Registry serviceProviderRegistry;

  public IResourceDescription getResourceDescription(ResourceSet resourceSet, URI uri) {
    Resource resource = resourceSet.getResource(uri, true);
    if (resource != null) {
      IResourceServiceProvider serviceProvider = serviceProviderRegistry.getResourceServiceProvider(uri);
      if (serviceProvider==null)
        throw new IllegalStateException("No "+IResourceServiceProvider.class.getSimpleName()+" found in registry for uri "+uri);
      final Manager resourceDescriptionManager = serviceProvider.getResourceDescriptionManager();
      if (resourceDescriptionManager == null)
        throw new IllegalStateException("No "+IResourceDescription.Manager.class.getName()+" provided by service provider for URI "+uri);
      return resourceDescriptionManager.getResourceDescription(resource);
    }
    return null;
  }

}
