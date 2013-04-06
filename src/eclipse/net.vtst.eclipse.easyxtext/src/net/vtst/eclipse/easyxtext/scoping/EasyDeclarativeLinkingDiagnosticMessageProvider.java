package net.vtst.eclipse.easyxtext.scoping;

import java.lang.reflect.Method;
import java.util.Collections;

import org.eclipse.xtext.diagnostics.DiagnosticMessage;
import org.eclipse.xtext.linking.impl.LinkingDiagnosticMessageProvider;
import org.eclipse.xtext.util.PolymorphicDispatcher;

import com.google.common.base.Predicate;

/**
 * Declarative {@code LinkingDiagnosticMessageProvider}, similar to {@code AbstractDeclarativeScopeProvider}.
 * 
 * @author Vincent Simonet
 */
public class EasyDeclarativeLinkingDiagnosticMessageProvider extends LinkingDiagnosticMessageProvider {
  
  private PolymorphicDispatcher.ErrorHandler<DiagnosticMessage> errorHandler = 
      new PolymorphicDispatcher.NullErrorHandler<DiagnosticMessage>();
  
  protected Predicate<Method> getPredicate(ILinkingDiagnosticContext context) {
    String methodName = "getUnresolvedProxyMessage_" + context.getReference().getEType().getName();
    return PolymorphicDispatcher.Predicates.forName(methodName, 1);
  }
  
  public DiagnosticMessage getUnresolvedProxyMessage(final ILinkingDiagnosticContext context) {
    Predicate<Method> predicate = getPredicate(context);
    PolymorphicDispatcher<DiagnosticMessage> dispatcher = new PolymorphicDispatcher<DiagnosticMessage>(
        Collections.singletonList(this), predicate, errorHandler) {
      @Override
      protected DiagnosticMessage handleNoSuchMethod(Object... params) {
        return EasyDeclarativeLinkingDiagnosticMessageProvider.super.getUnresolvedProxyMessage(context);
      }
    };
    return dispatcher.invoke(context);
  }

}
