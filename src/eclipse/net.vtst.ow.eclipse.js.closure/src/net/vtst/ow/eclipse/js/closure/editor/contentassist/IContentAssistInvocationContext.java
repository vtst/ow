package net.vtst.ow.eclipse.js.closure.editor.contentassist;

import org.eclipse.jface.text.ITextViewer;

public interface IContentAssistInvocationContext {

  int getInvocationOffset();

  int getPrefixLength();

  ITextViewer getViewer();

}
