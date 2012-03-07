package net.vtst.ow.eclipse.soy.resource;

import java.util.ArrayList;
import java.util.List;

import net.vtst.ow.eclipse.soy.soy.Declaration;
import net.vtst.ow.eclipse.soy.soy.DelTemplate;
import net.vtst.ow.eclipse.soy.soy.Namespace;
import net.vtst.ow.eclipse.soy.soy.RegularTemplate;
import net.vtst.ow.eclipse.soy.soy.SoyFile;
import net.vtst.ow.eclipse.soy.soy.Template;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.resource.IDefaultResourceDescriptionStrategy;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.IReferenceDescription;
import org.eclipse.xtext.util.IAcceptor;

import com.google.inject.Inject;

public class SoyResourceDescriptionStrategy implements IDefaultResourceDescriptionStrategy {
  
  @Inject 
  IQualifiedNameConverter converter;

  public boolean createEObjectDescriptions(EObject object,
      IAcceptor<IEObjectDescription> acceptor) {
    if (object instanceof SoyFile) {
      getEObjectDescriptions(converter, (SoyFile) object, false, acceptor);
    }
    return false;
  }

  public boolean createReferenceDescriptions(EObject eObject,
      URI exportedContainerURI, IAcceptor<IReferenceDescription> acceptor) {
    return false;
  }
  
  // The following static functions are used for the current class (SoyResourceDescriptionStrategy)
  // and for SoyScopeProvider.
  
  public static void getEObjectDescriptionsFromTemplates(IQualifiedNameConverter converter, SoyFile soyFile, boolean localNames, IAcceptor<IEObjectDescription> acceptor) {
    QualifiedName namespaceQN = getNamespaceQualifiedName(converter, soyFile.getNamespace());
    for (Template template: soyFile.getTemplate()) {
      String ident = template.getIdent();
      if (ident != null) {
        if (template instanceof RegularTemplate) {
            QualifiedName templateName = converter.toQualifiedName(ident);
            if (namespaceQN != null)
              acceptor.accept(EObjectDescription.create(namespaceQN.append(templateName.getLastSegment()), template));
            if (localNames) 
              acceptor.accept(EObjectDescription.create(templateName, template));
        } else if (template instanceof DelTemplate) {
          QualifiedName templateName = converter.toQualifiedName(ident);
          acceptor.accept(EObjectDescription.create(templateName, template));
        }
      }
    }
  }
  
  private static QualifiedName getNamespaceQualifiedName(IQualifiedNameConverter converter, Namespace namespace) {
    if (namespace == null) return null;
    String ident = namespace.getIdent();
    if (ident == null) return null;
    return converter.toQualifiedName(ident);
  }
  
  public static void getEObjectDescriptionsFromDeclarations(IQualifiedNameConverter converter, SoyFile soyFile, IAcceptor<IEObjectDescription> acceptor) {
    for (Declaration declaration: soyFile.getDeclaration()) {
      String ident = declaration.getIdent();
      if (ident != null) acceptor.accept(EObjectDescription.create(ident, declaration));
    }
  }
  
  public static void getEObjectDescriptions(IQualifiedNameConverter converter, SoyFile soyFile, boolean localNames, IAcceptor<IEObjectDescription> acceptor) {
    getEObjectDescriptionsFromTemplates(converter, soyFile, localNames, acceptor);
    getEObjectDescriptionsFromDeclarations(converter, soyFile, acceptor);
  }

  public static Iterable<IEObjectDescription> getEObjectDescriptions(IQualifiedNameConverter converter, SoyFile soyFile, boolean localNames) {
    final List<IEObjectDescription> list = new ArrayList<IEObjectDescription>();
    IAcceptor<IEObjectDescription> acceptor = new IAcceptor<IEObjectDescription>() {
      public void accept(IEObjectDescription t) {
        list.add(t);
      }
    };
    getEObjectDescriptions(converter, soyFile, localNames, acceptor);
    return list;
  }

}
