// EasyXtext
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.eclipse.easyxtext.ui.syntaxcoloring;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.xtext.ui.editor.utils.TextStyle;

/**
 * Text attribute in a
 * {@link net.vtst.eclipse.easyxtext.ui.syntaxcoloring.EasyHighlightingConfiguration}
 * .
 * 
 * <p>
 * This class allows to easily define text attributes in highlighting
 * configuration. It is basically the same as
 * {@link org.eclipse.xtext.ui.editor.utils.TextStyle}, with a few additional
 * constructors to build a text style in one line of code, and the methods
 * <code>getId</code> and <code>setId</code>, to support initialization by
 * reflection.
 * </p>
 * 
 * @author Vincent Simonet
 */
public class EasyTextAttribute extends TextStyle {

  /**
   * The ID of the style, for easy access by clients.
   */
  private String id;

  public EasyTextAttribute() {
    super();
  }

  /**
   * Create a text style with a specified color.
   * 
   * @param red
   *          The red component of the color (from 0 to 255).
   * @param blue
   *          The blue component of the color (from 0 to 255).
   * @param green
   *          The green component of the color (from 0 to 255).
   */
  public EasyTextAttribute(int red, int green, int blue) {
    super();
    setColor(red, green, blue);
  }

  /**
   * Create a text style with specified color and font style.
   * 
   * @param red
   *          The red component of the color (from 0 to 255).
   * @param blue
   *          The blue component of the color (from 0 to 255).
   * @param green
   *          The green component of the color (from 0 to 255).
   * @param style
   *          The font style.
   */
  public EasyTextAttribute(int red, int green, int blue, int style) {
    super();
    setColor(red, green, blue);
    setStyle(style);
  }

  /**
   * Create a text style with a specified font style.
   * 
   * @param style
   *          The font style.
   */
  public EasyTextAttribute(int style) {
    super();
    setStyle(style);
  }

  /**
   * Set the color of a text style by independently specifying the three color
   * components.
   * 
   * @param red
   *          The red component of the color (from 0 to 255).
   * @param blue
   *          The blue component of the color (from 0 to 255).
   * @param green
   *          The green component of the color (from 0 to 255).
   */
  public void setColor(int red, int green, int blue) {
    super.setColor(new RGB(red, green, blue));
  }

  /**
   * Set the ID of the current attribute. This method is not intended to be
   * called by client, but only by {@link EasyHighlightingConfiguration}.
   * 
   * @param id
   *          The ID of the style.
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Get the ID of the current attribute.
   * 
   * @return the ID.
   */
  public String getId() {
    return id;
  }

}
