package net.vtst.ow.eclipse.less.ui.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.xtext.validation.AbstractDeclarativeValidator;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import com.google.inject.Inject;

import net.vtst.eclipse.easyxtext.ui.validation.ValidationPropertyPage;
import net.vtst.eclipse.easyxtext.validation.config.ConfigurableDeclarativeValidator;
import net.vtst.eclipse.easyxtext.validation.config.ConfigurableValidationMessageAcceptor;
import net.vtst.ow.eclipse.less.ui.LessUiMessages;
import net.vtst.ow.eclipse.less.validation.LessJavaValidator;

public class LessValidationPropertyPage extends ValidationPropertyPage {
  
  @Inject
  LessJavaValidator validator;

  @Inject
  LessUiMessages messages;

  @Override
  protected AbstractDeclarativeValidator getValidator() {
    return validator;
  }
  
  @Override
  protected String getGroupLabel(String name) {
    return messages.getString("validation_" + name);
  }
}
