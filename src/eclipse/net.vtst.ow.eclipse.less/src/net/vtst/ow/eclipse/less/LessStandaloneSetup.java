// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less;

/**
 * Initialization support for running Xtext languages 
 * without equinox extension registry
 */
public class LessStandaloneSetup extends LessStandaloneSetupGenerated{

	public static void doSetup() {
		new LessStandaloneSetup().createInjectorAndDoEMFRegistration();
	}
}

