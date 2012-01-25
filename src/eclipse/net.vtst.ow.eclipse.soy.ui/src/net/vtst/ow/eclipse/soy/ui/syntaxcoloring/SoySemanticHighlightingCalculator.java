// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.soy.ui.syntaxcoloring;

import net.vtst.eclipse.easyxtext.ui.syntaxcoloring.EasySemanticHighlightingCalculator;
import net.vtst.ow.eclipse.soy.services.SoyGrammarAccess;
import net.vtst.ow.eclipse.soy.soy.CommandAttribute;
import net.vtst.ow.eclipse.soy.soy.Declaration;

import com.google.inject.Inject;

public class SoySemanticHighlightingCalculator extends EasySemanticHighlightingCalculator {
  
  @Inject
  private SoyGrammarAccess grammar;

  @Inject
  private SoyHighlightingConfiguration highlightingConfig;

  @Override
  protected void configure() {
    bindRule(grammar.getExprRule(), highlightingConfig.COMMAND_CONTENTS);
    bindRule(grammar.getPrintDirectiveRule(), highlightingConfig.COMMAND_CONTENTS);
    bindRule(grammar.getExprListRule(), highlightingConfig.COMMAND_CONTENTS);
    bindRule(grammar.getForeachRangeRule(), highlightingConfig.COMMAND_CONTENTS);
    bindRule(grammar.getForRangeRule(), highlightingConfig.COMMAND_CONTENTS);
    //bindRule(grammar.getDotIdentRule(), highlightingConfig.COMMAND_CONTENTS);
    //bindRule(grammar.getDottedIdentRule(), highlightingConfig.COMMAND_CONTENTS);
    bindRule(grammar.getCallParamIdentExprRule(), highlightingConfig.COMMAND_CONTENTS);
    bindRule(grammar.getCallParamIdentRule(), highlightingConfig.COMMAND_CONTENTS);
    bindRule(grammar.getCSS_IDENTRule(), highlightingConfig.COMMAND_CONTENTS);
    bindRule(grammar.getTemplateDefinitionDotIdentRule(), highlightingConfig.TEMPLATE_IDENT);
    bindRule(grammar.getTemplateDefinitionDottedIdentRule(), highlightingConfig.TEMPLATE_IDENT);
    bindRule(grammar.getHtmlTagBeginRule(), highlightingConfig.HTML_TAG);
    bindRule(grammar.getHtmlTagEndRule(), highlightingConfig.HTML_TAG);
    bindRule(grammar.getHtmlAttributeRule(), highlightingConfig.HTML_ATTRIBUTE);
    //bindRule(grammar.getCommandAttributesRule(), highlightingConfig.COMMAND_CONTENTS);
    bindRule(grammar.getIDENTRule(), CommandAttribute.class, highlightingConfig.COMMAND_CONTENTS);
    bindRule(grammar.getIDENTRule(), Declaration.class, highlightingConfig.SOY_DOC_IDENT);
    bindRule(grammar.getDEC_DIGITSRule(), Declaration.class, highlightingConfig.SOY_DOC_IDENT);
    bindKeyword("=", CommandAttribute.class, highlightingConfig.COMMAND_CONTENTS);
  }

}
