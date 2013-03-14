// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.resource;

import net.vtst.eclipse.easyxtext.resource.EasyLocationInFileProvider;
import net.vtst.ow.eclipse.less.less.InnerRuleSet;
import net.vtst.ow.eclipse.less.less.LessPackage;
import net.vtst.ow.eclipse.less.less.MixinDefinition;
import net.vtst.ow.eclipse.less.less.ToplevelRuleSet;
import net.vtst.ow.eclipse.less.less.VariableDefinition;

import org.eclipse.emf.ecore.EStructuralFeature;

public class LessLocationInFileProvider extends EasyLocationInFileProvider {

  protected EStructuralFeature _getIdentifierFeature(VariableDefinition obj) {
    return LessPackage.eINSTANCE.getVariableDefinition_Lhs();
  }

  protected EStructuralFeature _getIdentifierFeature(MixinDefinition obj) {
    return LessPackage.eINSTANCE.getMixinDefinition_Selector();
  }
  
  protected EStructuralFeature _getIdentifierFeature(ToplevelRuleSet obj) {
    return LessPackage.eINSTANCE.getToplevelRuleSet_Selector();
  }

  protected EStructuralFeature _getIdentifierFeature(InnerRuleSet obj) {
    return LessPackage.eINSTANCE.getInnerRuleSet_Selector();
  }

}
