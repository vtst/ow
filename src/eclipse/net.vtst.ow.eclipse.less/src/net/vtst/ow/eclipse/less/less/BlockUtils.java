// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.less;

import java.util.Iterator;

/**
 * This class implements some abstraction to manipulate blocks.
 */
public class BlockUtils {

  public static Iterator<InnerStatement> iterator(Block block) {
    return block.getStatement().iterator();
  }
  
}
