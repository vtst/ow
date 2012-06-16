package net.vtst.ow.eclipse.less.formatting;

import java.io.IOException;
import java.util.ArrayList;

import net.vtst.ow.eclipse.less.services.LessGrammarAccess;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.AbstractRule;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.formatting.IFormatter;
import org.eclipse.xtext.formatting.IIndentationInformation;
import org.eclipse.xtext.parsetree.reconstr.IHiddenTokenHelper;
import org.eclipse.xtext.parsetree.reconstr.ITokenStream;

import com.google.inject.Inject;

public class LessFormatter implements IFormatter {
  
  @Inject
  private LessGrammarAccess grammarAccess;

  @Inject
  private IHiddenTokenHelper hiddenTokenHelper;

  @Inject(optional = true)
  private IIndentationInformation indentInfo = new IIndentationInformation() {
    public String getIndentString() {
      return "\t";
    }
  };

  public ITokenStream createFormatterStream(String indent, ITokenStream out, boolean preserveWhitespaces) {
    return new TokenStream(out);
  }
  
  public LessGrammarAccess getGrammarAccess() {
    return grammarAccess;
  }
  
  public IHiddenTokenHelper getHiddenTokenHelper() {
    return hiddenTokenHelper;
  }
  
  public IIndentationInformation getIndentInformation() {
    return indentInfo;
  }
  
  // **************************************************************************
  // Class TokenStream
  
  public class TokenStream implements ITokenStream {
    
    private ArrayList<Token> tokens = new ArrayList<Token>(128);
    private ITokenStream outputStream;

    public TokenStream(ITokenStream outputStream) {
      this.outputStream = outputStream;
    }
    
    private void format() {
      int indentLevel = 0;
      for (int i = 0, n = tokens.size(); i < n; ++i) {
        Token token = tokens.get(i);
        if (i > 0) {
          switch (tokens.get(i - 1).getKeywordChar()) {
          case '{':
            ++indentLevel;
            token.setNewLine(indentLevel);
            break;
          case '}':
            token.setNewLine(indentLevel, 2);
            break;
          case '(':
          case '[':
          case '=':
            token.deleteWhiteSpace();
            break;
          case ';':
            token.setNewLine(indentLevel);
            break;
          default:
            if (token.isWhiteSpace()) {
              token.normalizeWhiteSpace();
            }
          }            
        }
        if (i < n - 1) {
          switch (tokens.get(i + 1).getKeywordChar()) {
          case '}':
            if (indentLevel > 0) --indentLevel;
            if (i > 0) token.setNewLine(indentLevel);
            break;
          case ':':
          case ';':
          case ')':
          case ']':
          case '=':
            token.deleteWhiteSpace();
            break;
          }
        }
      }
    }
    
    public void flush() throws IOException {
      format();
      for (Token token: tokens) token.writeTo(outputStream);
      tokens.clear();
    }

    public void writeHidden(EObject grammarElement, String value) throws IOException {
      //tokens.add(new LessFormattingToken(grammarAccess, grammarElement, value, true));
    }

    public void writeSemantic(EObject grammarElement, String value) throws IOException {
      if (grammarElement instanceof Keyword) {
        System.out.println("'" + ((Keyword) grammarElement).getValue() + "'");
      } else if (grammarElement instanceof RuleCall) {
        System.out.println(((RuleCall) grammarElement).getRule().getName());
      }
      System.out.println(grammarAccess);
      System.out.println(hiddenTokenHelper);
      System.out.println(indentInfo.getIndentString());
      tokens.add(new Token(grammarElement, value, false));    
    }

  }

  // **************************************************************************
  // Class Token
  
  public class Token {
    
    private EObject grammarElement;
    private String value;
    private boolean hidden;
    private String keywordValue;

    public Token(EObject grammarElement, String value, boolean hidden) {
      this.grammarElement = grammarElement;
      this.value = value;
      this.hidden = true;
    }
    
    void writeTo(ITokenStream stream) throws IOException {
      if (hidden) stream.writeHidden(grammarElement, value);
      else stream.writeSemantic(grammarElement, value);
    }
    
    private String getKeywordValue() {
      if (keywordValue == null) {
        if (grammarElement instanceof Keyword) keywordValue = ((Keyword) grammarElement).getValue();
        else keywordValue = "";
      }
      return keywordValue;
    }
    
    char getKeywordChar() {
      String keywordValue = getKeywordValue();
      if (keywordValue.length() == 1) return keywordValue.charAt(0);
      else return '\000';
    }
    
    AbstractRule getRule() {
      if (grammarElement instanceof RuleCall) return ((RuleCall) grammarElement).getRule();
      else return null;
    }
    
    private boolean isWhiteSpace() {
      AbstractRule rule = getRule();
      return grammarAccess.getOPT_SPACERule().equals(rule) || grammarAccess.getSPACERule().equals(rule);
    }

    public void setNewLine(int indentLevel) {
      setNewLine(indentLevel, 1);
    }
    
    private String repeat(String string, int repetitions) {
      StringBuffer buffer = new StringBuffer(string.length() * repetitions);
      for (int i = 0; i < repetitions; ++i)
        buffer.append(string);
      return buffer.toString();
    }
    
    public void setNewLine(int indentLevel, int numberOfNewLines) {
      if (isWhiteSpace()) {
        value = repeat("\n", numberOfNewLines) + repeat(indentInfo.getIndentString(), indentLevel);
      }
    }
    
    public void deleteWhiteSpace() {
      if (isWhiteSpace()) {
        value = "";
      }
    }

    public void normalizeWhiteSpace() {
      if (isWhiteSpace()) {
        value = " ";
      }
    }
  }

}
