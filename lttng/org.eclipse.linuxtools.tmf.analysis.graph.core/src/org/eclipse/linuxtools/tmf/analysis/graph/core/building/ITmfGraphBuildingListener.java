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

import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;

/**
 * @since 3.0
 */
public interface ITmfGraphBuildingListener {

	/**
	 * Begin the building process
	 */
	public void begin();

	/**
	 * Indicate the progress of the building phase
	 *
	 * @param time The current time
	 */
	public void progress(long time);

	/**
	 * Set the current executing phase
	 *
	 * @param phase phase number
	 */
	public void phase(int phase);

	/**
	 * Indicate the end of the building process
	 */
	public void finished();

	/**
	 * Returns whether the job is cancelled
	 *
	 * @return Where the listener was cancelled
	 */
	public boolean isCanceled();

    /**
     * Sets the time range to analyze
     *
     * @param startTime Start of the range
     * @param endTime End of the range
     */
    public void setTimeRange(ITmfTimestamp startTime, ITmfTimestamp endTime);

}
