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

}
