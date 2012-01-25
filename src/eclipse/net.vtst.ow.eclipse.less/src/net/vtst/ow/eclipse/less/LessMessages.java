// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less;

import java.util.ResourceBundle;

import net.vtst.eclipse.easyxtext.util.EasyResourceBundle;

import com.google.inject.Singleton;

@Singleton
public class LessMessages extends EasyResourceBundle {
  
  public ResourceBundle getBundle() {
    return ResourceBundle.getBundle("net.vtst.ow.eclipse.less.LessMessages");
  }
}
