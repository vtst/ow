// EasyXtext
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.eclipse.easyxtext.guice;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Type annotation for methods to be called after dependency injection.
 * 
 * <p>
 * This annotation is used for an extension of Google Guice.  It may be applied
 * to any method which has <b>no</b> parameter.  This method will be automatically
 * called by the injector after the class has been completely injected by Guice.
 * </p>
 * <p>
 * This annotation is ignored on methods which have parameters.
 * </p>
 *  
 * @author Vincent Simonet
 */
@Target({ ElementType.METHOD })
@Retention(RUNTIME)
public @interface PostInject {}
