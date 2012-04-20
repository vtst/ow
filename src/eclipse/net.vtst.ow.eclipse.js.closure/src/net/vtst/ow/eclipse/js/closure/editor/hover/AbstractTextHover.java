package net.vtst.ow.eclipse.js.closure.editor.hover;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.wst.jsdt.internal.ui.text.html.HTMLTextPresenter;
import org.eclipse.wst.jsdt.ui.text.java.hover.IJavaEditorTextHover;

/**
 * Base implementation for text hover displaying HTML. Sub-classes must implement {@code getHoverHTML}.
 * @author Vincent Simonet
 */
@SuppressWarnings("restriction")
public abstract class AbstractTextHover implements IJavaEditorTextHover, ITextHoverExtension, ITextHoverExtension2 {

  private IEditorPart editor;

  /**
   * @param viewer
   * @param region
   * @return The HTML for the text hover, or null if no hover shall be shown.
   */
  protected abstract String getHoverHTML(ITextViewer viewer, IRegion region);
  
  /* (non-Javadoc)
   * @see org.eclipse.jface.text.ITextHoverExtension2#getHoverInfo2(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
   */
  @Override
  public Object getHoverInfo2(ITextViewer viewer, IRegion region) {
    return getHoverHTML(viewer, region);
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.ITextHoverExtension#getHoverControlCreator()
   * This is copied from org.eclipse.wst.jsdt.internal.ui.text.java.hover.AbstractJavaEditorTextHover
   */
  @Override
  public IInformationControlCreator getHoverControlCreator() {
    return new IInformationControlCreator() {
      @SuppressWarnings({ "deprecation" })
      public IInformationControl createInformationControl(Shell parent) {
        return new DefaultInformationControl(parent, SWT.NONE, new HTMLTextPresenter(true), EditorsUI.getTooltipAffordanceString());
      }
    };
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.ITextHover#getHoverInfo(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
   */
  @Override
  public String getHoverInfo(ITextViewer viewer, IRegion region) {
    // This method is deprecated, and replaced by getHoverInfo2
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.ITextHover#getHoverRegion(org.eclipse.jface.text.ITextViewer, int)
   */
  @Override
  public IRegion getHoverRegion(ITextViewer viewer, int offset) {
    return new Region(offset, 0);
  }

  /* (non-Javadoc)
   * @see org.eclipse.wst.jsdt.ui.text.java.hover.IJavaEditorTextHover#setEditor(org.eclipse.ui.IEditorPart)
   */
  @Override
  public void setEditor(IEditorPart editor) {
    this.editor = editor;
  }
  
  /**
   * @return The editor this text hover is bound to.
   */
  protected IEditorPart getEditor() {
    return editor;
  }

}
