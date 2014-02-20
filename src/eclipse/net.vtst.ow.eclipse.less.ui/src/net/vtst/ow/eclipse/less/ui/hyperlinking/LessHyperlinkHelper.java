package net.vtst.ow.eclipse.less.ui.hyperlinking;

import net.vtst.ow.eclipse.less.less.ImportStatement;
import net.vtst.ow.eclipse.less.scoping.LessImportStatementResolver;
import net.vtst.ow.eclipse.less.scoping.LessImportStatementResolver.ResolvedImportStatement;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.jface.text.Region;
import org.eclipse.xtext.resource.EObjectAtOffsetHelper;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.hyperlinking.HyperlinkHelper;
import org.eclipse.xtext.ui.editor.hyperlinking.IHyperlinkAcceptor;
import org.eclipse.xtext.ui.editor.hyperlinking.XtextHyperlink;

import com.google.inject.Inject;

public class LessHyperlinkHelper extends HyperlinkHelper {

  @Inject
  private EObjectAtOffsetHelper eObjectAtOffsetHelper;
  
  @Inject
  private LessImportStatementResolver importStatementResolver;

  public void createHyperlinksByOffset(XtextResource resource, int offset, IHyperlinkAcceptor acceptor) {
    // Create hyper-link for import statements
    EObject object = eObjectAtOffsetHelper.resolveElementAt(resource, offset);
    if (object instanceof ImportStatement) {
      ResolvedImportStatement resolvedImportStatement = importStatementResolver.resolve((ImportStatement) object);
      if (!resolvedImportStatement.hasError())
        createHyperlinksTo(resource, resolvedImportStatement.getURI(), acceptor);
    }
    // Create other hyper-links
    super.createHyperlinksByOffset(resource, offset, acceptor);
  }

  /**
   * Create an hyper-link to the resource designated by an URI.
   * @param from  The source resource.
   * @param uri  The target URI.
   * @param acceptor  An acceptor for the hyper-link.
   */
  public void createHyperlinksTo(XtextResource from, URI uri, IHyperlinkAcceptor acceptor) {
    final URIConverter uriConverter = from.getResourceSet().getURIConverter();
    final String hyperlinkText = uri.toString();
    final URI normalized = uriConverter.normalize(uri).resolve(from.getURI());

    XtextHyperlink result = getHyperlinkProvider().get();
    result.setHyperlinkRegion(new Region(1, 1));
    result.setURI(normalized);
    result.setHyperlinkText(hyperlinkText);
    acceptor.accept(result);
  }

}
