// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.linking;

import net.vtst.eclipse.easyxtext.scoping.EasyDeclarativeLinkingDiagnosticMessageProvider;
import net.vtst.ow.eclipse.less.LessMessages;
import net.vtst.ow.eclipse.less.less.MixinUtils;
import net.vtst.ow.eclipse.less.validation.LessJavaValidator;

import org.eclipse.xtext.diagnostics.Diagnostic;
import org.eclipse.xtext.diagnostics.DiagnosticMessage;
import org.eclipse.xtext.diagnostics.Severity;

import com.google.inject.Inject;

public class LessLinkingDiagnosticMessageProvider extends EasyDeclarativeLinkingDiagnosticMessageProvider {
  
  @Inject
  private LessMessages messages;
  
  @Inject
  private LessJavaValidator validator;
    
}
