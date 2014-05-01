// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.ui.syntaxcoloring;

import net.vtst.eclipse.easyxtext.ui.syntaxcoloring.EasyAntlrTokenToAttributeIdMapper;
import net.vtst.eclipse.easyxtext.ui.syntaxcoloring.EasyTextAttribute;
import net.vtst.ow.eclipse.less.services.LessGrammarAccess;

import com.google.inject.Inject;

public class LessAntlrTokenToAttributeIdMapper extends EasyAntlrTokenToAttributeIdMapper {
  
  @Inject
  private LessGrammarAccess grammar;

  @Inject
  protected LessHighlightingConfiguration highlightingConfig;
  
  
  @Override
  public void configure() {
    setDefaultAttribute(highlightingConfig.DEFAULT);
    bindTerminalRule(grammar.getSTRINGRule(), highlightingConfig.STRING);
    bindTerminalRule(grammar.getML_COMMENTRule(), highlightingConfig.COMMENT);
    bindTerminalRule(grammar.getSL_COMMENTRule(), highlightingConfig.COMMENT);
    bindTerminalRule(grammar.getAT_IDENTRule(), highlightingConfig.VARIABLE_USE);
    bindTerminalRule(grammar.getAMP_COLON_EXTENDRule(), highlightingConfig.MIXIN_CALL);
    bindKeyword("@", highlightingConfig.VARIABLE_USE);
    bindKeyword("@import", highlightingConfig.AT_KEYWORD);
    bindKeyword("@import-multiple", highlightingConfig.AT_KEYWORD);
    bindKeyword("@import-once", highlightingConfig.AT_KEYWORD);
    bindKeyword("@import", highlightingConfig.AT_KEYWORD);
    bindKeywordWithVendors("@", "keyframes", highlightingConfig.AT_KEYWORD);
    bindKeywordWithVendors("@", "viewport", highlightingConfig.AT_KEYWORD);
    bindKeyword("@media", highlightingConfig.AT_KEYWORD);
    bindKeyword("@page", highlightingConfig.AT_KEYWORD);
    bindKeyword("@font-face", highlightingConfig.AT_KEYWORD);
    bindKeyword("@charset", highlightingConfig.AT_KEYWORD);
    bindKeyword("and", highlightingConfig.MEDIA_QUERY_KEYWORD);
    bindKeyword("only", highlightingConfig.MEDIA_QUERY_KEYWORD);
  }

  private void bindKeywordWithVendors(String prefix, String suffix, EasyTextAttribute attribute) {
    bindKeyword(prefix + suffix, attribute);
    bindKeyword(prefix + "-webkit-" + suffix, attribute);
    bindKeyword(prefix + "-moz-" + suffix, attribute);
    bindKeyword(prefix + "-ms-" + suffix, attribute);
    bindKeyword(prefix + "-o-" + suffix, attribute);

  }

}
