// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.ui.labeling;

import net.vtst.ow.eclipse.less.less.CharsetStatement;
import net.vtst.ow.eclipse.less.less.FontFaceStatement;
import net.vtst.ow.eclipse.less.less.ImportStatement;
import net.vtst.ow.eclipse.less.less.InnerRuleSet;
import net.vtst.ow.eclipse.less.less.InnerSelector;
import net.vtst.ow.eclipse.less.less.MediaQuery;
import net.vtst.ow.eclipse.less.less.MediaStatement;
import net.vtst.ow.eclipse.less.less.MixinDefinition;
import net.vtst.ow.eclipse.less.less.PageStatement;
import net.vtst.ow.eclipse.less.less.StyleSheet;
import net.vtst.ow.eclipse.less.less.ToplevelRuleSet;
import net.vtst.ow.eclipse.less.less.ToplevelSelector;
import net.vtst.ow.eclipse.less.less.VariableDefinition;
import net.vtst.ow.eclipse.less.ui.LessImageHelper;
import net.vtst.ow.eclipse.less.ui.LessUiMessages;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.edit.ui.provider.AdapterFactoryLabelProvider;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.ColorDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.ui.label.DefaultEObjectLabelProvider;

import com.google.inject.Inject;

/**
 * Provides labels for a EObjects.
 * 
 * see http://www.eclipse.org/Xtext/documentation/latest/xtext.html#labelProvider
 */
public class LessLabelProvider extends DefaultEObjectLabelProvider {
  
  @Inject
  LessUiMessages messages;

  private final Styler rootStyler;
  private final Styler atKeywordStyler;
  private final Styler italicStyler;
  
	@Inject
	public LessLabelProvider(AdapterFactoryLabelProvider delegate) {
		super(delegate);

		FontData[] fontData = JFaceResources.getDefaultFont().getFontData();
		
    FontData[] boldFontData = getModifiedFontData(fontData, SWT.BOLD);
    final Font boldFont = new Font(JFaceResources.getDefaultFont().getDevice(), boldFontData);
		rootStyler = new Styler() {
      public void applyStyles(TextStyle textStyle) {
        textStyle.font = boldFont;
      }
    };

    ColorDescriptor cd = JFaceResources.getColorRegistry().getColorDescriptor(JFacePreferences.COUNTER_COLOR);
    final Color colorAtKeyword = cd.createColor(JFaceResources.getDefaultFont().getDevice());
    atKeywordStyler = new Styler() {
		  public void applyStyles(TextStyle textStyle) {
		    textStyle.foreground = colorAtKeyword;
		  }
		};
		
    FontData[] italicFontData = getModifiedFontData(fontData, SWT.ITALIC);
    final Font italicFont = new Font(JFaceResources.getDefaultFont().getDevice(), italicFontData);
    italicStyler = new Styler() {
      public void applyStyles(TextStyle textStyle) {
        textStyle.font = italicFont;
      }
    };
	}
	
	private static FontData[] getModifiedFontData(FontData[] originalData, int additionalStyle) {
	  FontData[] styleData = new FontData[originalData.length];
	  for (int i = 0; i < styleData.length; i++) {
	    FontData base = originalData[i];
	    styleData[i] = new FontData(base.getName(), base.getHeight(), base.getStyle() | additionalStyle);
	  }
	  return styleData;
	}	
	
	StyledString text(StyleSheet obj) {
	  // We are careful about null, because we don't know all possible cases.
	  Resource resource = obj.eResource();
	  String label = messages.getString("stylesheet");
	  if (resource != null) {
	    URI uri = resource.getURI();
	    if (uri != null) label = (label + " [" + uri.lastSegment() + "]");
	  }
	  return new StyledString(label, rootStyler);
	}
	
	String text(VariableDefinition obj) {
	  return obj.getVariable().getIdent();
	}
	
	String text(ToplevelRuleSet obj) {
	  StringBuffer buf = new StringBuffer();
	  boolean first = true;
	  for(ToplevelSelector selector: obj.getSelector()) {
	    ICompositeNode parserNode = NodeModelUtils.getNode(selector);
	    if (parserNode != null) {
	      if (first) first = false;
	      else buf.append(", ");
	      buf.append(parserNode.getText());
	    }
	  }
    return stripString(buf.toString());
	}
	
	String image(StyleSheet obj) { return LessImageHelper.STYLESHEET;	}
  String image(MixinDefinition obj) { return LessImageHelper.MIXIN_DEFINITION; }
  String image(VariableDefinition obj) { return LessImageHelper.VARIABLE_DEFINITION; }
  String image(ToplevelRuleSet obj) { return LessImageHelper.RULE_SET; }
  String image(ImportStatement obj) { return LessImageHelper.IMPORT_STATEMENT; }
  String image(MediaStatement obj) { return LessImageHelper.MEDIA_STATEMENT; }
  String image(PageStatement obj) { return LessImageHelper.PAGE_STATEMENT; }
  String image(FontFaceStatement obj) { return LessImageHelper.FONT_FACE_STATEMENT; }
  String image(CharsetStatement obj) { return LessImageHelper.CHARSET_STATEMENT; }
  String image(InnerRuleSet obj) { return LessImageHelper.RULE_SET; }

  String text(InnerRuleSet obj) {
    StringBuffer buf = new StringBuffer();
    boolean first = true;
    for(InnerSelector selector: obj.getSelector()) {
      ICompositeNode parserNode = NodeModelUtils.getNode(selector);
      if (parserNode != null) {
        if (first) first = false;
        else buf.append(", ");
        buf.append(parserNode.getText());
      }
    }
    return stripString(buf.toString());    
  }

  StyledString text(MixinDefinition obj) {
    ICompositeNode parserNode = NodeModelUtils.getNode(obj.getSelector());
    if (parserNode == null) return null;
    return new StyledString(stripString(parserNode.getText()), italicStyler);
	}
	
	StyledString text(ImportStatement obj) {
    StyledString ss = new StyledString("@import ", atKeywordStyler);
    ss.append(obj.getUri(), italicStyler);
    return ss;
  }
	
  StyledString text(MediaStatement obj) {
    StyledString ss = new StyledString("@media", atKeywordStyler);
    EList<MediaQuery> medias = obj.getMedia_queries().getMedia_query();
    boolean first = true;
    for (MediaQuery mediaQuery: medias) {
      if (first) first = false;
      else ss.append(",");
      if (mediaQuery.getKeyword() != null) {
        ss.append(" ");
        ss.append(mediaQuery.getKeyword());
      }
      ss.append(" ");
      ss.append(mediaQuery.getMedia_type());
    }
    return ss;
  }
  
  StyledString text(PageStatement obj) {
    StyledString ss = new StyledString("@page", atKeywordStyler);
    if (obj.getPseudo_page() != null) ss.append(" :" + obj.getPseudo_page());
    return ss;
  }
  
  StyledString text(FontFaceStatement obj) {
    return new StyledString("@font-face", atKeywordStyler);
  }
  
  StyledString text(CharsetStatement obj) {
    return new StyledString("@charset", atKeywordStyler);
  }
  
  String stripString(String s) {
    return s;
  }
/*
	//Labels and icons can be computed like this:
	
	String text(MyModel ele) {
	  return "my "+ele.getName();
	}
	 
    String image(MyModel ele) {
      return "MyModel.gif";
    }
*/
}
