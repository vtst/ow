package net.vtst.ow.eclipse.less.linking;

import java.util.List;

import net.vtst.ow.eclipse.less.less.LessPackage;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.linking.impl.DefaultLinkingService;
import org.eclipse.xtext.linking.impl.IllegalNodeException;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.nodemodel.INode;

import com.google.inject.Inject;

public class LessLinkingService extends DefaultLinkingService {
  
  @Inject
  private IQualifiedNameConverter qualifiedNameConverter;
  
  @Inject
  private LessMixinLinkingService mixinLinkingService;
  
  public List<EObject> getLinkedObjects(EObject context, EReference ref, INode node)
      throws IllegalNodeException {
    if (LessPackage.eINSTANCE.getHashOrClassRefTarget().equals(ref.getEReferenceType())) {
      return mixinLinkingService.getLinkedObjects(context, ref, node);
    } else {
      return super.getLinkedObjects(context, ref, node);
    }
  }

}
