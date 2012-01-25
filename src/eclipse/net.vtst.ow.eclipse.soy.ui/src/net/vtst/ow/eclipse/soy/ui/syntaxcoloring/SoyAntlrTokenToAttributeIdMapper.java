// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.soy.ui.syntaxcoloring;

import net.vtst.eclipse.easyxtext.ui.syntaxcoloring.EasyAntlrTokenToAttributeIdMapper;
import net.vtst.ow.eclipse.soy.services.SoyGrammarAccess;

import com.google.inject.Inject;


public class SoyAntlrTokenToAttributeIdMapper extends EasyAntlrTokenToAttributeIdMapper {
  
  @Inject
  private SoyGrammarAccess grammar;

  @Inject
  private SoyHighlightingConfiguration highlightingConfig;

  @Override
  public void configure() {
    setDefaultAttribute(highlightingConfig.DEFAULT);
    bindTerminalRule(grammar.getFUNCTION_TAGRule(), highlightingConfig.SOY_DOC_TAG);
    bindTerminalRule(grammar.getPRINT_DIRECTIVE_TAGRule(), highlightingConfig.SOY_DOC_TAG);
    bindTerminalRule(grammar.getSTRING_SQRule(), highlightingConfig.STRING);
    bindTerminalRule(grammar.getSTRING_DQRule(), highlightingConfig.STRING);
    bindTerminalRule(grammar.getML_COMMENTRule(), highlightingConfig.COMMENT);
    bindTerminalRule(grammar.getSL_COMMENTRule(), highlightingConfig.COMMENT);
    bindTerminalRule(grammar.getSOY_DOC_OPENRule(), highlightingConfig.SOY_DOC);
    bindTerminalRule(grammar.getSOY_DOC_CLOSERule(), highlightingConfig.SOY_DOC);
    bindTerminalRule(grammar.getSOY_DOC_TEXTRule(), highlightingConfig.SOY_DOC);
    bindTerminalRule(grammar.getSOY_DOC_TAG_PARAMRule(), highlightingConfig.SOY_DOC_TAG);
    bindTerminalRule(grammar.getSOY_DOC_TAG_PARAM_OPTIONALRule(), highlightingConfig.SOY_DOC_TAG);
    bindTerminalRule(grammar.getSOY_DOC_IDENTRule(), highlightingConfig.SOY_DOC_IDENT);
    bindKeywords("\\{.*", highlightingConfig.COMMAND);
    bindKeywords(".*\\}", highlightingConfig.COMMAND);
  }
}
