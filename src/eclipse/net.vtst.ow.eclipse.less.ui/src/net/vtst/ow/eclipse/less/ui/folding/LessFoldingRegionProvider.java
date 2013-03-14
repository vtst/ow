// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.ui.folding;

import net.vtst.eclipse.easyxtext.ui.folding.EasyFoldingRegionProvider;
import net.vtst.ow.eclipse.less.less.Block;
import net.vtst.ow.eclipse.less.less.Declaration;
import net.vtst.ow.eclipse.less.less.MixinParameter;
import net.vtst.ow.eclipse.less.less.TerminatedMixin;
import net.vtst.ow.eclipse.less.less.UnterminatedMixin;

public class LessFoldingRegionProvider extends EasyFoldingRegionProvider {

  // Only blocks are candidates for folding.
  
  protected Boolean _isHandled(Block obj) {
    return Boolean.TRUE;
  }
  
  // In order to improve efficiency, we stop the traversal at the AST at some
  // points.
  
  protected Boolean _shouldProcessContent(Declaration obj) {
    return Boolean.FALSE;
  }
  
  protected Boolean _shouldProcessContent(MixinParameter obj) {
    return Boolean.FALSE;
  }
  
  protected Boolean _shouldProcessContent(TerminatedMixin obj) {
    if (obj.getBody() == null) return Boolean.FALSE;
    else return Boolean.TRUE;
  }

  protected Boolean _shouldProcessContent(UnterminatedMixin obj) {
    return Boolean.FALSE;
  }

}
