package net.vtst.ow.eclipse.soy.ui.folding;

import net.vtst.eclipse.easyxtext.ui.folding.EasyFoldingRegionProvider;
import net.vtst.ow.eclipse.soy.soy.Template;

public class SoyFoldingRegionProvider extends EasyFoldingRegionProvider {

  // Only templates are candidates for folding.
  // TODO: Bug for empty templates
  
  protected Boolean _isHandled(Template obj) {
    return Boolean.TRUE;
  }
  
  // In order to improve efficiency, we stop the traversal at the AST at some
  // points.
  
  protected Boolean _shouldProcessContent(Template obj) {
    return Boolean.FALSE;
  }
  
}
