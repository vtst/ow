package net.vtst.eclipse.easyxtext.validation.config;

import java.lang.reflect.Field;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

public class AdditionalBooleanOption {
  
  private Field field;
  private ConfigurableValidationMessageAcceptor acceptor;

  public AdditionalBooleanOption(Field field, ConfigurableValidationMessageAcceptor acceptor) {
    this.field = field;
    this.acceptor = acceptor;
  }

  public boolean get(Resource resource) {
    return acceptor.getFieldValue(resource, this.field);
  }
  
  public boolean get(EObject obj) {
    return get(obj.eResource());
  }
  
}
