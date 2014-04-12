package net.vtst.ow.eclipse.less.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.CommonPlugin;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ExtensibleURIConverterImpl;
import org.eclipse.xtext.resource.ClasspathUriResolutionException;
import org.eclipse.xtext.resource.ClasspathUriUtil;
import org.eclipse.xtext.resource.XtextResourceSet;

/**
 * An URI converter that is capable of resolving classpath URIs (as XtextResourceSet) and that ensures
 * URI are normalized (by calling CommonPlugin.resolve, what ensures platform:/resource URIs are translated
 * into their equivalent file:/ URIs.
 * See XtextResourceSet.getURIConverter.
 * 
 * @author Vincent Simonet
 */
public class LessURIConverter extends ExtensibleURIConverterImpl {
  
  private XtextResourceSet resourceSet;

  // From XtextResourceSet.addTimeout
  private Map<?, ?> addTimeout(Map<?, ?> options) {
    if (options == null || !options.containsKey(URIConverter.OPTION_TIMEOUT)) {
      HashMap<Object, Object> newOptions = options != null ? new HashMap<Object, Object>(options) : new HashMap<Object, Object>();
      newOptions.put(URIConverter.OPTION_TIMEOUT, 500);
      options = newOptions;
    }
    return options;
  }

  public LessURIConverter(XtextResourceSet resourceSet) {
    this.resourceSet = resourceSet;
  }

  @Override
  public URI normalize(URI uri) {
    if (ClasspathUriUtil.isClasspathUri(uri)) {
      URI result = this.resourceSet.getClasspathUriResolver().resolve(
          this.resourceSet.getClasspathURIContext(), uri);
      if (ClasspathUriUtil.isClasspathUri(result))
        throw new ClasspathUriResolutionException(result);
      result = super.normalize(result);
      return result;
    }
    return super.normalize(CommonPlugin.resolve(uri));
  }
  
  @Override
  public InputStream createInputStream(URI uri, Map<?, ?> options) throws IOException {
    // timeout is set here because e.g. SAXXMIHandler.resolveEntity(String,String) calls it without a timeout
    // causing the builder to wait too long...
    options = addTimeout(options);
    return super.createInputStream(uri, options);
  }

  @Override
  public Map<String, ?> contentDescription(URI uri, Map<?, ?> options) throws IOException {
    options = addTimeout(options);
    return super.contentDescription(uri, options);
  }

}
