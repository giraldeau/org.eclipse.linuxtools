/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien and Francis Giraldeau - Initial implementation and API
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.analysis.graph.core.model;

import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Structure to save some model objects for a trace. Like the rest of this
 * package, it is only temporary for the construction of some stuff.
 *
 * TODO Could we think of something else to do this
 *
 * @since 3.0
 */
public class TmfModelRegistry {

	HashMap<Class<?>, Object> fRegistry;

	/**
	 * Constructor
	 */
	public TmfModelRegistry() {
		fRegistry = new HashMap<>();
	}

	/**
	 * Gets a specific model for a given context. If model does not exist,
	 * create it
	 *
	 * @param klass
	 *            The class
	 * @return A model of class klass
	 */
	public <T> T getOrCreateModel(Class<T> klass) {
		if (!fRegistry.containsKey(klass)) {

			Object inst = null;
			try {
				inst = klass.newInstance();
			} catch (Exception e) {
				throw new RuntimeException(
						"Error creating model " + klass.getName()); //$NON-NLS-1$
			}
			fRegistry.put(klass, inst);
		}
		return klass.cast(fRegistry.get(klass));
	}

	/**
	 * Gets a specific model for a given context.
	 *
	 * @param klass
	 *            the class
	 * @return A model of class klass
	 */
	public <T> T getModel(Class<T> klass) {
		return klass.cast(fRegistry.get(klass));
	}

	/**
	 * Gets a specific model for a given context.
	 *
	 * @param klass
	 *            the class
	 * @param orChildClass
	 *            Whether to return the model from a class inheriting the
	 *            requested class if the exact model is not available
	 * @return A model of class klass
	 */
	public <T> T getModel(Class<T> klass, boolean orChildClass) {
		T model = getModel(klass);
		if ((model == null) && orChildClass) {
			for (Entry<Class<?>, Object> entry : fRegistry.entrySet()) {
				if (klass.isAssignableFrom(entry.getKey())) {
					model = klass.cast(entry.getValue());
				}
			}
		}
		return model;
	}

}
