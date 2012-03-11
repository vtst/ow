package net.vtst.ow.eclipse.js.closure.contentassist;

import org.eclipse.jface.text.ITextViewer;

public interface IContentAssistInvocationContext {

  int getInvocationOffset();

  int getPrefixLength();

  ITextViewer getViewer();

}
