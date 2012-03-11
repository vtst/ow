// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.ui;

import net.vtst.eclipse.easyxtext.util.IEasyMessages;
import net.vtst.ow.eclipse.less.ui.folding.LessFoldingRegionProvider;
import net.vtst.ow.eclipse.less.ui.folding.LessFoldingStructureProvider;
import net.vtst.ow.eclipse.less.ui.hyperlinking.LessHyperlinkHelper;
import net.vtst.ow.eclipse.less.ui.syntaxcoloring.LessAntlrTokenToAttributeIdMapper;
import net.vtst.ow.eclipse.less.ui.syntaxcoloring.LessHighlightingConfiguration;
import net.vtst.ow.eclipse.less.ui.syntaxcoloring.LessSemanticHighlightingCalculator;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.xtext.ui.IImageHelper;
import org.eclipse.xtext.ui.editor.folding.IFoldingRegionProvider;
import org.eclipse.xtext.ui.editor.folding.IFoldingStructureProvider;
import org.eclipse.xtext.ui.editor.syntaxcoloring.AbstractAntlrTokenToAttributeIdMapper;
import org.eclipse.xtext.ui.editor.syntaxcoloring.IHighlightingConfiguration;
import org.eclipse.xtext.ui.editor.syntaxcoloring.ISemanticHighlightingCalculator;

/**
 * Use this class to register components to be used within the IDE.
 */
public class LessUiModule extends net.vtst.ow.eclipse.less.ui.AbstractLessUiModule {
  
  public static String PLUGIN_ID = "net.vtst.ow.eclipse.less.ui";
  
	public LessUiModule(AbstractUIPlugin plugin) {
		super(plugin);
	}
	
  public Class<? extends IEasyMessages> bindIEasyMessages() {
    return LessUiMessages.class;
  }
    
  // Image helper

  public Class<? extends IImageHelper> bindIImageHelper() {
    return LessImageHelper.class;
  }

  // Syntax coloring

  public Class<? extends IHighlightingConfiguration> bindIHighlightingConfiguration () {
    return LessHighlightingConfiguration.class;
  }
	public Class<? extends AbstractAntlrTokenToAttributeIdMapper> bindAbstractAntlrTokenToAttributeIdMapper() {
	  return LessAntlrTokenToAttributeIdMapper.class;
	}	
  public Class<? extends ISemanticHighlightingCalculator> bindISemanticHighlightingCalculator(){
    return LessSemanticHighlightingCalculator.class;
  }
  
  // Folding
  
  public Class<? extends IFoldingRegionProvider> bindIFoldingRegionProvider() {
    return LessFoldingRegionProvider.class;
  }

  public Class<? extends IFoldingStructureProvider> bindIFoldingStructureProvider() {
    return LessFoldingStructureProvider.class;
  }
  
  // Hyper-linking
  
  public Class<? extends org.eclipse.xtext.ui.editor.hyperlinking.IHyperlinkHelper> bindIHyperlinkHelper() {
    return LessHyperlinkHelper.class;
  }
  
}
