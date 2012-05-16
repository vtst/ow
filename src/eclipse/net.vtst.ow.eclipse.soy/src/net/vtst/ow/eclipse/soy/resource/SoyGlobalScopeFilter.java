package net.vtst.ow.eclipse.soy.resource;

import net.vtst.ow.eclipse.soy.soy.Namespace;

import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.IEObjectDescription;

import com.google.common.base.Predicate;

/**
 * A scope filter that selects only fully qualified name, and local name which belongs to the same namespace.
 * @author Vincent Simonet
 */
public class SoyGlobalScopeFilter implements Predicate<IEObjectDescription> {
  
  private String namespace;
  
  public SoyGlobalScopeFilter(Namespace namespace) {
    if (namespace == null) {
      this.namespace = null;
    } else {
      this.namespace = namespace.getIdent();
    }
  }

  @Override
  public boolean apply(IEObjectDescription input) {
    QualifiedName qn = input.getQualifiedName();
    if (qn.getFirstSegment().isEmpty()) {
      // We get the namespace, which has been stored by SoyResourceDescriptionProvider.
      String inputNamespace = input.getUserData(SoyResourceDescriptionStrategy.NAMESPACE);
      return inputNamespace != null && inputNamespace.equals(namespace);
    } else {
      return true;
    }
  }

}
