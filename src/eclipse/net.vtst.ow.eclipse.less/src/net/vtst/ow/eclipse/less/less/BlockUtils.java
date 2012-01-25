// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.less;

import java.util.NoSuchElementException;

import org.eclipse.emf.ecore.EObject;

/**
 * This class implements some abstraction to manipulate blocks.
 */
public class BlockUtils {
  
  public static class Iterator implements Iterable<EObject>, java.util.Iterator<EObject> {
    private BlockContents contents;
    public Iterator(Block block) {
      this.contents = block.getContents();
    }
    
    public java.util.Iterator<EObject> iterator() {
      return this;
    }

    public boolean hasNext() {
      return this.contents != null;
    }

    public EObject next() {
      if (this.contents == null) throw new NoSuchElementException();
      EObject next = this.contents.getItem();
      this.contents = this.contents.getNext();
      return next;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

  }

  public static Iterator iterator(Block block) {
    return new Iterator(block);
  }
  
  public static Block getBlock(BlockContents contents) {
    EObject obj = contents;
    while (!(obj == null || obj instanceof Block)) obj = obj.eContainer();
    return (Block) obj;
  }
  
}
