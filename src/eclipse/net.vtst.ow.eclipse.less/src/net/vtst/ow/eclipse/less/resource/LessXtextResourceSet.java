package net.vtst.ow.eclipse.less.resource;

import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.xtext.resource.XtextResourceSet;

/**
 * A customization of XtextResourceSet using LessURIConverter.
 * @author Vincent Simonet
 */
public class LessXtextResourceSet extends XtextResourceSet {
  
  @Override
  public URIConverter getURIConverter() {
    if (uriConverter == null) {
      uriConverter = new LessURIConverter(this);
    }
    return uriConverter;
  }
  
}
