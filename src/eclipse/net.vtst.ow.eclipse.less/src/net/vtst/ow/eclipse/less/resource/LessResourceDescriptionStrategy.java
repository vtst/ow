package net.vtst.ow.eclipse.less.resource;

import net.vtst.ow.eclipse.less.less.LessPackage;
import net.vtst.ow.eclipse.less.less.StyleSheet;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.resource.IDefaultResourceDescriptionStrategy;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.IReferenceDescription;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.util.IAcceptor;

/**
 * Resource description strategy for Less.
 * <br>
 * A call to {@code createEObjectDescriptions} returns an object description containing the whole
 * style sheet.
 * @author Vincent Simonet
 */
public class LessResourceDescriptionStrategy implements IDefaultResourceDescriptionStrategy {
  
  public static final QualifiedName STYLESHEET_NAME = QualifiedName.create("<stylesheet>");

  public boolean createEObjectDescriptions(EObject eObject, IAcceptor<IEObjectDescription> acceptor) {
    if (eObject instanceof StyleSheet) {
      acceptor.accept(EObjectDescription.create(STYLESHEET_NAME, eObject));
    }
    return false;
  }

  public boolean createReferenceDescriptions(EObject eObject, URI exportedContainerURI, IAcceptor<IReferenceDescription> acceptor) {
    return false;
  }

  public static StyleSheet getStyleSheet(IResourceDescription desc) {
    for (IEObjectDescription objectDesc : desc.getExportedObjectsByType(LessPackage.eINSTANCE.getStyleSheet())) {
      if (LessResourceDescriptionStrategy.STYLESHEET_NAME.equals(objectDesc.getQualifiedName())) {
        EObject obj = objectDesc.getEObjectOrProxy();
        if (obj instanceof StyleSheet) {
          return (StyleSheet) obj;
        }
      }
    }
    return null;
  }
  
}
