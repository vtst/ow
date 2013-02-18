package net.vtst.eclipse.easyxtext.validation.config;

import org.eclipse.xtext.validation.AbstractDeclarativeValidator;

/**
 * This is a purely static class for making a validator configurable.
 * 
 * To make a validator configurable:
 * <ol>
 * <li>Add the following code to the validator class (which shall inherits
 *    from AbstractDeclarativeValidator):
 *    <pre>
 *      protected boolean isResponsible(Map<Object, Object> context, EObject eObject) {
 *        ConfigurableDeclarativeValidator.makeConfigurable(this);
 *        return super.isResponsible(context, eObject);
 *      }
 *    </pre>
 * </li>
 *    
 * <li>(optional) Add some annotations {@code ConfigurableValidator} 
 *   and {@code ConfigurableCheck} into your validator class.
 * </li>
 *    
 * <li>Creates a subclass of {@code ValidationPropertyPage}.</li>
 *    
 * @author Vincent Simonet
 *
 */
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
