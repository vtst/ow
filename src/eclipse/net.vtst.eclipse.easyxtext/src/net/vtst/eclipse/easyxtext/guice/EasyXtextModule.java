// EasyXtext
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.eclipse.easyxtext.guice;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.matcher.Matchers;

/**
 * Runtime module for configuring Google Guice extensions for
 * <em>EasyXtext.</em>
 * <p>
 * This module configures the extensions of Google Guice needed for
 * <em>EasyXtext</em>. It should be installed by the runtime module (the class
 * named {@code YourDslRuntimeModule} which is created by Xtext) of your
 * Xtext-based Eclipse plugin. Typically, include the following method in your
 * module:
 * 
 * <pre>
 * <code>public void configure(Binder binder) {
 *   super.configure(binder);
 *   binder.install(new EasyXtextModule());
 * }
 * </code>
 * </pre>
 * 
 * @author Vincent Simonet
 */
public class EasyXtextModule implements Module {

  @Override
  public void configure(Binder binder) {
    binder.bindListener(Matchers.any(), new PostInjectTypeListener());
  }

}
