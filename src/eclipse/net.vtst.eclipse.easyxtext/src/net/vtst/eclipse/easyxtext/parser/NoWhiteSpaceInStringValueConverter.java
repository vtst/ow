// EasyXtext
// (c) Vincent Simonet, 2011

package net.vtst.eclipse.easyxtext.parser;

import net.vtst.eclipse.easyxtext.EasyXtextMessages;

import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.conversion.impl.AbstractValueConverter;
import org.eclipse.xtext.nodemodel.INode;

import com.google.inject.Inject;

/**
 * This class implements a simple value converter for strings 
 * ({@link org.eclipse.xtext.conversion.IValueConverter}) which raises an error if the underlying tokens contains
 * whitespace.
 * <p><b>Example use:</b></p>
 * <p> This class may typically be used for dotted identifiers which are parsed by a data type rule (instead of 
 * a plain terminal rule).</p>
 * <ul>
 *   <li>Grammar definition file:
 *   <pre>
 *   terminal IDENT: ('a'..'z' | 'A'..'Z' | '_') ('a'..'z' | 'A'..'Z' | '_' | '0'..'9')*;
 *   DottedIdent returns ecore::EString: IDENT ('.' IDENT)*;
 *   </pre>
 *   </li>
 *   <li>Create a value converter service:
 *   <pre>
 *   public class MyValueConverterService extends AbstractDeclarativeValueConverterService {
 *     &#064;ValueConverter(rule = "DottedIdent")
 *     public IValueConverter<String> getDottedIdentConverter() {
 *       return new NoWhiteSpaceInStringValueConverter();
 *     }
 *   }
 *   </pre>
 *   </li>
 *   <li>And bind it in the runtime module of your Xtext-based plugin:
 *   <pre>
 *   public Class<? extends IValueConverterService> bindIValueConverterService() {
 *     return MyValueConverterService.class;
 *   }
 *   </pre>
 *   </li>
 * </ul>
 * @author Vincent Simonet
 *
 */
public class NoWhiteSpaceInStringValueConverter extends AbstractValueConverter<String> {

  @Inject
  private EasyXtextMessages messages;
  
  /** Convert a parsed string into an internal string.  The current implementation returns the same string
   * as the one passed as argument, and throws a ValueConverterException if the passed string contains one or
   * several white spaces.
   * @see org.eclipse.xtext.conversion.IValueConverter#toValue(java.lang.String, org.eclipse.xtext.nodemodel.INode)
   */
  @Override
  public String toValue(String string, INode node) throws ValueConverterException {
    if (string != null && string.indexOf(' ') != -1) {
      throw new ValueConverterException(messages.getString("no_whitespace_in_identifier"), node, null);
    }
    return string;
  }
  
  /** Convert an internal string into its parsed representation.  The current implementation returns the same
   * string as the one passed as argument.
   * @see org.eclipse.xtext.conversion.IValueConverter#toString(java.lang.Object)
   */
  @Override
  public String toString(String value) throws ValueConverterException {
    return value;
  }

}
