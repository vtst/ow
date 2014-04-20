// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.ui.contentassist;

import java.util.Set;

import net.vtst.ow.eclipse.less.CssProfile;
import net.vtst.ow.eclipse.less.less.Block;
import net.vtst.ow.eclipse.less.less.BlockContents;
import net.vtst.ow.eclipse.less.less.BlockUtils;
import net.vtst.ow.eclipse.less.less.Declaration;
import net.vtst.ow.eclipse.less.less.Expr;
import net.vtst.ow.eclipse.less.less.FontFaceStatement;
import net.vtst.ow.eclipse.less.less.PageStatement;
import net.vtst.ow.eclipse.less.less.Property;
import net.vtst.ow.eclipse.less.less.RawProperty;
import net.vtst.ow.eclipse.less.scoping.LessMixinScopeProvider;
import net.vtst.ow.eclipse.less.scoping.MixinScope;
import net.vtst.ow.eclipse.less.scoping.MixinScopeElement;
import net.vtst.ow.eclipse.less.ui.LessImageHelper;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.swt.graphics.Image;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.ui.IImageHelper;
import org.eclipse.xtext.ui.editor.contentassist.ContentAssistContext;
import org.eclipse.xtext.ui.editor.contentassist.ICompletionProposalAcceptor;

import com.google.inject.Inject;

/**
 * see http://www.eclipse.org/Xtext/documentation/latest/xtext.html#contentAssist on how to customize content assistant
 */
public class LessProposalProvider extends AbstractLessProposalProvider {
  
  @Inject
  private LessMixinScopeProvider mixinScopeProvider;
  
  @Inject
  private IImageHelper imageHelper;  // implemented by org.eclipse.xtext.ui.PluginImageHelper

  private CssProfile cssProfile;
  
  public LessProposalProvider() {
    this.cssProfile = CssProfile.getDefault();
  }
  
  public void addProposalsFromSet(
      Set<String> proposals, 
      String imageFile,
      ContentAssistContext context,
      ICompletionProposalAcceptor acceptor) {
    for (String proposal: proposals) {
      Image image = imageHelper.getImage(imageFile);
      acceptor.accept(createCompletionProposal(proposal, proposal, image, context));
    }    
  }

  // **************************************************************************
  // Content assist for properties

  public void complete_Property(
      BlockContents model, 
      RuleCall ruleCall,
      ContentAssistContext context,
      ICompletionProposalAcceptor acceptor) {
    complete_Property(BlockUtils.getBlock(model), ruleCall, context, acceptor);
  }
  
  public void complete_Property(
		  Block model, 
		  RuleCall ruleCall,
		  ContentAssistContext context,
		  ICompletionProposalAcceptor acceptor) {
    int ruleType = getRuleType(model); 
    addProposalsFromSet(this.cssProfile.getProperties(ruleType), LessImageHelper.PROPERTY, context, acceptor);
  }

  private int getRuleType(Block model) {
    if (model == null) return CssProfile.STYLE_RULE;
    EObject container = model.eContainer();
    if (container == null) return CssProfile.STYLE_RULE;
    if (container instanceof FontFaceStatement) {
      return CssProfile.FONTFACE_RULE;
    } else if (container instanceof PageStatement) {
      return CssProfile.PAGE_RULE;
    }
    return CssProfile.STYLE_RULE;
  }

  // **************************************************************************
  // Content assist for terms

  public void complete_Term(
      Expr model,
      RuleCall ruleCall,
      ContentAssistContext context,
      ICompletionProposalAcceptor acceptor) {
    EObject obj = model;
    while (!(obj == null || obj instanceof Declaration)) obj = obj.eContainer(); 
    if (obj != null) complete_Term(obj, ruleCall, context, acceptor);
  }

  public void complete_Term(
      Declaration model,
      RuleCall ruleCall,
      ContentAssistContext context,
      ICompletionProposalAcceptor acceptor) {
    Property property = model.getProperty();
    if (property instanceof RawProperty) {
      CssProfile.PropertyDef propertyDef = this.cssProfile.getProperty(property.getIdent());
      addProposalsFromSet(propertyDef.keywords, LessImageHelper.PROPERTY_IDENT, context, acceptor);
      addProposalsFromSet(propertyDef.strings, LessImageHelper.PROPERTY_IDENT, context, acceptor);
    }
  }

  // **************************************************************************
  // Content assist for mixin calls

  public void complete_MixinSelectors(EObject model, RuleCall ruleCall, ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
  }
  
  public void complete_HashOrClassRef(EObject model, RuleCall ruleCall, ContentAssistContext context, ICompletionProposalAcceptor acceptor) {    
    IMixinContentAssistContext mixinContext = MixinContentAssistContext.create(context.getPreviousModel());
    if (!mixinContext.isValid()) return;
    MixinScope scope = mixinScopeProvider.getScopeForCompletionProposal(model, mixinContext.getPath());
    for (MixinScopeElement element : scope.getCompletionProposals(mixinContext.getIndex())) {
      Image image = imageHelper.getImage(LessImageHelper.MIXIN_DEFINITION);
      acceptor.accept(createCompletionProposal(element.getName(), element.getName(), image, context));      
    }
  }
  
  // **************************************************************************

//  private void printAncestors(String label, EObject obj) {
//    System.out.println(label);
//    while (obj != null) {
//      System.out.println(obj);
//      obj = obj.eContainer();
//    }
//  }

  @Override
  public void createProposals(ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
    super.createProposals(context, acceptor);
  }

}
