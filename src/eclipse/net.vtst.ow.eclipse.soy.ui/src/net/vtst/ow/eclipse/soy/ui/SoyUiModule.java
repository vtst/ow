/*
 * generated by Xtext
 */
package net.vtst.ow.eclipse.soy.ui;

import net.vtst.eclipse.easyxtext.util.IEasyMessages;
import net.vtst.ow.eclipse.soy.ui.folding.SoyFoldingRegionProvider;
import net.vtst.ow.eclipse.soy.ui.syntaxcoloring.SoyAntlrTokenToAttributeIdMapper;
import net.vtst.ow.eclipse.soy.ui.syntaxcoloring.SoyHighlightingConfiguration;
import net.vtst.ow.eclipse.soy.ui.syntaxcoloring.SoySemanticHighlightingCalculator;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.xtext.resource.containers.IAllContainersState;
import org.eclipse.xtext.ui.IImageHelper;
import org.eclipse.xtext.ui.editor.folding.IFoldingRegionProvider;
import org.eclipse.xtext.ui.editor.syntaxcoloring.AbstractAntlrTokenToAttributeIdMapper;
import org.eclipse.xtext.ui.editor.syntaxcoloring.IHighlightingConfiguration;
import org.eclipse.xtext.ui.editor.syntaxcoloring.ISemanticHighlightingCalculator;

import com.google.inject.Provider;

/**
 * Use this class to register components to be used within the IDE.
 */
public class SoyUiModule extends net.vtst.ow.eclipse.soy.ui.AbstractSoyUiModule {
  
  public static String PLUGIN_ID = "net.vtst.ow.eclipse.soy.ui";
  
	public SoyUiModule(AbstractUIPlugin plugin) {
		super(plugin);
	}
	
  public Class<? extends IEasyMessages> bindIEasyMessages() {
    return SoyUiMessages.class;
  }
	
  // This is to replace the default generated parser, by the customized version.
	
	@Override
  public void configureHighlightingLexer(com.google.inject.Binder binder) {
    binder.bind(org.eclipse.xtext.parser.antlr.Lexer.class).annotatedWith(com.google.inject.name.Names.named(org.eclipse.xtext.ui.LexerUIBindings.HIGHLIGHTING)).to(net.vtst.ow.eclipse.soy.parser.CustomizedSoyLexer.class);
  }
	
  // Image helper

  public Class<? extends IImageHelper> bindIImageHelper() {
    return SoyImageHelper.class;
  }

  // Syntax coloring

  public Class<? extends IHighlightingConfiguration> bindIHighlightingConfiguration () {
    return SoyHighlightingConfiguration.class;
  }
  public Class<? extends AbstractAntlrTokenToAttributeIdMapper> bindAbstractAntlrTokenToAttributeIdMapper() {
    return SoyAntlrTokenToAttributeIdMapper.class;
  } 
  public Class<? extends ISemanticHighlightingCalculator> bindISemanticHighlightingCalculator(){
    return SoySemanticHighlightingCalculator.class;
  }
  
  // Folding
  
  public Class<? extends IFoldingRegionProvider> bindIFoldingRegionProvider() {
    return SoyFoldingRegionProvider.class;
  }
  
  // Scoping
  
  public Provider<IAllContainersState> provideIAllContainersState() {
    return org.eclipse.xtext.ui.shared.Access.getWorkspaceProjectsState();
  }


}
