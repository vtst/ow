// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.ui.syntaxcoloring;

import net.vtst.eclipse.easyxtext.ui.syntaxcoloring.EasySemanticHighlightingCalculator;
import net.vtst.ow.eclipse.less.less.Declaration;
import net.vtst.ow.eclipse.less.less.MediaExpression;
import net.vtst.ow.eclipse.less.less.MediaQuery;
import net.vtst.ow.eclipse.less.less.MixinDefinition;
import net.vtst.ow.eclipse.less.less.MixinDefinitionGuard;
import net.vtst.ow.eclipse.less.less.MixinDefinitionGuards;
import net.vtst.ow.eclipse.less.less.NumericLiteral;
import net.vtst.ow.eclipse.less.services.LessGrammarAccess;
import net.vtst.ow.eclipse.less.services.LessGrammarAccess.NumericLiteralElements;

import com.google.inject.Inject;

public class LessSemanticHighlightingCalculator extends EasySemanticHighlightingCalculator {
  
  @Inject
  private LessGrammarAccess grammar;

  @Inject
  protected LessHighlightingConfiguration highlightingConfig;

  @Override
  protected void configure() {
	bindRule(grammar.getDeprecatedSelectorInterploationIdentRule(), highlightingConfig.DEPRECATED_SELECTOR_INTERPOLATION);
	bindRule(grammar.getAMPERSAND_IDENTRule(), highlightingConfig.AMPERSAND);
	bindRule(grammar.getVariableSelectorRule(), highlightingConfig.VARIABLE_USE);
    bindRule(grammar.getInnerSelectorRule(), highlightingConfig.SELECTOR);
    bindRule(grammar.getToplevelSelectorRule(), highlightingConfig.SELECTOR);
    bindRule(grammar.getHashOrClassRule(), MixinDefinition.class, highlightingConfig.SELECTOR);
    bindRule(grammar.getHashOrClassCrossReferenceRule(), highlightingConfig.MIXIN_CALL);
    bindRule(grammar.getHashOrClassCrossReferenceRule(), highlightingConfig.MIXIN_CALL);
    bindRule(grammar.getPropertyRule(), highlightingConfig.PROPERTY);
    bindRule(grammar.getVariableDefinitionIdentRule(), highlightingConfig.VARIABLE_DEFINITION);
    bindRule(grammar.getNUMBERRule(), NumericLiteral.class, highlightingConfig.NUMERIC_LITERAL);
    bindRule(grammar.getPERCENTAGERule(), NumericLiteral.class, highlightingConfig.NUMERIC_LITERAL);
    bindRule(grammar.getLENGTHRule(), NumericLiteral.class, highlightingConfig.NUMERIC_LITERAL);
    bindRule(grammar.getEMSRule(), NumericLiteral.class, highlightingConfig.NUMERIC_LITERAL);
    bindRule(grammar.getEXSRule(), NumericLiteral.class, highlightingConfig.NUMERIC_LITERAL);
    bindRule(grammar.getANGLERule(), NumericLiteral.class, highlightingConfig.NUMERIC_LITERAL);
    bindRule(grammar.getTIMERule(), NumericLiteral.class, highlightingConfig.NUMERIC_LITERAL);
    bindRule(grammar.getFREQRule(), NumericLiteral.class, highlightingConfig.NUMERIC_LITERAL);
    bindRule(grammar.getHASH_COLORRule(), NumericLiteral.class, highlightingConfig.NUMERIC_LITERAL);
    bindKeyword(":", Declaration.class, highlightingConfig.PROPERTY);
    bindRule(grammar.getMediaFeatureRule(), highlightingConfig.MEDIA_FEATURE);
    bindKeyword(":", MediaExpression.class, highlightingConfig.MEDIA_FEATURE);
    bindKeyword("not", MediaQuery.class, highlightingConfig.PROPERTY);
    // TODO Is this useful once the mixin definitions are correctly colored?
    bindKeyword("when", MixinDefinitionGuards.class, highlightingConfig.SELECTOR);
    bindKeyword("not", MixinDefinitionGuard.class, highlightingConfig.SELECTOR);
  }

}
