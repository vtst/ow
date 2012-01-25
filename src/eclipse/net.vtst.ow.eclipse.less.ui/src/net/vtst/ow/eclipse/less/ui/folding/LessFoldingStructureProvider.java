// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.ui.folding;

import org.eclipse.xtext.ui.editor.folding.DefaultFoldingStructureProvider;


public class LessFoldingStructureProvider extends DefaultFoldingStructureProvider {
  public void initialize() {
    calculateProjectionAnnotationModel(false);
  }
}
