// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.scoping;

import net.vtst.ow.eclipse.less.LessMessages;
import net.vtst.ow.eclipse.less.less.LessPackage;
import net.vtst.ow.eclipse.less.less.MixinUtils;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.xtext.diagnostics.Diagnostic;
import org.eclipse.xtext.diagnostics.DiagnosticMessage;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.linking.impl.LinkingDiagnosticMessageProvider;

import com.google.inject.Inject;

public class LessLinkingDiagnosticMessageProvider extends LinkingDiagnosticMessageProvider {
  
  @Inject
  LessMessages messages;

  public DiagnosticMessage getUnresolvedProxyMessage(ILinkingDiagnosticContext context) {
    EClass referenceType = context.getReference().getEReferenceType();
    if (referenceType.getClassifierID() == LessPackage.VARIABLE_DEFINITION_IDENT) {
      String message = String.format(messages.getString("unresolved_variable"), context.getLinkText());
      return new DiagnosticMessage(message, Severity.ERROR, Diagnostic.LINKING_DIAGNOSTIC);      
    } else if (referenceType.getClassifierID() == LessPackage.HASH_OR_CLASS) {
      String message = String.format(messages.getString("unresolved_mixin"), context.getLinkText());
      return new DiagnosticMessage(message, Severity.ERROR, Diagnostic.LINKING_DIAGNOSTIC);            
    } else if (referenceType.getClassifierID() == LessPackage.AT_VARIABLE_REF_TARGET) {
      if (MixinUtils.isMixinParameterName(context.getContext())) return null;
    }
    return super.getUnresolvedProxyMessage(context);
  }
  
}
