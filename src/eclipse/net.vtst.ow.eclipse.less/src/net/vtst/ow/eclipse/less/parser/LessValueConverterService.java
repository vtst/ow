package net.vtst.ow.eclipse.less.parser;

import org.eclipse.xtext.conversion.IValueConverter;
import org.eclipse.xtext.conversion.ValueConverter;
import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.conversion.impl.AbstractDeclarativeValueConverterService;
import org.eclipse.xtext.nodemodel.INode;

public class LessValueConverterService extends AbstractDeclarativeValueConverterService {

  private IValueConverter<String> stringValueConverter = new IValueConverter<String>() {

    public String toValue(String string, INode node)
        throws ValueConverterException {
      return string;
    }

    public String toString(String value) throws ValueConverterException {
      return value;
    }
    
  };
  
  @ValueConverter(rule = "STRING")
  public IValueConverter<String> getSTRINGConverter() {
    return stringValueConverter;
  }
  
  private IValueConverter<String> prependAtConverter = new IValueConverter<String>() {

    public String toValue(String string, INode node)
        throws ValueConverterException {
      return "@" + string;
    }

    public String toString(String value) throws ValueConverterException {
      if (value.startsWith("@"))
        return value.substring(1);
      else
        return value;
    }
    
  };
  
  @ValueConverter(rule = "IDENT_OR_KEYWORD_PREPEND_AT")
  public IValueConverter<String> getIDENT_OR_KEYWORD_PREPEND_ATConverter() {
    return prependAtConverter;
  }

  
  /**
   * Return the string denoted by a string literal.  This is useful for URI.
   * Note that the current implementation does not unescape escape sequences.
   * @param string
   * @return
   */
  public static String getStringValue(String string) {
    if (string.length() < 2) return "";
    return string.substring(1, string.length() - 1);
  }

}
