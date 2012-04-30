package net.vtst.ow.eclipse.less.formatting;

import net.vtst.ow.eclipse.less.services.LessGrammarAccess;

import org.eclipse.xtext.AbstractRule;
import org.eclipse.xtext.ParserRule;
import org.eclipse.xtext.parsetree.reconstr.IHiddenTokenHelper;

import com.google.inject.Inject;

public class LessHiddenTokenHelper implements IHiddenTokenHelper {

  @Inject
  LessGrammarAccess grammarAccess;

  @Override
  public boolean isWhitespace(AbstractRule rule) {
    boolean r = rule != null && (
        rule.equals(grammarAccess.getSPACERule())
        || rule.equals(grammarAccess.getOPT_SPACERule())
        || rule.equals(grammarAccess.getWSRule()));
    return r;
  }

  @Override
  public boolean isComment(AbstractRule rule) {
    return false;
  }

  @Override
  public AbstractRule getWhitespaceRuleFor(String whitespace) {
    return grammarAccess.getWSRule();
  }

  @Override
  public AbstractRule getWhitespaceRuleFor(ParserRule context, String whitespace) {
    return grammarAccess.getWSRule();
  }

}
