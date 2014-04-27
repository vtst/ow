package net.vtst.ow.eclipse.less.parser;

import net.vtst.ow.eclipse.less.parser.antlr.internal.InternalLessLexer;
import net.vtst.ow.eclipse.less.parser.antlr.internal.InternalLessParser;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.IntStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;

/**
 * A customization of the ANTLR lexer, which handles some aspects of the Soy
 * syntax which cannot be handled by a simple ANTLR lexer.
 * This customization is based on different modes.  It runs with the following
 * principle:
 * <ol>
 *   <li>Lex a token according to the current mode,</li>
 *   <li>Set the new mode according to the newly lexed token.</li>
 * </ol>
 * @author Vincent Simonet
 */
public class CustomizedLessLexer extends InternalLessLexer {
  
  public CustomizedLessLexer() { 
    super();
  }

  public CustomizedLessLexer(CharStream stream) {
    super(stream);
  }

  // **************************************************************************
  // Tokens
    
  private static final int TOKEN_OPENING_BRACE = getTokenType(InternalLessParser.tokenNames, "'{'");;
  private static final int TOKEN_CLOSING_BRACE = getTokenType(InternalLessParser.tokenNames, "'}'");;

  private static final int TOKEN_SEMI_COLON = getTokenType(InternalLessParser.tokenNames, "';'");;  
  
  /**
   * Get the type (aka the numeric index) of a token from its name.
   * @param tokenNames  The array of defined token names.
   * @param tokenName  The token name to look for.  Shall be an element of {@code tokenNames}.
   * @return  The type of the token.
   */
  private static int getTokenType(String[] tokenNames, String tokenName) {
    for (int i = 0; i < tokenNames.length; ++i) {
      if (tokenNames[i].equals(tokenName)) return i;
    }
    assert false;
    return 0;
  }

  // **************************************************************************
  // Lexer main functions
  
  private Token lastToken = null;
  private Token tokenToEmit = null;
  
  private boolean isWhitespace(int type) {
    return type == RULE_SL_COMMENT || type == RULE_ML_COMMENT || type == RULE_WS; 
  }
  
  private boolean isBlockSeparator(int type) {
    return type == TOKEN_CLOSING_BRACE || type == TOKEN_OPENING_BRACE || type == TOKEN_SEMI_COLON;
  }
    
  // Adds ';' before '}' which are not preceded by either ';' or '}'
  public Token nextToken() {
    if (tokenToEmit == null) {
      Token nextToken = super.nextToken();
      if (isWhitespace(nextToken.getType())) {
        return nextToken;
      } else if (nextToken.getType() != TOKEN_CLOSING_BRACE ||
          lastToken == null ||
          isBlockSeparator(lastToken.getType())) {
        lastToken = nextToken;
      } else {
        tokenToEmit = nextToken;
        lastToken = new CommonToken(nextToken);
        lastToken.setType(TOKEN_SEMI_COLON);
      }      
    } else {
      lastToken = tokenToEmit;
      tokenToEmit = null;
    }
    return lastToken;
  }

}
