package net.vtst.ow.eclipse.soy.resource;

import net.vtst.eclipse.easyxtext.resource.EasyLocationInFileProvider;
import net.vtst.ow.eclipse.soy.soy.DelTemplate;
import net.vtst.ow.eclipse.soy.soy.RegularTemplate;
import net.vtst.ow.eclipse.soy.soy.SoyPackage;

import org.eclipse.emf.ecore.EStructuralFeature;

public class SoyLocationInFileProvider extends EasyLocationInFileProvider {

  protected EStructuralFeature _getIdentifierFeature(RegularTemplate obj) {
    return SoyPackage.eINSTANCE.getTemplate_Ident();
  }

  protected EStructuralFeature _getIdentifierFeature(DelTemplate obj) {
    return SoyPackage.eINSTANCE.getTemplate_Ident();
  }

}
