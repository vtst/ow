package net.vtst.ow.eclipse.js.closure.contentassist;

import java.util.List;

import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;
import net.vtst.ow.eclipse.js.closure.util.Utils;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.internal.text.html.BrowserInformationControl;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;
import org.eclipse.jface.text.link.LinkedModeUI.IExitPolicy;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;
import org.eclipse.wst.jsdt.internal.ui.text.java.hover.JavadocHover;
import org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposal;

/**
 * This class is a base class for {@code ClosureCompletionProposal}.  It implements all the
 * features which are independent of the closure compiler, as well as some lazyness/caching.
 * @author Vincent Simonet
 */
@SuppressWarnings("restriction")
public abstract class AbstractCompletionProposal 
    implements ICompletionProposal, ICompletionProposalExtension, ICompletionProposalExtension2, ICompletionProposalExtension4, 
    ICompletionProposalExtension5, ICompletionProposalExtension6, IJavaCompletionProposal {

  // **************************************************************************
  // Constructor

  private IContentAssistInvocationContext context;
  private String completionString;
  
  /**
   * Create a new completion proposal.
   * @param completionString  The completion string, which is used to determine if the
   *   current text entered by the user matches the completion.
   */
  public AbstractCompletionProposal(IContentAssistInvocationContext context, String completionString) {
    this.context = context;
    this.completionString = completionString;
  }
  
  /**
   * @return  The name of the image, or null if no image for the proposal.
   */
  protected abstract String getImageName();

  // **************************************************************************
  // Display of the proposal

  @Override
  public Image getImage() {
    String name = getImageName();
    if (name == null) return null;
    else return OwJsClosurePlugin.getDefault().getImageFromRegistry(name);
  }

  @Override
  public String getDisplayString() {
    return completionString;
  }

  @Override
  public StyledString getStyledDisplayString() {
    StyledString result = new StyledString();
    result.append(completionString);
    return result;
  }

  // **************************************************************************
  // Validation as you type of the proposal
  
  private int matchLength = 0;
  private boolean nextCharDoesNotMatch = false;
  
  /* (non-Javadoc)
   * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#validate(org.eclipse.jface.text.IDocument, int, org.eclipse.jface.text.DocumentEvent)
   */
  @Override
  public boolean validate(IDocument document, int offset, DocumentEvent event) {
    // Update matchLength if there is an event which affect the known match.
    int invocationOffset = context.getInvocationOffset();
    if (event != null &&
        event.getOffset() < invocationOffset + matchLength + (nextCharDoesNotMatch ? 1 : 0) &&
        event.getOffset() + event.getLength() >= invocationOffset) {
      nextCharDoesNotMatch = false;
      matchLength = Math.min(matchLength, Math.max(0, event.getOffset() - invocationOffset));
    }
    
    // Test whether some additional characters match
    if (!nextCharDoesNotMatch) {
      int prefixLength = context.getPrefixLength();
      try {
        while (matchLength < offset - invocationOffset) {
          if (prefixLength + matchLength < completionString.length() &&
              document.getChar(invocationOffset + matchLength) == completionString.charAt(prefixLength + matchLength)) {
            ++matchLength;
          } else {
            break;
          }
        }
      } catch (BadLocationException e) {
        assert false;
      }
    }
    return (matchLength >= offset - invocationOffset);
  }
  
  // **************************************************************************
  // Application
  
  @Override
  public boolean isAutoInsertable() {
    return false;
  }
  
  /**
   * A fragment represents a piece of text that will be inserted when the completion proposal
   * is selected.
   */
  public static class Fragment {
    private String text;
  
    public Fragment(String text) { 
      this.text = text;
    }
    
    private String getText() {
      return this.text;
    }
  }
  
  /**
   * A linked fragment will be inserted as a linked model, to be filled by the user.
   */
  public static class LinkedFragment extends Fragment {
    public LinkedFragment(String text) {
      super(text);
    }
  }
  
  /**
   * Store the region to select once the completion proposal is selected.  Set by
   * {@code setUpLinkedMode} and read by {@code getSelection}. 
   */
  private Point regionToSelect = null;
  
  /**
   * Get the fragments to be inserted when the selection proposal is selected.
   * @return  The fragments to be inserted.
   */
  protected abstract List<Fragment> makeFragments();
 
  /**
   * Get the exit characters for the linked mode.
   * @return  An array, which should not be null or empty.
   */
  protected abstract char[] makeExitCharactersForLinkedMode();
  
  List<Fragment> cachedFragments = null;
  private List<Fragment> getFragments() {
    if (cachedFragments == null) cachedFragments = makeFragments();
    return cachedFragments;
  }
  
  /**
   * Compute the whole string to be inserted, from the fragments.
   * @return  The string to be inserted.
   */
  private String getReplacementString(char trigger) {
    StringBuffer buffer = new StringBuffer();
    for (Fragment fragment: getFragments()) buffer.append(fragment.getText());
    // This is to insert the trigger character if it is not in the completion proposal itself.
    if (Utils.contains(this.getTriggerCharacters(), trigger)) {
      String triggerString = Character.toString(trigger);
      if (buffer.indexOf(triggerString) < 0) buffer.append(triggerString);
    }
    return buffer.toString();
  }
  
  @Override
  public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
    String replacementString = getReplacementString(trigger);
    try {
      IDocument document = viewer.getDocument();
      int replacementOffset = context.getInvocationOffset() - context.getPrefixLength();
      document.replace(
          replacementOffset, context.getPrefixLength() + offset - context.getInvocationOffset(), 
          replacementString);
      regionToSelect = new Point(context.getInvocationOffset() + replacementString.length() - context.getPrefixLength(), 0);
      setUpLinkedMode(document, replacementOffset, getFragments());
    } catch (BadLocationException e) {
      assert false;
    }        
  }
  
  /**
   * Exit policy for the linked mode.
   */
  protected static final class ExitPolicy implements IExitPolicy {
    
    final char[] exitCharacters;
    private final IDocument document;
  
    public ExitPolicy(char[] exitCharacters, IDocument document) {
      this.exitCharacters = exitCharacters;
      this.document = document;
    }
  
    /*
     * @see org.eclipse.wst.jsdt.internal.ui.text.link.LinkedPositionUI.ExitPolicy#doExit(org.eclipse.wst.jsdt.internal.ui.text.link.LinkedPositionManager, org.eclipse.swt.events.VerifyEvent, int, int)
     */
    public ExitFlags doExit(LinkedModeModel environment, VerifyEvent event, int offset, int length) {

      if (Utils.contains(exitCharacters, event.character)) {
        if (environment.anyPositionContains(offset))
          return new ExitFlags(ILinkedModeListener.UPDATE_CARET, false);
        else
          return new ExitFlags(ILinkedModeListener.UPDATE_CARET, true);
      }
  
      switch (event.character) {
        case ';':
          return new ExitFlags(ILinkedModeListener.NONE, true);
        case SWT.CR:
          // when entering an anonymous class as a parameter, we don't want
          // to jump after the parenthesis when return is pressed
          if (offset > 0) {
            try {
              if (document.getChar(offset - 1) == '{')
                return new ExitFlags(ILinkedModeListener.EXIT_ALL, true);
            } catch (BadLocationException e) {
            }
          }
          return null;
        default:
          return null;
      }
    }
  
  }
  
  /**
   * Set up the linked mode for the replacement text.
   * @param document  The document in which the replacement is made.
   * @param replacementOffset  The offset at which the replacement is made.
   * @param fragments   The fragments to insert.
   * @param closingChar  The closing character (used for the exit mode).
   * @throws BadLocationException
   */
  private void setUpLinkedMode(IDocument document, int replacementOffset, Iterable<Fragment> fragments) throws BadLocationException {
    int currentOffset = replacementOffset;
    boolean hasLinkedFragment = false;
    LinkedModeModel model = new LinkedModeModel();
    for (Fragment fragment: fragments) {
      int length = fragment.getText().length();
      if (fragment instanceof LinkedFragment) {
        LinkedPositionGroup group = new LinkedPositionGroup();
        group.addPosition(new LinkedPosition(document, currentOffset, length, LinkedPositionGroup.NO_STOP));
        model.addGroup(group);
        hasLinkedFragment = true;
      }
      currentOffset += length;
    }
    if (!hasLinkedFragment) return;
  
    model.forceInstall();
  
    LinkedModeUI ui = new EditorLinkedModeUI(model, context.getViewer());
    ui.setExitPosition(context.getViewer(), currentOffset, 0, Integer.MAX_VALUE);
    ui.setExitPolicy(new ExitPolicy(makeExitCharactersForLinkedMode(), document));
    ui.setDoContextInfo(true);
    ui.setCyclingMode(LinkedModeUI.CYCLE_WHEN_NO_PARENT);
    ui.enter();
    
    IRegion region = ui.getSelectedRegion();
    regionToSelect = new Point(region.getOffset(), region.getLength());    
  }
  
  @Override
  public Point getSelection(IDocument document) {
    return regionToSelect;
  }

  /**
   * @return The trigger characters for the completion proposal.  May be empty, but should never
   * be null.
   */
  protected abstract char[] makeTriggerCharacters();
  
  private char[] triggerCharacters = null;
  
  @Override
  public char[] getTriggerCharacters() {
    if (triggerCharacters == null) triggerCharacters = makeTriggerCharacters();
    return triggerCharacters;
  }

  // **************************************************************************
  // Proposal info
  
  /**
   * @return The additional proposal info for the completion proposal.  Should never be null.
   */
  protected abstract IAdditionalProposalInfo makeAdditionalProposalInfo();
  
  private IAdditionalProposalInfo additionalProposalInfo = null;
  
  
  @Override
  public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
    if (additionalProposalInfo == null) {
      additionalProposalInfo = makeAdditionalProposalInfo();
    }
    return additionalProposalInfo.getHTMLString();
  }
  
  IInformationControlCreator informationControlCreator;
  
  public IInformationControlCreator getInformationControlCreator() {
    Shell shell = context.getViewer().getTextWidget().getShell();
    if (informationControlCreator == null && 
        shell != null && BrowserInformationControl.isAvailable(shell)) {
      /*
       * Take control creators (and link handling) out of JavadocHover,
       * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=232024
       */
      JavadocHover.PresenterControlCreator presenterControlCreator = new JavadocHover.PresenterControlCreator();
      informationControlCreator = new JavadocHover.HoverControlCreator(presenterControlCreator);
    }
    return informationControlCreator;
  }
  
  // **************************************************************************
  // Deprecated and unused methods
  
  @Override
  public String getAdditionalProposalInfo() {
    return (String) getAdditionalProposalInfo(new NullProgressMonitor());
  }
  
  @Override
  public void apply(IDocument document) {
    // This method is deprecated.
    assert false;
  }
  
  @Override
  public void selected(ITextViewer viewer, boolean smartToggle) {}
  
  @Override
  public void unselected(ITextViewer viewer) {}
  
  @Override
  public IContextInformation getContextInformation() {
    return null;
  }
  
  @Override
  public void apply(IDocument arg0, char arg1, int arg2) {
    // This method is deprecated.
    assert false;
  }
  
  @Override
  public int getContextInformationPosition() {
    return 0;
  }
    
  @Override
  public boolean isValidFor(IDocument document, int offset) {
    return false;
  }

}
