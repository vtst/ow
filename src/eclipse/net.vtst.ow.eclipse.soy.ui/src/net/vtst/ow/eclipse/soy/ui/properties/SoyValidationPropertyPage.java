package net.vtst.ow.eclipse.soy.ui.properties;

import net.vtst.eclipse.easyxtext.ui.validation.config.ValidationPropertyPage;
import net.vtst.ow.eclipse.soy.ui.SoyUiMessages;
import net.vtst.ow.eclipse.soy.validation.SoyJavaValidator;

import org.eclipse.xtext.validation.AbstractDeclarativeValidator;

import com.google.inject.Inject;

public class SoyValidationPropertyPage extends ValidationPropertyPage {
  
  @Inject
  SoyJavaValidator validator;

  @Inject
  SoyUiMessages messages;

  @Override
  protected AbstractDeclarativeValidator getValidator() {
    return validator;
  }
  
  @Override
  protected String getGroupLabel(String name) {
    return messages.getString("validation_" + name);
  }

}
