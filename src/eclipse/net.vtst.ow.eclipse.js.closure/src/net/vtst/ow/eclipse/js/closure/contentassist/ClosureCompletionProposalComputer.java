package net.vtst.ow.eclipse.js.closure.contentassist;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.wst.jsdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposalComputer;

/**
 * Completion proposal computer which provides proposals from Closure compiler.
 * The provided completion proposals have the class {@code ClosureCompletionProposal}.
 * The invocation context is inspected thanks to {@code ClosureContentAssistInvocationContext}.
 * The collection of completion proposals itself is implemented in
 * {@code ClosureCompletionProposalCollector}.
 * @author Vincent Simonet
 */
public class ClosureCompletionProposalComputer implements IJavaCompletionProposalComputer {
  
  @Override
  public void sessionStarted() {
  }
  
  @Override
  public List<? extends ICompletionProposal> computeCompletionProposals(
      ContentAssistInvocationContext context, IProgressMonitor monitor) {
    ClosureContentAssistIncovationContext closureContext = new ClosureContentAssistIncovationContext(context);
    ClosureCompletionProposalCollector collector = new ClosureCompletionProposalCollector(closureContext);
    return collector.getProposals();
  }

  @Override
  public List<IContextInformation> computeContextInformation(ContentAssistInvocationContext context,
      IProgressMonitor monitor) {
    return Collections.emptyList();
  }

  @Override
  public String getErrorMessage() {
    return "";
  }

  @Override
  public void sessionEnded() {
  }

}
