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

package org.eclipse.linuxtools.tmf.analysis.graph.core.building;

/**
 * Base class for event handlers, implementing common behavior like cancellation
 *
 * @author Geneviève Bastien
 */
public abstract class AbstractTraceEventHandler implements ITraceEventHandler {

	boolean fHandlerCancelled = false;

	@Override
	public boolean isCancelled() {
		return fHandlerCancelled;
	}

	@Override
	public void cancel() {
		fHandlerCancelled = true;
	}

}
