package net.vtst.eclipse.easyxtext.validation;

import org.eclipse.xtext.validation.AbstractDeclarativeValidator;

public class ConfigurableDeclarativeValidator {
  
  private static boolean isConfigured(AbstractDeclarativeValidator validator) {
    return validator.getMessageAcceptor() instanceof ConfigurableValidationMessageAcceptor;
  }
  
  public static void makeConfigurable(AbstractDeclarativeValidator validator) {
    if (isConfigured(validator)) return;
    synchronized (validator) {
      if (isConfigured(validator)) return;
      ConfigurableValidationMessageAcceptor acceptor =
          new ConfigurableValidationMessageAcceptor(new DeclarativeValidatorInspector(validator), validator.getMessageAcceptor());
      acceptor.stateAccess = validator.setMessageAcceptor(acceptor);
    }
  }

}
