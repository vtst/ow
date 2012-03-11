package net.vtst.eclipse.easyxtext.scoping;

import java.util.LinkedHashSet;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.scoping.impl.ImportUriGlobalScopeProvider;
import org.eclipse.xtext.util.IAcceptor;
import org.eclipse.xtext.util.IResourceScopeCache;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * This class extends {@code ImportUriGlobalScopeProvider}, and provides an efficient way to retrieve
 * the imported URIs from a resource.  The main limitation of {@code ImportUriGlobalScopeProvider} is that
 * it queries every node of the abstract syntax tree to check whether it contains an URI to import.  With
 * this class, it is possible to prune sub-trees.  This is particular useful when imported URIs may appear
 * only at limited places of the abstract syntax tree, e.g. the top-level statements. 
 * @author Vincent Simonet
 */
public abstract class EasyUriGlobalScopeProvider extends ImportUriGlobalScopeProvider {
// TODO: Add to documentation
  
  @Inject
  private IResourceScopeCache cache;
  
  /**
   * Sub-classes must implement this method to specify the URI(s) to import from each node of the abstract
   * syntax tree.
   * @param object  The node to look for.
   * @param acceptor  The acceptor for URIs.
   * @return  true if the children of {@code object} must be traversed, false if the children can be pruned.
   */
  protected abstract boolean getImportedUris(EObject object, IAcceptor<String> acceptor);
  
  /**
   * An acceptor for URIs.  It checks that URIs are valid for the given resource, and removes duplicates.
   * @author Vincent Simonet
   */
  private static class UriStringAcceptor implements IAcceptor<String> {
    
    private Resource resource;
    private LinkedHashSet<URI> uris = new LinkedHashSet<URI>(10);
    
    public UriStringAcceptor(Resource resource) {
      this.resource = resource;
    }

    @Override
    public void accept(String uriString) {
      if (uriString != null) {
        URI uri = URI.createURI(uriString);
        if (EcoreUtil2.isValidUri(resource, uri)) {
          uris.add(uri);
        }
      }      
    }
    
    public LinkedHashSet<URI> get() {
      return uris;
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.xtext.scoping.impl.ImportUriGlobalScopeProvider#getImportedUris(org.eclipse.emf.ecore.resource.Resource)
   */
  protected LinkedHashSet<URI> getImportedUris(final Resource resource) {
    return cache.get(EasyUriGlobalScopeProvider.class, resource, new Provider<LinkedHashSet<URI>>(){
      public LinkedHashSet<URI> get() {
        final UriStringAcceptor uris = new UriStringAcceptor(resource);
        TreeIterator<EObject> it = resource.getAllContents();
        while (it.hasNext()) {
          if (!getImportedUris(it.next(), uris))
            it.prune();
        }
        return uris.get();
      }
    });
  }

}
