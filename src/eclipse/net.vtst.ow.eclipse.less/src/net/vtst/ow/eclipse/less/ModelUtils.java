package net.vtst.ow.eclipse.less;

import org.eclipse.emf.ecore.EObject;

public class ModelUtils {

	/**
	 * Searches the object's parent hierarchy for an object of given type.
	 * 
	 * @param <C> the type of the EObject which is searched
	 * @param from the starting point
	 * @param type the type of the EObject which is searched
	 * @return the parent or grand parent or... of the given object which has the specified type
	 */
	@SuppressWarnings("unchecked")
	public static <C extends EObject> C goUpTo(EObject from, java.lang.Class<C> type) {
		EObject parent = from;
		while (parent != null) {
			if (type.isInstance(parent)) {
				return (C) parent;
			}
			parent = parent.eContainer();
		}
		return null;
	}
}
