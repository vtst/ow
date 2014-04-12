package net.vtst.ow.eclipse.less.resource;

import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.xtext.resource.SynchronizedXtextResourceSet;

/**
 * A customization of SynchronizedXtextResourceSet using LessURIConverter.
 * @author Vincent Simonet
 */
public class LessSynchronizedXtextResourceSet extends SynchronizedXtextResourceSet {

  @Override
  public URIConverter getURIConverter() {
    if (uriConverter == null) {
      uriConverter = new LessURIConverter(this);
    }
    return uriConverter;
  }

}
