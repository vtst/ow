package net.vtst.ow.eclipse.soy.parser;

import net.vtst.eclipse.easyxtext.parser.NoWhiteSpaceInStringValueConverter;

import org.eclipse.xtext.conversion.IValueConverter;
import org.eclipse.xtext.conversion.ValueConverter;
import org.eclipse.xtext.conversion.impl.AbstractDeclarativeValueConverterService;

import com.google.inject.Inject;

public class SoyValueConverterService extends AbstractDeclarativeValueConverterService {

  @Inject
  private NoWhiteSpaceInStringValueConverter noWhiteSpaceInStringValueConverter;
  
  @ValueConverter(rule = "NamespaceDottedIdent")
  public IValueConverter<String> getNamespaceDottedIdentConverter() {
      return noWhiteSpaceInStringValueConverter;
  }

  @ValueConverter(rule = "TemplateDottedIdent")
  public IValueConverter<String> getTemplateDottedIdentConverter() {
      return noWhiteSpaceInStringValueConverter;
  }

  @ValueConverter(rule = "TemplateDefinitionDottedIdent")
  public IValueConverter<String> getTemplateDefinitionDottedIdentConverter() {
      return noWhiteSpaceInStringValueConverter;
  }

  @ValueConverter(rule = "TemplateDotIdent")
  public IValueConverter<String> getTemplateDotIdentConverter() {
      return noWhiteSpaceInStringValueConverter;
  }

  @ValueConverter(rule = "TemplateDefinitionDotIdent")
  public IValueConverter<String> getTemplateDefinitionDotIdentConverter() {
      return noWhiteSpaceInStringValueConverter;
  }

}
