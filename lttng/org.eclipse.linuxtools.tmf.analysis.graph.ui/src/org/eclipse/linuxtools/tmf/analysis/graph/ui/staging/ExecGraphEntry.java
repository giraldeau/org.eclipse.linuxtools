/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation (from ControlFlowEntry)
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.analysis.graph.ui.staging;

import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * An entry in the Control Flow view
 */
public class ExecGraphEntry extends TimeGraphEntry {

    private int fQuark;

    /**
     * Constructor
     *
     * @param taskname
     *            Name of the task
     * @param trace
     *            The trace on which we are working
     * @param startTime
     *            The start time of this process's lifetime
     * @param endTime
     *            The end time of this process
     * @param worker
     *            The worker object of this entry
     */
    public ExecGraphEntry(String taskname, ITmfTrace trace, long startTime, long endTime, int quark) {
        super(taskname, startTime, endTime);
        fQuark = quark;
    }

	/**
	 * Get the worker object associated with the entry
	 *
	 * @return The worker object
	 */
	public int getQuark() {
	    return fQuark;
	}

}
