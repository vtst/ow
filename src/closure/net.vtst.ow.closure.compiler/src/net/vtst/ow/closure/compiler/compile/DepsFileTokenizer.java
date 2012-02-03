package net.vtst.ow.closure.compiler.compile;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.vtst.ow.closure.compiler.util.StringEscapeUtils;
import net.vtst.ow.closure.compiler.util.Utils;

import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.DiagnosticType;
import com.google.javascript.jscomp.JSError;

/**
 * Tokenize a dependency file (deps.js) for the Google Closure Library.
 * @author Vincent Simonet
 */
public class DepsFileTokenizer {

  // goog.addDependency("asserts/asserts.js", ["goog.asserts", "goog.asserts.AssertionError"], ["goog.debug.Error", "goog.string"]);

  static final DiagnosticType OW_SYNTAX_ERROR = DiagnosticType.warning(
      "OW_SYNTAX_ERROR",
      "Syntax error.");

  private LineNumberReader lineReader;
  private LinkedList<Token> buffer = new LinkedList<Token>();
  private boolean eofReached = false;
  private AbstractCompiler compiler;
  private String filename;
  private Token lastToken;

  public DepsFileTokenizer(AbstractCompiler compiler, String filename, Reader reader) {
    lineReader = new LineNumberReader(reader);
    this.compiler = compiler;
    this.filename = filename;
  }

  public Token nextToken() throws IOException {
    if (eofReached ||
        buffer.isEmpty() && !fillBuffer()) return null;
    this.lastToken = buffer.removeFirst();
    return this.lastToken;
  }
  
  public Token lastToken() {
    return lastToken;
  }
  
  public boolean expect(int tokenType) throws IOException {
    Token token = nextToken();
    if (token == null || token.getType() != tokenType) {
      reportError();
      return false;
    }
    return true;
  }
  
  public void reportError() {
    Utils.reportError(
        compiler, 
        JSError.make(filename, getLineNumber(), 0, OW_SYNTAX_ERROR));    
  }
  
  public int getLineNumber() {
    return lineReader.getLineNumber();
  }
  
  private boolean fillBuffer() throws IOException {
    while (true) {
      String line = lineReader.readLine();
      if (line == null) {
        eofReached = true;
        return false;
      }
      if (!(line.isEmpty() || line.startsWith("//"))) {
        buffer.addAll(splitDepsLine(line));
        return true;
      }
    }
  }
  
  private final static String PATTERN_JS_STRING =
      "(\\\"([^\\\"\\\\]|(\\\\[\\\"]))*\\\")|(\\\'([^\\\'\\\\]|(\\\\[\\\']))*\\\')";
  private final static Pattern pattern_js_string = Pattern.compile(PATTERN_JS_STRING);
  
  private static Collection<Token> splitDepsLine(String line) {
    ArrayList<Token> result = new ArrayList<Token>();
    Matcher matcher = pattern_js_string.matcher(line);
    int i = 0;
    while (matcher.find()) {
      if (matcher.start() > i)
        addTerminals(line.substring(i, matcher.start()), result);
      result.add(new TokenString(matcher.group()));
      i = matcher.end();
    }
    if (line.length() > i)
      addTerminals(line.substring(i, line.length()), result);
    return result;
  }
  
  private static void addTerminals(String text, Collection<Token> result) {
    text = text.replace(" ", "");
    if ("goog.addDependency(".equals(text)) {
      result.add(new TokenTerminal(text, TOKEN_GOOG_DEPENDENCY));
      return;
    }
    if (",[".equals(text)) {
      result.add(new TokenTerminal(text, TOKEN_COMA_OPEN_BRACKET));
      return;
    }
    if (",".equals(text)) {
      result.add(new TokenTerminal(text, TOKEN_COMA));
      return;
    }
    if ("],[".equals(text)) {
      result.add(new TokenTerminal(text, TOKEN_BRACKET_COMA_BRACKET));
      return;
    }
    if ("]);".equals(text)) {
      result.add(new TokenTerminal(text, TOKEN_CLOSE));
      return;
    }
    if (",[],[".equals(text)) {  // first list empty
      result.add(new TokenTerminal(text, TOKEN_COMA_OPEN_BRACKET));
      result.add(new TokenTerminal(text, TOKEN_BRACKET_COMA_BRACKET));
      return;
    }
    if ("],[]);".equals(text)) {  // second list empty
      result.add(new TokenTerminal(text, TOKEN_BRACKET_COMA_BRACKET));
      result.add(new TokenTerminal(text, TOKEN_CLOSE));
      return;
    }
    if (",[],[]);".equals(text)) {  // both lists empty
      result.add(new TokenTerminal(text, TOKEN_COMA_OPEN_BRACKET));
      result.add(new TokenTerminal(text, TOKEN_BRACKET_COMA_BRACKET));
      result.add(new TokenTerminal(text, TOKEN_CLOSE));
      return;
    }
    result.add(new TokenTerminal(text, TOKEN_UNKNOWN));
  }

  // **************************************************************************
  // Tokens

  public static int TOKEN_UNKNOWN = 0;
  public static int TOKEN_GOOG_DEPENDENCY = 1;
  public static int TOKEN_COMA_OPEN_BRACKET = 3;
  public static int TOKEN_COMA = 4;
  public static int TOKEN_BRACKET_COMA_BRACKET = 5;
  public static int TOKEN_CLOSE = 6;
  public static int TOKEN_STRING = 7;

  public abstract static class Token {
    private String text;
    public Token(String text) {
      this.text = text;
    }
    public String getText() { return text; }
    public abstract int getType();
    public abstract String getString();    
  }
  
  public static class TokenString extends Token {
    private String string;
    public TokenString(String text) {
      super(text);
      this.string = parseJsString(text);
    }
    public int getType() { return TOKEN_STRING; }
    public String getString() { return string; }
    private static String parseJsString(String string) {
      int n = string.length();
      if (n < 2 ||
          string.charAt(0) != '"' && string.charAt(0) != '\'' ||
          string.charAt(n-1) != string.charAt(0)) 
        return "";
      return StringEscapeUtils.unescapeJavaScript(string.substring(1, n - 1));
    }
  }

  public static class TokenTerminal extends Token {
    private int type = TOKEN_UNKNOWN;
    public TokenTerminal(String text, int type) {
      super(text);
      this.type = type;
    }
    public int getType() { return type; }
    public String getString() { return null; }
  }

}
