// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.soy.ui.syntaxcoloring;

import net.vtst.eclipse.easyxtext.ui.syntaxcoloring.EasyHighlightingConfiguration;
import net.vtst.eclipse.easyxtext.ui.syntaxcoloring.EasyTextAttribute;

import org.eclipse.swt.SWT;

public class SoyHighlightingConfiguration extends EasyHighlightingConfiguration {
  
  // Every highlighting rule should have a description text in LessUiMessages.property
  // (highlighting__ followed by the id string)
  public EasyTextAttribute DEFAULT = new EasyTextAttribute();
  public EasyTextAttribute COMMAND = new EasyTextAttribute(127, 0, 85, SWT.BOLD);
  public EasyTextAttribute COMMAND_CONTENTS = new EasyTextAttribute(0, 0, 255, SWT.NORMAL);
  public EasyTextAttribute COMMENT = new EasyTextAttribute(63, 127, 95, SWT.NORMAL);
  public EasyTextAttribute TEMPLATE_IDENT = new EasyTextAttribute(0, 0, 192, SWT.BOLD);
  public EasyTextAttribute STRING = new EasyTextAttribute(127, 159, 191, SWT.NORMAL);
  public EasyTextAttribute HTML_TAG = new EasyTextAttribute(127, 127, 0, SWT.BOLD);
  public EasyTextAttribute HTML_ATTRIBUTE = new EasyTextAttribute(127, 127, 0, SWT.ITALIC);
  public EasyTextAttribute SOY_DOC = new EasyTextAttribute(63, 95, 191, SWT.NORMAL);
  public EasyTextAttribute SOY_DOC_TAG = new EasyTextAttribute(127, 159, 191, SWT.BOLD);
  public EasyTextAttribute SOY_DOC_IDENT = new EasyTextAttribute(127, 159, 191, SWT.ITALIC);

}
