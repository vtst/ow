package net.vtst.eclipse.easyxtext.validation;

import org.eclipse.xtext.validation.AbstractDeclarativeValidator;

public class ConfigurableDeclarativeValidator {
  
  public static ConfigurableValidationMessageAcceptor makeConfigurable(AbstractDeclarativeValidator validator) {
    ConfigurableValidationMessageAcceptor acceptor =
        new ConfigurableValidationMessageAcceptor(new DeclarativeValidatorInspector(validator), validator.getMessageAcceptor());
    acceptor.stateAccess = validator.setMessageAcceptor(acceptor);
    return acceptor;
  }

}
