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

package org.eclipse.linuxtools.tmf.analysis.graph.core.base;

/**
 * Edge of a TmfGraph
 *
 * @since 3.0
 */
public class TmfEdge {

    /**
     * Enumeration of the different types of edges
     *
     * FIXME: this sounds very specific to kernel traces, maybe it shouldn't be here
     */
    public enum EdgeType {

        /**
         *
         */
        EPS,
        /**
         *
         */
        UNKNOWN,
        /**
         *
         */
        DEFAULT,
        /**
         *
         */
        RUNNING,
        /**
         *
         */
        BLOCKED,
        /**
         *
         */
        INTERRUPTED,
        /**
         *
         */
        PREEMPTED,
        /**
         *
         */
        TIMER,
        /**
         * Network communication
         */
        NETWORK,
        /**
         *
         */
        USER_INPUT,
        /**
         *
         */
        BLOCK_DEVICE

    }

    private EdgeType fType;
    private final TmfVertex fVertexFrom;
    private final TmfVertex fVertexTo;

    /**
     * Constructor
     *
     * @param from
     *            The vertex this edge leaves from
     * @param to
     *            The vertex the edge leads to
     */
    public TmfEdge(TmfVertex from, TmfVertex to) {
        this.fVertexFrom = from;
        this.fVertexTo = to;
        this.fType = EdgeType.DEFAULT;
    }

    /*
     * Getters
     */

    /**
     * Get the origin vertex of this edge
     *
     * @return The origin vertex
     */
    public TmfVertex getVertexFrom() {
        return fVertexFrom;
    }

    /**
     * Get the destination vertex of this edge
     *
     * @return The destination vertex
     */
    public TmfVertex getVertexTo() {
        return fVertexTo;
    }

    /**
     * Get the edge type
     *
     * @return  The type of the edge
     */
    public EdgeType getType() {
        return fType;
    }

    /**
     * Sets the edge type
     *
     * @param type  The edge type
     */
    public void setType(final EdgeType type) {
        fType = type;
    }

    /**
     * Returns the duration of the edge
     *
     * @return The duration (in nanoseconds)
     */
    public long getDuration() {
        if (fVertexFrom != null && fVertexTo != null) {
            return fVertexTo.getTs() - fVertexFrom.getTs();
        }
        return 0;
    }

    @SuppressWarnings("nls")
    @Override
    public String toString() {
        return "[" + fVertexFrom + "--" + fType + "->" + fVertexTo + "]";
    }
}
