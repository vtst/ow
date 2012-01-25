// EasyXtext
// (c) Vincent Simonet, 2011

package net.vtst.eclipse.easyxtext.parser;

import java.nio.CharBuffer;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;

/**
 * Framework for scanning an ANTLR CharStream using Java regular expressions,
 * and generating ANTLR tokens.
 * 
 * <p>
 * This framework may be used to customized the standard ANTLR lexer generated
 * from the Xtext grammar specification of a plugin.
 * </p>
 * 
 * @author Vincent Simonet
 */
public class EasyScanner {

  // **************************************************************************
  // ReadableCharStream

  /**
   * This class wraps an ANTLR CharStream with an implementation of Readable,
   * which can be used by a Java scanner.
   */
  private static class ReadableCharStream implements Readable {
    private CharStream stream;
    private int look_ahead = 0;

    public ReadableCharStream(CharStream stream) {
      this.stream = stream;
    }

    public void reset() {
      look_ahead = 0;
    }

    public int read(CharBuffer buffer) {
      ++look_ahead;
      if (stream.LA(look_ahead) == Token.EOF)
        return -1;
      buffer.append((char) stream.LA(look_ahead));
      return 1;
    }
  }

  // **************************************************************************
  // Constructor

  private Scanner scanner;
  private CharStream input;
  private ReadableCharStream readableInput;

  public EasyScanner(CharStream input) {
    this.setInputCharStream(input);
  }

  private void prepareToScan() {
    readableInput.reset();
    scanner = new Scanner(readableInput);
  }

  public void setInputCharStream(CharStream input) {
    this.input = input;
    this.readableInput = new ReadableCharStream(input);
  }

  // **************************************************************************
  // EasyToken

  /**
   * An extension of CommonToken with facilities to handle errors and match
   * results.
   */
  public static class EasyToken extends CommonToken {

    private static final long serialVersionUID = 1L;
    private String errorMessage = null;

    private EasyToken(CharStream input, int type, int channel, int start,
        int stop) {
      super(input, type, channel, start, stop);
    }

    private EasyToken(Token token) {
      super(token);
    }

    public void setAsError(String errorMessage) {
      this.errorMessage = errorMessage;
      setType(Token.INVALID_TOKEN_TYPE);
      setChannel(Token.HIDDEN_CHANNEL);
    }

    public String getErrorMessage() {
      return this.errorMessage;
    }

    public boolean isError() {
      return (this.errorMessage != null);
    }

    public static EasyToken cast(Token token) {
      if (token instanceof EasyToken)
        return (EasyToken) token;
      return new EasyToken(token);
    }

    public static EasyToken castToError(Token token) {
      EasyToken easyToken = cast(token);
      easyToken.setType(Token.INVALID_TOKEN_TYPE);
      easyToken.setChannel(Token.HIDDEN_CHANNEL);
      return easyToken;
    }
  }

  // **************************************************************************
  // EasyResult

  /**
   * A class to store the result of a parsing operation, and to generate tokens
   * from it.
   */
  public abstract static class EasyResult {
    protected CharStream input;
    protected int index;
    protected int line;
    protected int charPositionInLine;

    public EasyResult(CharStream input) {
      this.input = input;
      this.index = input.index();
      this.line = input.getLine();
      this.charPositionInLine = input.getCharPositionInLine();
    }

    public abstract EasyToken getToken(int tokenType, int channel);

    public abstract EasyToken addToToken(Token token);

    protected EasyToken createEmptyErrorToken() {
      EasyToken token = new EasyToken(input, Token.INVALID_TOKEN_TYPE,
          Token.HIDDEN_CHANNEL, this.index, this.index);
      token.setLine(line);
      token.setCharPositionInLine(charPositionInLine);
      token.setText("");
      return token;
    }
  }

  /** A class to store the result of a match, and to generate tokens from it. */
  public static class EasyMatchResult extends EasyResult {
    protected MatchResult matchResult;

    public EasyMatchResult(CharStream input, MatchResult matchResult) {
      super(input);
      this.matchResult = matchResult;
    }

    /** Returns true if the stream matched the pattern. */
    public boolean matched() {
      return (matchResult != null);
    }

    /**
     * Return the string matched by the index-th group of the pattern. Throws
     * IndexOutOfBoundsException if there is no capturing group in the pattern
     * with the given index.
     */
    public String matchGroup(int index) {
      if (matchResult == null)
        return null;
      return matchResult.group(index);
    }

    /** Create a token from the match result. */
    public EasyToken getToken(int tokenType, int channel) {
      return getToken(tokenType, channel, 0);
    }

    /**
     * Create a token from a given group of the match result. Returns null if
     * the match did not succeed, or if the given group was not matched.
     */
    public EasyToken getToken(int tokenType, int channel, int group) {
      if (matchResult == null || matchResult.group(group) == null) {
        return createEmptyErrorToken();
      } else {
        EasyToken token = new EasyToken(input, tokenType, channel, this.index
            + matchResult.start(group), this.index + matchResult.end(group) - 1);
        token.setLine(line);
        token.setCharPositionInLine(charPositionInLine);
        token.setText(matchResult.group(group));
        return token;
      }
    }

    public EasyToken addToToken(Token token) {
      return addToToken(token, 0);
    }

    private EasyToken addToToken(Token token, int group) {
      if (matchResult == null || matchResult.group(group) == null) {
        return EasyToken.castToError(token);
      } else {
        String matchedText = matchResult.group(group);
        EasyToken easyToken = EasyToken.cast(token);
        easyToken.setStartIndex(Math.min(easyToken.getStartIndex(), index
            + matchResult.start(group)));
        easyToken.setStopIndex(Math.max(easyToken.getStopIndex(), index
            + matchResult.end(group) - 1));
        easyToken.setText(token.getText() + matchedText);
        return easyToken;
      }
    }

    public String toString() {
      if (matchResult == null)
        return "null";
      return matchResult.toString();
    }
  }

  /** A class to store the result of a scan, and to generate tokens from it. */
  public static class EasyScanResult extends EasyResult {
    protected int scanFoundIndex;

    public EasyScanResult(CharStream input, MatchResult matchResult) {
      super(input);
      if (matchResult == null)
        this.scanFoundIndex = -1;
      else
        this.scanFoundIndex = matchResult.start();
    }

    public EasyToken getToken(int tokenType, int channel) {
      return getToken(tokenType, channel, 0);
    }

    public EasyToken getToken(int tokenType, int channel, int group) {
      if (scanFoundIndex < 0) {
        return createEmptyErrorToken();
      } else {
        EasyToken token = new EasyToken(input, tokenType, channel, this.index,
            this.index + scanFoundIndex - 1);
        token.setLine(line);
        token.setCharPositionInLine(charPositionInLine);
        token.setText(input.substring(this.index, this.index + scanFoundIndex
            - 1));
        return token;
      }
    }

    public EasyToken addToToken(Token token) {
      return addToToken(token, 0);
    }

    private EasyToken addToToken(Token token, int group) {
      if (scanFoundIndex < 0) {
        return EasyToken.castToError(token);
      } else {
        String matchedText = input.substring(this.index, this.index
            + scanFoundIndex - 1);
        EasyToken easyToken = EasyToken.cast(token);
        easyToken.setStartIndex(Math.min(easyToken.getStartIndex(), index));
        easyToken.setStopIndex(Math.max(easyToken.getStopIndex(), index
            + matchedText.length()));
        easyToken.setText(token.getText() + matchedText);
        return easyToken;
      }
    }
  }

  // **************************************************************************
  // Scanning

  private void seekInput(MatchResult matchResult) {
    if (matchResult == null)
      return;
    input.seek(input.index() + matchResult.end() - matchResult.start());
  }

  private void seekInput(int offset) {
    input.seek(input.index() + offset);
  }

  public EasyScanResult scan(Pattern pattern) {
    prepareToScan();
    MatchResult m = null;
    if (scanner.findWithinHorizon(pattern, 0) != null) {
      m = scanner.match();
    }
    EasyScanResult r = new EasyScanResult(input, m);
    if (m != null)
      seekInput(m.start());
    return r;
  }

  /**
   * Match the pattern against the stream. The pattern <b>must</b> start with
   * the BOF pattern (\A). In case the stream does not match, the function
   * returns and EasyMatchResult which contains a null MatchResult.
   */
  public EasyMatchResult match(Pattern pattern) {
    prepareToScan();
    MatchResult m = null;
    // TODO: Should I set the horizon to 1 (in case it corresponds to the
    // beginning of the pattern)
    if (scanner.findWithinHorizon(pattern, 0) != null) {
      m = scanner.match();
    }
    EasyMatchResult r = new EasyMatchResult(input, m);
    seekInput(m);
    return r;
  }

  // **************************************************************************
  // Error messages

  public static String getErrorMessage(Token token, String defaultErrorMessage) {
    if (!(token instanceof EasyToken))
      return null;
    String message = ((EasyToken) token).getErrorMessage();
    if (message == null)
      return defaultErrorMessage;
    return message;
  }

}
