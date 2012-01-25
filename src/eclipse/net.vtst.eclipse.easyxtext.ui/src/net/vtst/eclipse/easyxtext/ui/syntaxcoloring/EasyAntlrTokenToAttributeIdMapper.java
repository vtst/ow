// EasyXtext
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.eclipse.easyxtext.ui.syntaxcoloring;

import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import net.vtst.eclipse.easyxtext.guice.PostInject;
import net.vtst.eclipse.easyxtext.util.Misc;

import org.antlr.runtime.Token;
import org.eclipse.xtext.TerminalRule;
import org.eclipse.xtext.parser.antlr.ITokenDefProvider;
import org.eclipse.xtext.ui.LexerUIBindings;
import org.eclipse.xtext.ui.editor.syntaxcoloring.AbstractAntlrTokenToAttributeIdMapper;
import org.eclipse.xtext.ui.editor.syntaxcoloring.DefaultHighlightingConfiguration;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Abstract class for implementing a syntactic highlighting calculator, in a declarative way.
 * Sub-classes need just to implement the method {@code configure}.
 * @author Vincent Simonet
 */
public abstract class EasyAntlrTokenToAttributeIdMapper extends AbstractAntlrTokenToAttributeIdMapper {

  /**
   * Mapping from token type to an attribute ID.  Note that it is shifted of Token.MIN_TOKEN_TYPE.
   */
  private String[] tokenTypeToAttributeID;
    
  
  /**
   * Mapping from token names to token types.
   */
  private Map<String, Integer> tokenNameToTokenType;
  
  
  /**
   * This is a re-implementation of {@code TokenTypeToStringMapper}, in order to have a better
   * initialization behavior (the original implementation directly called calculateId, what
   * makes impossible to have the super-classes initialized first.
   * @see org.eclipse.xtext.ui.editor.model.TokenTypeToStringMapper#setTokenDefProvider(org.eclipse.xtext.parser.antlr.ITokenDefProvider)
   */
  @Inject
  @Override
  public void setTokenDefProvider(@Named(LexerUIBindings.HIGHLIGHTING) ITokenDefProvider tokenDefProvider) {
    Map<Integer, String> tokenDef = tokenDefProvider.getTokenDefMap();
    tokenTypeToAttributeID = new String[tokenDef.size()];
    tokenNameToTokenType = Misc.invertMap(tokenDef);
  }
  
  
  /**
   * Get the attribute ID associated with a token type.  This is a re-implementation
   * of the one from {@code AbstractAntlrTokenToAttributeIdMapper}, because we do not use
   * the underlying machinery from {@code TokenTypeToStringMapper}.
   * @param tokenType  The token type.
   * @return  The attribute ID.
   * @see org.eclipse.xtext.ui.editor.syntaxcoloring.AbstractAntlrTokenToAttributeIdMapper#getId(int)
   */
  public String getId(int tokenType) {
    if (tokenType == Token.INVALID_TOKEN_TYPE)
      return DefaultHighlightingConfiguration.INVALID_TOKEN_ID;
    return tokenTypeToAttributeID[tokenType - Token.MIN_TOKEN_TYPE];
  }
  
  
  /**
   * Get the token name for a keyword token.  For instance, "@import" is converted
   * into "'@import'".
   * @param keyword  The keyword to convert.
   * @return  The token name.
   */
  private static String getTokenNameFromLiteral(String keyword) {
    return "'" + keyword + "'";
  }
  
  
  /**
   * Get the token name for a terminal rule.
   * @param terminalRule  The terminal rule.
   * @return  The token name for the terminal rule.
   */
  private static String getTokenNameFromTerminalRule(TerminalRule terminalRule) {
    return "RULE_" + terminalRule.getName();
  }
  
  
  /**
   * Register an attribute for a terminal rule.
   * This is a wrapper around {@code bindTokenName(String, String)}.
   * @param terminalRule
   * @param attribute  The attribute, only the ID will be used.
   */
  protected void bindTerminalRule(TerminalRule terminalRule, EasyTextAttribute attribute) {
    bindTokenName(getTokenNameFromTerminalRule(terminalRule), attribute.getId());
  }
  
  
  /**
   * Register an attribute for a terminal rule.
   * This is a wrapper around {@code bindTokenName(String, String)}.
   * @param terminalRule
   * @param attributeId
   */
  protected void bindTerminalRule(TerminalRule terminalRule, String attributeId) {
    bindTokenName(getTokenNameFromTerminalRule(terminalRule), attributeId);
  }
  
  
  /**
   * Register an attribute for a keyword terminal.
   * This is a wrapper around {@code bindTokenName(String, String)}.
   * @param keyword
   * @param attribute  The attribute, only the ID will be used.
   */
  protected void bindKeyword(String keyword, EasyTextAttribute attribute) {
    bindTokenName(getTokenNameFromLiteral(keyword), attribute.getId());
  }
  
  
  /**
   * Register an attribute for a keyword terminal.
   * This is a wrapper around {@code bindTokenName(String, String)}.
   * @param keyword
   * @param attributeId
   */
  protected void bindKeyword(String keyword, String attributeId) {
    bindTokenName(getTokenNameFromLiteral(keyword), attributeId);
  }
  

  /**
   * Register an attribute for all keywords which match a given pattern.
   * @param regex
   * @param attribute
   */
  protected void bindKeywords(String regex, EasyTextAttribute attribute) {
    bindKeywords(regex, attribute.getId());
  }
  
  
  /**
   * Register an attribute for all keywords which match a given pattern.
   * @param regex
   * @param attributeId
   */
  protected void bindKeywords(String regex, String attributeId) {
    for (Entry<String, Integer> entry : tokenNameToTokenType.entrySet()) {
      String tokenName = entry.getKey();
      if (tokenName.length() > 0 && tokenName.charAt(0) == '\'') {
        CharSequence keyword = tokenName.subSequence(1, tokenName.length() - 1);
        if (Pattern.matches(regex, keyword)) {
          tokenTypeToAttributeID[entry.getValue().intValue() - Token.MIN_TOKEN_TYPE] = attributeId;
        }
      }
    }
  }
  
  
  /**
   * Register an attribute for a token name.
   * This is a wrapper around {@code bindTokenName(String, String)}.
   * @param tokenName
   * @param attribute  (Only the ID is used.)
   */
  protected void bindTokenName(String tokenName, EasyTextAttribute attribute) {
    bindTokenName(tokenName, attribute.getId());
  }
  
  
  /**
   * Register an attribute for a token name.
   * @param tokenName
   * @param attributeId
   */
  protected void bindTokenName(String tokenName, String attributeId) {
    Integer tokenType = tokenNameToTokenType.get(tokenName);
    if (tokenType != null) tokenTypeToAttributeID[tokenType.intValue() - Token.MIN_TOKEN_TYPE] = attributeId;
  }
  
  /**
   * Set the default attribute.
   * This is a wrapper around {@code setDefaultAttribute(String)}.
   * @param attribute
   */
  protected void setDefaultAttribute(EasyTextAttribute attribute) {
    setDefaultAttribute(attribute.getId());
  }
  
  
  /**
   * Set the default attribute.
   * @param attributeId
   */
  protected void setDefaultAttribute(String attributeId) {
    for (int i = 0; i < tokenTypeToAttributeID.length; ++i) {
      if (tokenTypeToAttributeID[i] == null) tokenTypeToAttributeID[i] = attributeId;
    }
  }
  
  
  /**
   * This method shall be implemented by sub-classes, and called at initialization (e.g.
   * by adding an annotation @Inject).  It shall register rules by calling the methods
   * {@code bindTerminalRule}, {@code bindKeyword} and {@code bindTokenName}.
   */
  public abstract void configure();
  
  
  /**
   * This method is invoked automatically after dependency injection, and calls {@code configure}.
   */
  @PostInject
  public final void configurePostInject() {
    configure();
  }

  
  /* This method is no longer used.
   * (non-Javadoc)
   * @see org.eclipse.xtext.ui.editor.model.TokenTypeToStringMapper#calculateId(java.lang.String, int)
   */
  @Override
  protected String calculateId(String tokenName, int tokenType) {
    return null;
  }

}
