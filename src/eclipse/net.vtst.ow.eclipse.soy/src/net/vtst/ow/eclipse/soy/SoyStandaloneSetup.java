
package net.vtst.ow.eclipse.soy;

/**
 * Initialization support for running Xtext languages 
 * without equinox extension registry
 */
public class SoyStandaloneSetup extends SoyStandaloneSetupGenerated{

	public static void doSetup() {
		new SoyStandaloneSetup().createInjectorAndDoEMFRegistration();
	}
}

