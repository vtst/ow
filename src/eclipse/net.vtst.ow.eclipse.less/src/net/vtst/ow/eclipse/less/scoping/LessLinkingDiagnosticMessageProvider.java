// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.scoping;

import net.vtst.eclipse.easyxtext.scoping.EasyDeclarativeLinkingDiagnosticMessageProvider;
import net.vtst.ow.eclipse.less.LessMessages;
import net.vtst.ow.eclipse.less.less.MixinUtils;

import org.eclipse.xtext.diagnostics.Diagnostic;
import org.eclipse.xtext.diagnostics.DiagnosticMessage;
import org.eclipse.xtext.diagnostics.Severity;

import com.google.inject.Inject;

public class LessLinkingDiagnosticMessageProvider extends EasyDeclarativeLinkingDiagnosticMessageProvider {
  
  @Inject
  LessMessages messages;
  
  public DiagnosticMessage getUnresolvedProxyMessage_AtVariableRefTarget(ILinkingDiagnosticContext context) {
    if (MixinUtils.isBoundByMixinDefinitionParameter(context.getContext())) return null;
    String message = String.format(messages.getString("unresolved_variable"), context.getLinkText());
    return new DiagnosticMessage(message, Severity.ERROR, Diagnostic.LINKING_DIAGNOSTIC);      
  }
  
  public DiagnosticMessage getUnresolvedProxyMessage_HashOrClassRefTarget(ILinkingDiagnosticContext context) {
    if (MixinUtils.isBoundByMixinDefinitionSelector(context.getContext())) return null;
    String message = String.format(messages.getString("unresolved_mixin"), context.getLinkText());
    return new DiagnosticMessage(message, Severity.ERROR, Diagnostic.LINKING_DIAGNOSTIC);            
  }
  
}
