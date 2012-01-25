// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.ui.folding;

import net.vtst.eclipse.easyxtext.ui.folding.EasyFoldingRegionProvider;
import net.vtst.ow.eclipse.less.less.Block;
import net.vtst.ow.eclipse.less.less.Declaration;
import net.vtst.ow.eclipse.less.less.MixinCall;
import net.vtst.ow.eclipse.less.less.MixinDefinitionParameter;

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
  
  protected Boolean _shouldProcessContent(MixinDefinitionParameter obj) {
    return Boolean.FALSE;
  }
  
  protected Boolean _shouldProcessContent(MixinCall obj) {
    return Boolean.FALSE;
  }

}
