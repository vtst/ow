package net.vtst.ow.eclipse.soy.parser;

import net.vtst.ow.eclipse.soy.parser.antlr.internal.InternalSoyLexer;
import net.vtst.ow.eclipse.soy.parser.antlr.internal.InternalSoyParser;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.IntStream;
import org.antlr.runtime.MismatchedTokenException;
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
public class CustomizedSoyLexer extends InternalSoyLexer {
  
  public CustomizedSoyLexer() { 
    super();
  }

  public CustomizedSoyLexer(CharStream stream) {
    super(stream);
  }

  // **************************************************************************
  // Tokens
    
  /**
   * The type of the token '{'.  We need to know it, in order to be able to generate
   * it later on.
   */
  private static final int TOKEN_OPENING_BRACE = getTokenType(InternalSoyParser.tokenNames, "'{'");;
  
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
  // Modes

  /**
   * The number of modes handled by the lexer.  The modes shall be indexed from 0
   * to {@code NUMBER_OF_MODES} - 1.
   */
  private final static int NUMBER_OF_MODES = 8;
  
  /** The default mode. */
  private final static int MODE_DEFAULT = 0;
  /** Within a command tag (except call) with single braces: {...} */
  private final static int MODE_COMMAND = 1;
  /** Within a command tag (except call) with double braces: {{...}} */
  private final static int MODE_COMMAND_2 = 2;
  /** Within a call command: {call ...} or {{call ...}} */
  private final static int MODE_COMMAND_CALL = 3;
  /** Within a soy doc */
  private final static int MODE_SOY_DOC = 4;
  /** Within an HTML tag: <...> */
  private final static int MODE_HTML_TAG = 5;
  /** Within a literal command: {literal}...{/literal} */
  private final static int MODE_LITERAL = 6;
  /** Within a special single-line command which shall be parsed as a directive */
  private final static int MODE_SPECIAL_SL_COMMENT = 7;
  /** A special value for modes, used in the transition table only. It indicates
   * that the previous mode has to be activated.
   */
  private final static int MODE_PREVIOUS = -1;

  /** The transition table for the modes:  {@code targetMode[currentMode][tokenType]}. */
  private static int[][] targetMode;
  
  /**
   * Test whether a token name contains a particular substring at a given position.
   * @param tokenName  The token name.
   * @param string  The substring.
   * @param pos  The position.
   * @return  true if {@code tokenName.substring(position, position + string.length()) == string}.
   */
  private static boolean tokenNameContainsSubstringAtPosition(String tokenName, String string, int pos) {
    int n = string.length();
    if (tokenName.length() < pos + n) return false;
    for (int i = 0; i < n; ++i) {
      if (tokenName.charAt(pos + i) != string.charAt(i)) return false;
    }
    return true;
  }
  
  /**
   * @param sourceMode  The source mode.
   * @param tokenType  The token type.
   * @param tokenName  The name of the token (equals {@code tokenNames[tokenType]}.
   * @return  The mode to switch into after that token in the given source mode.
   */
  private static int getTargetMode(int sourceMode, int tokenType, String tokenName) {
    // For some tokens, the new mode does not depend on the current mode.
    switch (tokenType) {
    case RULE_SOY_DOC_OPEN:
      return MODE_SOY_DOC;
    case RULE_SOY_DOC_CLOSE:
      return MODE_DEFAULT;
    }
    // In the other cases, the new mode depends on the current mode.
    switch(sourceMode) {
    case MODE_DEFAULT:
    case MODE_HTML_TAG:
      if (tokenName.startsWith("'{")) {
        if (tokenNameContainsSubstringAtPosition(tokenName, "call", 2) ||
            tokenNameContainsSubstringAtPosition(tokenName, "delcall", 2) ||
            tokenNameContainsSubstringAtPosition(tokenName, "{call", 2) ||
            tokenNameContainsSubstringAtPosition(tokenName, "{delcall", 2)) {
          return MODE_COMMAND_CALL;
        } else if (tokenNameContainsSubstringAtPosition(tokenName, "literal", 2) ||
            tokenNameContainsSubstringAtPosition(tokenName, "{literal", 2)) {
          return MODE_LITERAL;
        } else if (!tokenName.endsWith("}'")) {
          if (tokenNameContainsSubstringAtPosition(tokenName, "{", 2)) return MODE_COMMAND_2;
          else return MODE_COMMAND;
        }
      } else if (tokenName.startsWith("'<")) {
        return MODE_HTML_TAG;
      } else if (tokenName.equals("'>'")) {
        return MODE_DEFAULT;
      }
      return sourceMode;
    case MODE_COMMAND:
    case MODE_COMMAND_2:
    case MODE_COMMAND_CALL:
      if (tokenName.endsWith("}'")) return MODE_PREVIOUS;
      return sourceMode;
    case MODE_SOY_DOC:
      return MODE_SOY_DOC;
    case MODE_LITERAL:
      return MODE_PREVIOUS;
    case MODE_SPECIAL_SL_COMMENT:
      return MODE_PREVIOUS;
    default:
        assert false;
    }
    return MODE_DEFAULT;
  }
    
  /**
   * Initializes the transition table for modes.
   * @param tokenNames  The tokens of the lexer.
   */
  private static void initTransitionTable(String[] tokenNames) {
    targetMode = new int[NUMBER_OF_MODES][tokenNames.length];
    for (int tokenType = 0; tokenType < tokenNames.length; ++tokenType) {
      for (int sourceMode = 0; sourceMode < NUMBER_OF_MODES; ++sourceMode) {
        targetMode[sourceMode][tokenType] = getTargetMode(sourceMode, tokenType, tokenNames[tokenType]);
      }
    }
  }
  static { initTransitionTable(InternalSoyParser.tokenNames); }

  /** The current mode. */
  private int mode = MODE_DEFAULT;
  /** The previous mode. */
  private int previousMode = MODE_DEFAULT;

  /**
   * Switch to the mode after matching a token.
   * @param tokenType  The type of the last matched token.
   */
  private void updateModeAfterToken(int tokenType) {
    if (tokenType < 0) return;
    int newMode = targetMode[this.mode][tokenType];
    if (newMode == this.mode) return;
    if (newMode == MODE_PREVIOUS) {
      this.mode = this.previousMode;
    } else {
      this.previousMode = this.mode;
      this.mode = newMode;
    }
  }

  // **************************************************************************
  // Utility matching functions
  
  /**
   * Test whether a character is within an interval (inclusive)
   * @param i  The character to check.
   * @param lb  The lower bound.
   * @param ub  The upper bound.
   * @return  true if {@code i} is between {@code lb} and {@code ub}, inclusive. 
   */
  private boolean charInInterval(int i, char lb, char ub) {
    return (lb <= i) && (i <= ub);
  }
  
  /**
   * Test whether the look ahead characters are equal to a given string.
   * @param i  The look ahead position.
   * @param text  The string to match.
   * @return  The number of matched characters.
   */
  private int LAequals(int i, String text) {
    int n = text.length();
    for (int j = 0; j < n; ++j) {
      if (input.LA(i + j) != text.charAt(j)) return j;
    }
    return n;
  }
  
  /**
   * Consume characters from the input.
   * @param n  The number of characters to consume.
   */
  private void consume(int n) {
    input.seek(input.index() + n);
  }

  /**
   * Consume a string if it appears in the look ahead.
   * @param text  The string to consume.
   * @return  true if the string has been consumed.
   */
  private boolean consumeAndMatch(String text) {
    int i = 0;
    int n = text.length();
    while (true) {
      int c = input.LA(1);
      if (c != text.charAt(i)) return false;
      input.consume();
      ++i;
      if (i == n) return true;
    }
  }

  // **************************************************************************
  // Custom exceptions

  public static class UnterminatedStringException extends RecognitionException {
    private static final long serialVersionUID = 1L;
    public UnterminatedStringException(IntStream input) { super(input); }
    public String getMessage() { return "Unterminated string."; }
  }

  public static class IllegalEscapeSequenceException extends RecognitionException {
    private static final long serialVersionUID = 1L;
    public IllegalEscapeSequenceException(IntStream input) { super(input); }
    public String getMessage() { return "Illegal escape sequence in string."; }
  }

  public static class IllegalBraceException extends RecognitionException {
    private static final long serialVersionUID = 1L;
    public IllegalBraceException(IntStream input) { super(input); }
    public String getMessage() { return "Illegal brace in string."; }
  }


  // **************************************************************************
  // Custom matching functions

  /**
   * Lex a string literal to the end.  The method shall be called after consuming the opening
   * single or double quote.  It consumes the stream until the closing single or double quote
   * is matched.
   * @param doubleQuote  true if the opening quote is a double quote.
   * @param doubleCommand  true if the string appears in a command within double braces.
   * @throws RecognitionException  if the string is not terminated, or contains an illegal escape sequence.
   */
  private void mEndStringInCommand(boolean doubleQuote, boolean doubleCommand) throws RecognitionException {
    boolean illegalEscapeSequence = false;
    boolean illegalBrace = false;
    string_loop: while (true) {
      switch (input.LA(1)) {
        case Token.EOF:
          throw new UnterminatedStringException(input);
        case '\'':
          input.consume();
          if (doubleQuote) break;
          else break string_loop;
        case '"':
          input.consume();
          if (doubleQuote) break string_loop;
          else break;
        case '\\':
          input.consume();
          switch (input.LA(1)) {
          case 'n': case 'r': case 't': case 'b': case 'f': case '\\': case '\'': case '\"':
            input.consume();
            break;
          case 'u':
            input.consume();
            unicode_loop: for (int i = 0; i < 4; ++i) {
              int c = input.LA(1);
              if (charInInterval(c, '0', '9') || charInInterval(c, 'a', 'f')) {
                input.consume();
              } else {
                illegalEscapeSequence = true;
                break unicode_loop;
              }
            }
            break;
          default:
            illegalEscapeSequence = true;
            break;
          }
          break;
        case '{': case '}':
          if (doubleCommand) {
            if (input.LA(1) == input.LA(2)) illegalBrace = true;
          } else {
            illegalBrace = true;
          }
          input.consume();
          break;
        default:
          input.consume();
      }
    }
    if (illegalEscapeSequence) throw new IllegalEscapeSequenceException(input);
    if (illegalBrace) throw new IllegalBraceException(input);
  }

  public static class UnterminatedLiteralException extends RecognitionException {
    private static final long serialVersionUID = 1L;
    public UnterminatedLiteralException(IntStream input) { super(input); }
    public String toString() { return "Unterminated string."; }
  }

  /**
   * Lex a literal to its end.  The method shall once the opening tag is consumed.
   * It does not consume the closing tag.
   * @throws RecognitionException  if the literal is not terminated.
   */
  private void mEndLiteral() throws RecognitionException {
    literal_loop: while (true) {
      switch (input.LA(1)) {
      case Token.EOF:
        throw new UnterminatedLiteralException(input);
      case '{':
        int i = input.LA(2) == '{' ? 2 : 1;
        int n = LAequals(i + 1, "/literal}}");
        if (n >= 9) {
          if (i == 2 && n == 9) input.consume();  // Consume the first '{' in '{{/literal}'
          break literal_loop;
        }
        consume(i + n);
        break;
      default:
        input.consume();
      }
    }
  }
  
  /**
   * Single line comments are allowed only at the beginning of a line or after some
   * white space.  This function tests these conditions.
   * @return  true if a single line comment can be parsed here.
   */
  private boolean canParseSlComment() {
    if (lastToken == null) return true;
    int type = lastToken.getType();
    return (type == RULE_SL_COMMENT || type == RULE_WS);
  }
  
  /**
   * Lex a single-line comment until its end.
   * @return true if this is a special single-line comment, containing a directive to be parsed.
   */
  private boolean mSlComment() {
    whitespace_loop: while (true) {
      switch (input.LA(1)) {
      case Token.EOF:
        return false;
      case ' ': case '\t':
        input.consume();
        break;
      case '@':
        if (LAequals(2, "function") == 8 || LAequals(2, "printDirective") == 14) {
          return true;
        }
        input.consume();
        break whitespace_loop;
      default:
        break whitespace_loop;
      }
    }
    comment_loop: while (true) {
      switch (input.LA(1)) {
      case Token.EOF:
        break comment_loop;
      case '\n':
        input.consume();
        break comment_loop;
      default:
        input.consume();
      }
    }
    return false;
  }
  
  boolean soyDocAfterParam = false;
  
  /**
   * Lex a soy doc.  This method uses {@code soyDocAfterParam} as an internal state.
   * @return  The type of token to emit.
   */
  public int mSoyDoc() {
    // If we are just after a '@param', we try to match either '?' or an identifier.
    if (soyDocAfterParam) {
      switch (input.LA(1)) {
      case '?':
        input.consume();
        return RULE_SOY_DOC_TAG_PARAM_OPTIONAL;
      case ' ':
        input.consume();
        while (input.LA(1) == ' ') input.consume();
        return RULE_WS;
      default:
        soyDocAfterParam = false;
        boolean matchedAtLeastOne = true;
        ident_loop: while (true) {
          int c = input.LA(1);
          if (c == '_' || charInInterval(c, 'a', 'z') || charInInterval(c, 'A', 'Z') || 
              charInInterval(c, '0', '9') && matchedAtLeastOne) {
            input.consume();
            matchedAtLeastOne = true;
          } else {
            break ident_loop;
          }
        }
        if (matchedAtLeastOne) return RULE_SOY_DOC_IDENT;
      }
    }
    
    int n = LAequals(1, "@param");
    consume(n);
    if (n == 6) {
      soyDocAfterParam = true;
      return RULE_SOY_DOC_TAG_PARAM;
    }
    
    while (true) {
      switch (input.LA(1)) {
      case Token.EOF:
        return RULE_SOY_DOC_TEXT;
      case '*':
        input.consume();
        if (input.LA(1) == '/') {
          input.consume();
          return RULE_SOY_DOC_CLOSE;
        }
        break;
      case '@':
        int m = LAequals(2, "param");
        if (m == 5) return RULE_SOY_DOC_TEXT;
        consume(m + 1);
        break;
      default:
        input.consume();
      }
    }
  }
  
  // **************************************************************************
  // Lexer main functions
  
  private Token lastToken = null;
  
  public Token nextToken() {
    lastToken = super.nextToken();
    return lastToken;
  }
  
  public void mTokens() throws RecognitionException {
    switch (mode) {
    case MODE_LITERAL:
      mEndLiteral();
      state.type = RULE_STRING_SQ;
      state.channel = DEFAULT_TOKEN_CHANNEL;
      break;
    case MODE_SPECIAL_SL_COMMENT:
      input.consume();  // Consume the @
      if (consumeAndMatch("function")) state.type = RULE_FUNCTION_TAG;
      else if (consumeAndMatch("printDirective")) state.type = RULE_PRINT_DIRECTIVE_TAG;
      state.channel = DEFAULT_TOKEN_CHANNEL;
      break;
    case MODE_SOY_DOC:
      state.type = mSoyDoc();
      state.channel = DEFAULT_TOKEN_CHANNEL;
      break;
    default:
      super.mTokens();
        switch (state.type) {
        case RULE_IDENT:
          switch (mode) {
          case MODE_HTML_TAG:
            state.type = RULE_HTML_IDENT;
            break;
          default:
            break;
          }
          break;
        case RULE_PRINT_IDENT:
          // This goes back to the '{'.  The ident will be lexed again.
          input.seek(state.tokenStartCharIndex + 1);
          state.type = TOKEN_OPENING_BRACE;
          break;
        case RULE_STRING_SQ:
        case RULE_STRING_DQ:
          switch (mode) {
          case MODE_COMMAND: 
            mEndStringInCommand(state.type == RULE_STRING_DQ, false);
            break;
          case MODE_COMMAND_2:
            mEndStringInCommand(state.type == RULE_STRING_DQ, true);
            break;
          case MODE_COMMAND_CALL:
            state.type = RULE_CALL_COMMAND_ATTRIBUTE_DQ;
            break;
          }
          break;
        case RULE_SL_COMMENT:
          if (canParseSlComment()) {
            if (mSlComment()) {
              mode = MODE_SPECIAL_SL_COMMENT;
              return;
            }
          } else {
            state.type = RULE_ANY_OTHER_CHAR;
          }
          break;
        default:
        }
        break;
    }
    updateModeAfterToken(state.type);
  }

}
