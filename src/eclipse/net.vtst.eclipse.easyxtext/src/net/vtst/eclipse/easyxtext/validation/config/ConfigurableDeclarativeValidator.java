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
 * <li>Add the following binding to the RuntimeModule:
 *   <pre>
 *     public EPackage bindEPackage() {
 *       return <your-package-name>Package.eINSTANCE;
 *     }
 *   </pre>
 * </li>
 * 
 * <li>Bind an instance of your IEasyMessages in your UiModule which shall define
 * display labels for the various checks.
 * </li>
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
