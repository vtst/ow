package net.vtst.ow.eclipse.soy.parser;

import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Pattern;

import net.vtst.eclipse.easyxtext.parser.EasyScanner;
import net.vtst.eclipse.easyxtext.parser.EasyScanner.EasyMatchResult;
import net.vtst.eclipse.easyxtext.parser.EasyScanner.EasyToken;
import net.vtst.ow.eclipse.soy.SoyMessages;
import net.vtst.ow.eclipse.soy.parser.antlr.internal.InternalSoyLexer;
import net.vtst.ow.eclipse.soy.parser.antlr.internal.InternalSoyParser;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.Token;

import com.google.inject.Inject;

/**
 * This class is a wrapper around an ANTLR Lexer (which is intended to be an InternalSoyLexer).
 * It implements context-sensitive parsing of strings.
 */
public class CustomizedSoyLexer extends InternalSoyLexer {

  // This injection is made static because it seems that Xtext does not always initialize
  // lexers by dependency injection.
  @Inject
  static private SoyMessages messages;
    
  public CustomizedSoyLexer() {
    buildContextTransitionTable();
    resetScanner();
  } 
  public CustomizedSoyLexer(CharStream input) {
    super(input);
    buildContextTransitionTable();
    resetScanner();
  }
  public CustomizedSoyLexer(CharStream input, RecognizerSharedState state) {
    super(input, state);
    buildContextTransitionTable();
    resetScanner();
  }
  
  EasyScanner scanner;
  private void resetScanner() {
    scanner = new EasyScanner(input);
  }
  private void resetScanner(CharStream input) {
    scanner.setInputCharStream(input);
  }
  public void setCharStream(CharStream input) {
    super.setCharStream(input);
    resetScanner(input);
  }

  
  // **************************************************************************
  // Management of contexts

  // The lexer may be in three different context. Parsing of string behaves differently in each
  // context.
  private final static int CONTEXT_RAW_TEXT = 0;
  private final static int CONTEXT_COMMAND = 1;
  private final static int CONTEXT_COMMAND_2 = 2;
  private final static int CONTEXT_COMMAND_CALL = 3;
  private final static int CONTEXT_HTML_TAG = 4;
    
  // Transition table between context.
  // newContext = contextTransition[currentcontext][token]
  private static final String[] tokenNames = InternalSoyParser.tokenNames;
  private static int[][] contextTransitions = null;
  private final static int OPENING_BRACE = getTokenIndex("'{'");
  private final static int COMMAND_LITERAL = getTokenIndex("'{literal}'");
  private final static int COMMAND_LITERAL_2 = getTokenIndex("'{{literal}}'");

  private static int getTokenIndex(String tokenName) {
    for (int i = 0; i < tokenNames.length; ++i) {
      if (tokenNames[i].equals(tokenName)) return i;
    }
    return -1;
  }
  private static void buildContextTransitionTable() {
    if (contextTransitions != null) return;
    contextTransitions = new int[5][tokenNames.length];
    
    // TODO Use some regexps
    for (int i = 0; i < tokenNames.length; ++i) {      
      // Transitions from CONTEXT_RAW_TEXT
      if (tokenNames[i].startsWith("'{call") ||
          tokenNames[i].startsWith("'{delcall") ||
          tokenNames[i].startsWith("'{{call") ||
          tokenNames[i].startsWith("'{{delcall")) {
        contextTransitions[CONTEXT_RAW_TEXT][i] = CONTEXT_COMMAND_CALL;
      } else if (tokenNames[i].startsWith("'{{") && !tokenNames[i].endsWith("}'")) {
        contextTransitions[CONTEXT_RAW_TEXT][i] = CONTEXT_COMMAND_2;
      } else if (tokenNames[i].startsWith("'{") && !tokenNames[i].endsWith("}'")) {
        contextTransitions[CONTEXT_RAW_TEXT][i] = CONTEXT_COMMAND;
      } else if (tokenNames[i].equals("'<'")) {
        contextTransitions[CONTEXT_RAW_TEXT][i] = CONTEXT_HTML_TAG;        
      } else {
        contextTransitions[CONTEXT_RAW_TEXT][i] = CONTEXT_RAW_TEXT;                
      }

      // Transitions from CONTEXT_COMMAND, CONTEXT_COMMAND_2 and CONTEXT_COMMAND_CALL
      if (tokenNames[i].endsWith("}'")) {
        contextTransitions[CONTEXT_COMMAND][i] = CONTEXT_RAW_TEXT;
        contextTransitions[CONTEXT_COMMAND_2][i] = CONTEXT_RAW_TEXT;
        contextTransitions[CONTEXT_COMMAND_CALL][i] = CONTEXT_RAW_TEXT;
      } else {
        contextTransitions[CONTEXT_COMMAND][i] = CONTEXT_COMMAND;
        contextTransitions[CONTEXT_COMMAND_2][i] = CONTEXT_COMMAND_2;
        contextTransitions[CONTEXT_COMMAND_CALL][i] = CONTEXT_COMMAND_CALL;
      }
      
      // Transition from CONTEXT_HTML_TAG
      if (tokenNames[i].startsWith("'{call") ||
          tokenNames[i].startsWith("'{delcall") ||
          tokenNames[i].startsWith("'{{call") ||
          tokenNames[i].startsWith("'{{delcall")) {
        contextTransitions[CONTEXT_RAW_TEXT][i] = CONTEXT_COMMAND_CALL;
      } else if (tokenNames[i].startsWith("'{{")) {
        contextTransitions[CONTEXT_HTML_TAG][i] = CONTEXT_COMMAND_2;
      } else if (tokenNames[i].startsWith("'{")) {
        contextTransitions[CONTEXT_HTML_TAG][i] = CONTEXT_COMMAND;
      } else if (tokenNames[i].equals("'>'")) {
        contextTransitions[CONTEXT_HTML_TAG][i] = CONTEXT_RAW_TEXT;        
      } else {
        contextTransitions[CONTEXT_HTML_TAG][i] = CONTEXT_HTML_TAG;                
      }
    }
  }

  private int context = CONTEXT_RAW_TEXT;

  private void updateContext(Token token) {
    int tokenType = token.getType();
    if (tokenType < 0) return;
    this.context = contextTransitions[this.context][token.getType()];
  }
  
  
  // **************************************************************************
  // Parsing functions

  // Strings
  
  // TODO u123 at the end
  static final Pattern singleQuoteStringPattern = Pattern.compile(
      "\\A([^'\\\\\\n\\r\\{\\}]|\\\\([nrtbf\\\\'\"]|u[0-9a-f]{4}|([^nrtbf\\\\'\"u]|[u][0-9a-f]{0,3}[^0-9a-f])))*'");
  static final Pattern singleQuoteStringPattern2 = Pattern.compile(
      "\\A([^'\\\\\\n\\r\\{\\}]|\\\\([nrtbf\\\\'\"]|u[0-9a-f]{4}|([^nrtbf\\\\'\"u]|[u][0-9a-f]{0,3}[^0-9a-f]))|(\\{(\\{)*)|(\\}(\\})*))*?'");
  static final Pattern doubleQuoteStringPattern = Pattern.compile(
      "\\A([^\"\\\\\\n\\r\\{\\}]|\\\\([nrtbf\\\\'\"]|u[0-9a-f]{4}|([^nrtbf\\\\'\"u]|[u][0-9a-f]{0,3}[^0-9a-f])))*\"");
  static final Pattern doubleQuoteStringPattern2 = Pattern.compile(
      "\\A([^\"\\\\\\n\\r\\{\\}]|\\\\([nrtbf\\\\'\"]|u[0-9a-f]{4}|([^nrtbf\\\\'\"u]|[u][0-9a-f]{0,3}[^0-9a-f]))|(\\{(\\{)*)|(\\}(\\})*))*?\"");

  private Pattern getPatternForStringInCommand(boolean dq, boolean command2) {
    if (dq) {
      if (command2) return doubleQuoteStringPattern2;
      else return doubleQuoteStringPattern;
    } else {
      if (command2) return singleQuoteStringPattern2;
      else return singleQuoteStringPattern;
    }
  }
  
  private Token parseStringInCommand(Token token, boolean dq, boolean command2) {
    EasyMatchResult result = scanner.match(getPatternForStringInCommand(dq, command2));
    EasyToken newToken = result.addToToken(token);
    if (result.matchGroup(3) != null) {
      newToken.setAsError(String.format(messages.getString("illegal_escape_sequence"), result.matchGroup(3)));
    } else if (command2 && (result.matchGroup(5) != null || result.matchGroup(7) != null)) {
      newToken.setAsError(String.format(messages.getString("illegal_brace"), result.matchGroup(3)));
    }
    return newToken;    
  }

  // Literals

  static final Pattern closeLiteralPattern = Pattern.compile(
      "\\{/literal\\}|\\{\\{/literal\\}\\}");
  
  private Token parseLiteralCommand(Token token) {
    tokenQueue.add(scanner.scan(closeLiteralPattern).getToken(RULE_STRING_SQ, DEFAULT_TOKEN_CHANNEL));
    return token;
  }

  // Soy docs

  static final Pattern soyDocPattern = Pattern.compile(
      "\\A(.*?)((\\*/)|((@param([\\?])? +)([a-zA-Z_][a-zA-Z0-9_]*)))", Pattern.DOTALL);
      // "\\A((.|[\\n\\r])*)((\\*/)|((@param +)([a-zA-Z_][a-zA-Z0-9_]*)))");
  private Token parseSoyDoc(Token token) {
    while (true) {
      EasyMatchResult matchResult = scanner.match(soyDocPattern);
      if (!matchResult.matched()) {
        break;
      }
      tokenQueue.add(matchResult.getToken(RULE_SOY_DOC_TEXT, DEFAULT_TOKEN_CHANNEL, 1));
      if (matchResult.matchGroup(3) == null) {
        tokenQueue.add(matchResult.getToken(
            (matchResult.matchGroup(6) == null ? RULE_SOY_DOC_TAG_PARAM : RULE_SOY_DOC_TAG_PARAM_OPTIONAL), 
            DEFAULT_TOKEN_CHANNEL, 5));  // @param
        tokenQueue.add(matchResult.getToken(RULE_SOY_DOC_IDENT, DEFAULT_TOKEN_CHANNEL, 7));  // ident
      } else {
        tokenQueue.add(matchResult.getToken(RULE_SOY_DOC_CLOSE, DEFAULT_TOKEN_CHANNEL, 3));  // * /
        break;
      }
    }
    return token;
  }
  
  // Comments
  
  static final Pattern soySlCommentDirective = Pattern.compile(
      "\\A([ ]*)((@function)|(@printDirective))");
  static final Pattern soySlComment = Pattern.compile(
      "\\A.*[\\r]?[\\n]");
  
  private Token parseSlComment(Token token) {
    EasyMatchResult matchResult = scanner.match(soySlCommentDirective);
    if (!matchResult.matched()) {
      EasyMatchResult matchResult2 = scanner.match(soySlComment);
      return matchResult2.addToToken(token);
    }
    tokenQueue.add(matchResult.getToken(RULE_WS, DEFAULT_TOKEN_CHANNEL, 1));
    int tokenType;
    if (matchResult.matchGroup(3) == null) tokenType = RULE_PRINT_DIRECTIVE_TAG;
    else tokenType = RULE_FUNCTION_TAG;
    tokenQueue.add(matchResult.getToken(tokenType, DEFAULT_TOKEN_CHANNEL, 2));
    return token;
  }
  
  // **************************************************************************
  // Implementation of TokenSource

  private final Queue<Token> tokenQueue = new LinkedList<Token>();
  
  public String getErrorMessage(Token t) {
    String message = EasyScanner.getErrorMessage(t, messages.getString("lexing_error"));
    if (message == null) message = super.getErrorMessage(t);
    return message;
  }


  public Token nextToken() {
    if (!tokenQueue.isEmpty()) {
      return tokenQueue.poll();
    }
    Token token = super.nextToken();
    if (token == null) return token;
    int tokenType = token.getType();
    switch (tokenType) {
    case RULE_STRING_SQ:
    case RULE_STRING_DQ:
      switch (context) {
      case CONTEXT_RAW_TEXT: return token;
      case CONTEXT_COMMAND: return parseStringInCommand(token, tokenType == RULE_STRING_DQ, false);
      case CONTEXT_COMMAND_2: return parseStringInCommand(token, tokenType == RULE_STRING_DQ, true);
      case CONTEXT_COMMAND_CALL:
        token.setType(RULE_CALL_COMMAND_ATTRIBUTE_DQ);
        return token;
      case CONTEXT_HTML_TAG: return token;
      }
    case RULE_PRINT_IDENT:
      CommonToken firstToken = new CommonToken(token);
      firstToken.setType(OPENING_BRACE);
      firstToken.setStopIndex(firstToken.getStartIndex());
      firstToken.setText("{");
      CommonToken secondToken = new CommonToken(token);
      secondToken.setStartIndex(secondToken.getStartIndex() + 1);
      secondToken.setType(InternalSoyLexer.RULE_IDENT);
      secondToken.setText(token.getText().substring(1));
      tokenQueue.add(secondToken);
      return firstToken;
    case RULE_SOY_DOC_OPEN:
      return parseSoyDoc(token);
    case RULE_SL_COMMENT:
      return parseSlComment(token);
    default:
      // This cannot be a switch case because COMMAND_LITERAL is not a constant.
      if (tokenType == COMMAND_LITERAL || tokenType == COMMAND_LITERAL_2) {
        return parseLiteralCommand(token);
      }
      updateContext(token);
      return token;
    }    
  }

}
