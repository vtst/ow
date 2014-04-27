// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.ui.outline;

import net.vtst.ow.eclipse.less.less.Block;
import net.vtst.ow.eclipse.less.less.Declaration;
import net.vtst.ow.eclipse.less.less.FontFaceStatement;
import net.vtst.ow.eclipse.less.less.InnerRuleSet;
import net.vtst.ow.eclipse.less.less.InnerStatement;
import net.vtst.ow.eclipse.less.less.MediaStatement;
import net.vtst.ow.eclipse.less.less.PageStatement;
import net.vtst.ow.eclipse.less.less.StyleSheet;
import net.vtst.ow.eclipse.less.less.TerminatedMixin;
import net.vtst.ow.eclipse.less.less.ToplevelRuleSet;
import net.vtst.ow.eclipse.less.less.ToplevelStatement;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.ui.editor.outline.IOutlineNode;
import org.eclipse.xtext.ui.editor.outline.impl.DefaultOutlineTreeProvider;
import org.eclipse.xtext.ui.editor.outline.impl.DocumentRootNode;

/**
 * customization of the default outline structure
 * 
 */
public class LessOutlineTreeProvider extends DefaultOutlineTreeProvider {

  // Create the root node
  protected void _createChildren(DocumentRootNode parentNode, StyleSheet obj) {
    createNode(parentNode, obj);
  }
  
  // The root node (StyleSheet)
  protected void _createChildren(IOutlineNode parentNode, StyleSheet obj) {
    for (ToplevelStatement statement: obj.getStatements()) {
      createNode(parentNode, statement);
    }
  }
  protected boolean _isLeaf(StyleSheet obj) {
    return obj.getStatements().isEmpty();
  }
  
  // Elements containing blocks
  protected void _createChildren(IOutlineNode parentNode, TerminatedMixin obj) {
    createChildrenForBlock(parentNode, obj.getBody());
  }
  protected boolean _isLeaf(TerminatedMixin obj) {
    return isLeafForBlock(obj.getBody());
  }
  protected void _createChildren(IOutlineNode parentNode, ToplevelRuleSet obj) {
    createChildrenForBlock(parentNode, obj.getBlock());
  }
  protected boolean _isLeaf(ToplevelRuleSet obj) {
    return isLeafForBlock(obj.getBlock());
  }
  protected void _createChildren(IOutlineNode parentNode, MediaStatement obj) {
    createChildrenForBlock(parentNode, obj.getBlock());
  }
  protected boolean _isLeaf(MediaStatement obj) {
    return isLeafForBlock(obj.getBlock());
  }
  protected void _createChildren(IOutlineNode parentNode, PageStatement obj) {
    createChildrenForBlock(parentNode, obj.getBlock());
  }
  protected boolean _isLeaf(PageStatement obj) {
    return isLeafForBlock(obj.getBlock());
  }
  protected void _createChildren(IOutlineNode parentNode, FontFaceStatement obj) {
    createChildrenForBlock(parentNode, obj.getBlock());
  }
  protected boolean _isLeaf(FontFaceStatement obj) {
    return isLeafForBlock(obj.getBlock());
  }
  protected void _createChildren(IOutlineNode parentNode, InnerRuleSet obj) {
    createChildrenForBlock(parentNode, obj.getBlock());
  }
  protected boolean _isLeaf(InnerRuleSet obj) {
    return isLeafForBlock(obj.getBlock());
  }

  // Common methods for all elements containing a block
  protected void createChildrenForBlock(IOutlineNode parentNode, Block block) {
    if (block == null) return;
    for (InnerStatement statement: block.getStatement()) {
      if (innerStatementHasNode(statement)) {
        createNode(parentNode, statement);
      }      
    }
  }
  protected boolean isLeafForBlock(Block block) {
    if (block == null) return true;
    for (InnerStatement statement: block.getStatement()) {
      if (innerStatementHasNode(statement)) {
        return false;
      }      
    }
    return true;
  }
  protected boolean innerStatementHasNode(EObject statement) {
    if (statement instanceof Declaration) return false;
    if (statement instanceof TerminatedMixin) return (((TerminatedMixin) statement).getBody() != null);
    return true;
  }
  
  // By default, elements are leafs.
  protected void _createChildren(IOutlineNode parentNode, EObject modelElement) {}
  protected boolean _isLeaf(EObject modelElement) {
    return true;
  }

}
