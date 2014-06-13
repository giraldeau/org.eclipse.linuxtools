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

import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

/**
 * Defines a model entity doing something in the trace. For example, a worker
 * can be a process, a function, etc
 *
 * Workers can have many custom fields and can be searched with model filters
 * like other model elements.
 *
 * @since 3.0
 */
public class TmfWorker extends TmfAbstractModelElement {

    private final long fId;
    private String fName;
    private long fStart;
    private long fEnd;

    // TODO: See if possible to change trace to host, workers can be part of
    // multiple traces of the same host
    private final ITmfTrace fTrace;

    /**
     * Constructor
     *
     * @param id
     *            The id of this worker
     * @param trace
     *            The trace it belongs to
     * @param declaration
     *            The model declaration for this worker
     */
    public TmfWorker(long id, ITmfTrace trace, TmfModelElementDeclaration declaration) {
        super(declaration);
        fId = id;
        fTrace = trace;
    }

    /**
     * Gets the id of the worker
     *
     * @return The worker id
     */
    public long getId() {
        return fId;
    }

    /**
     * Get the trace
     *
     * @return The trace
     */
    public ITmfTrace getTrace() {
        return fTrace;
    }

    /**
     * Get the start time of this worker
     *
     * @return The start time
     */
    public long getStart() {
        return fStart;
    }

    /**
     * Sets the start time of this worker
     *
     * TODO Change start and end for timestamp
     *
     * @param start
     *            The start time
     */
    public void setStart(long start) {
        this.fStart = start;
    }

    /**
     * Gets the end time of this worker
     *
     * @return The end time
     */
    public long getEnd() {
        return fEnd;
    }

    /**
     * Sets the end time of this worker
     *
     * @param end
     *            The end time
     */
    public void setEnd(long end) {
        this.fEnd = end;
    }

    /**
     * Gets the name of this worker
     *
     * @return The worker's name
     */
    public String getName() {
        return fName;
    }

    /**
     * Sets the name of this worker
     *
     * @param name
     *            The name of the worker
     */
    public void setName(String name) {
        this.fName = name;
    }

    @Override
    public String toString() {
        return String.format("[%d,%s]", fId, fName); //$NON-NLS-1$
    }

    @Override
    public int compareTo(TmfAbstractModelElement other) {
        int result = 0;

        // FIXME: take into account the trace
        if (other instanceof TmfWorker) {
            TmfWorker worker = (TmfWorker) other;
            if (fId < worker.getId()) {
                result = -1;
            } else if (fId > worker.getId()) {
                result = 1;
            }
        } else {
            result = super.compareTo(other);
        }
        return result;
    }

}
