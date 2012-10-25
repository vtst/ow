// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.ui.syntaxcoloring;

import net.vtst.eclipse.easyxtext.ui.syntaxcoloring.EasyHighlightingConfiguration;
import net.vtst.eclipse.easyxtext.ui.syntaxcoloring.EasyTextAttribute;

import org.eclipse.swt.SWT;

public class LessHighlightingConfiguration extends EasyHighlightingConfiguration {
  
  // Every highlighting rule should have a description text in LessUiMessages.property
  // (highlighting__ followed by the id string)
  public EasyTextAttribute DEFAULT = new EasyTextAttribute();
  public EasyTextAttribute AT_KEYWORD = new EasyTextAttribute(127, 0, 85, SWT.BOLD + SWT.ITALIC);
  public EasyTextAttribute COMMENT = new EasyTextAttribute(63, 127, 95, SWT.NORMAL);
  public EasyTextAttribute PROPERTY = new EasyTextAttribute(127, 0, 85, SWT.BOLD);
  public EasyTextAttribute SELECTOR = new EasyTextAttribute(0, 0, 192, SWT.BOLD);
  public EasyTextAttribute AMPERSAND = new EasyTextAttribute(9, 150, 255, SWT.BOLD + SWT.ITALIC);
  public EasyTextAttribute STRING = new EasyTextAttribute(127, 159, 191);
  public EasyTextAttribute DEPRECATED_SELECTOR_INTERPOLATION = new EasyTextAttribute(210, 70, 0);
  public EasyTextAttribute VARIABLE_DEFINITION = new EasyTextAttribute(0, 0, 192, SWT.BOLD);
  public EasyTextAttribute VARIABLE_USE = new EasyTextAttribute(0, 0, 192, SWT.NORMAL);
  public EasyTextAttribute MIXIN_CALL = new EasyTextAttribute(0, 0, 192, SWT.NORMAL);
  public EasyTextAttribute MEDIA_QUERY_KEYWORD = new EasyTextAttribute(127, 0, 85, SWT.BOLD + SWT.ITALIC);
  public EasyTextAttribute MEDIA_FEATURE = new EasyTextAttribute(127, 0, 85, SWT.BOLD);
  public EasyTextAttribute NUMERIC_LITERAL = new EasyTextAttribute();
  
}
