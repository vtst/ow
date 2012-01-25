// EasyXtext
// (c) Vincent Simonet, 2011
package net.vtst.eclipse.easyxtext.scoping;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.resource.IResourceFactory;
import org.eclipse.xtext.util.Pair;
import org.eclipse.xtext.util.SimpleCache;
import org.eclipse.xtext.util.Tuples;
import org.osgi.framework.Bundle;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Resource description provider for
 * {@link net.vtst.eclipse.easyxtext.scoping.EasyBuiltinGlobalScopeProvider}.
 * 
 * <p>
 * This class parses files of the Domain Specific Language which are included in
 * the plugin's bundle, and makes the corresponding resources and object
 * descriptions available. It is used by
 * {@link net.vtst.eclipse.easyxtext.scoping.EasyBuiltinGlobalScopeProvider} to
 * build the built-in scope.
 * </p>
 * <p>
 * It is implemented so that every file is read and interpreted only once
 * (singleton class and caches).
 * </p>
 * <p>
 * Every source file is identified by a pair of strings:
 * <ul>
 * <li>The identifier of the plugin's bundle,</li>
 * <li>The path of the file in the bundle.</li>
 * </ul>
 * 
 * @author Vincent
 */
@Singleton
public class EasyBuiltinResourceDescriptionProvider {

  @Inject
  private IResourceDescription.Manager descriptionManager;

  @Inject
  private IResourceFactory resourceFactory;

  private final SimpleCache<Pair<String, String>, Resource> resourceCache = new SimpleCache<Pair<String, String>, Resource>(
      new Function<Pair<String, String>, Resource>() {
        public Resource apply(Pair<String, String> key) {
          Bundle bundle = Platform.getBundle(key.getFirst());
          try {
            Path path = new Path(key.getSecond());
            Resource resource = resourceFactory.createResource(URI
                .createURI("platform:/resource/" + key.getFirst() + "/"
                    + path.toString()));
            InputStream inputStream = FileLocator.openStream(bundle, path,
                false);
            resource.load(inputStream, new HashMap<String, String>());
            return resource;
          } catch (IOException e) {
            e.printStackTrace();
            return null;
          }
        }
      });

  /**
   * Load a resource from the plugin's bundle, and return the EMF resource
   * object. (The results of this function are cached so that every file is read
   * only once.)
   * 
   * @param bundleName
   *          The name of the bundle (e.g. "net.vtst.ow.eclipse.soy")
   * @param filename
   *          The path of the file in the bundle (e.g. "data/builtin.soy")
   * @return The loaded resource, null in case of error.
   */
  public final Resource getResource(String bundleName, String filename) {
    return resourceCache.get(Tuples.pair(bundleName, filename));
  }

  private final SimpleCache<Pair<String, String>, IResourceDescription> resourceDescriptionCache = new SimpleCache<Pair<String, String>, IResourceDescription>(
      new Function<Pair<String, String>, IResourceDescription>() {
        public IResourceDescription apply(Pair<String, String> key) {
          Resource resource = resourceCache.get(key);
          if (resource == null)
            return null;
          return descriptionManager.getResourceDescription(resource);
        }
      });

  /**
   * Load a resource from the plugin's bundle, and return its description. (The
   * results of this function are cached so that every file is read only once.)
   * 
   * @param bundleName
   *          The name of the bundle (e.g. "net.vtst.ow.eclipse.soy")
   * @param filename
   *          The path of the file in the bundle (e.g. "data/builtin.soy")
   * @return The description of the loaded resource, null in case of error.
   */
  public final IResourceDescription getResourceDescription(String bundleName,
      String filename) {
    return resourceDescriptionCache.get(Tuples.pair(bundleName, filename));
  }

  private final SimpleCache<Pair<String, Iterable<String>>, Iterable<IEObjectDescription>> objectDescriptionsCache = new SimpleCache<Pair<String, Iterable<String>>, Iterable<IEObjectDescription>>(
      new Function<Pair<String, Iterable<String>>, Iterable<IEObjectDescription>>() {
        public Iterable<IEObjectDescription> apply(
            Pair<String, Iterable<String>> key) {
          ArrayList<IEObjectDescription> objectDescriptions = new ArrayList<IEObjectDescription>();
          String bundleName = key.getFirst();
          for (String filename : key.getSecond()) {
            IResourceDescription resourceDescription = resourceDescriptionCache
                .get(Tuples.pair(bundleName, filename));
            if (resourceDescription != null) {
              Iterables.addAll(objectDescriptions,
                  resourceDescription.getExportedObjects());
            }
          }
          return objectDescriptions;
        }
      });

  /**
   * Load a set of resource from the plugin's bundle, an return an iterable of
   * the object descriptions that the files includes.
   * 
   * @param bundleName
   *          The name of the bundle (e.g. "net.vtst.ow.eclipse.soy")
   * @param filenames
   *          The paths of the files in the bundle (e.g. "data/builtin.soy")
   * @return The list of object descriptions.
   */
  public final Iterable<IEObjectDescription> getObjectDescriptions(
      String bundleName, Iterable<String> filenames) {
    return objectDescriptionsCache.get(Tuples.pair(bundleName, filenames));
  }

}
