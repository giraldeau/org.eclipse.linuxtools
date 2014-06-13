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
 * Timed vertex for TmfGraph
 *
 * @since 3.0
 */
public class TmfVertex implements Comparable<TmfVertex> {

    private static long count = 0;

    /**
     * Constant for the outgoing vertical edge (to other object)
     */
    public static final int OUTV = 0;
    /**
     * Constant for the incoming vertical edge (from other object)
     */
    public static final int INV = 1;
    /**
     * Constant for the outgoing horizontal edge (to same object)
     */
    public static final int OUTH = 2;
    /**
     * Constant for the incoming horizontal edge (from same object)
     */
    public static final int INH = 3;

    private TmfEdge[] fEdges;
    private final long fTimestamp;
    private final long fId;

    /**
     * Default Constructor
     */
    public TmfVertex() {
        this(0);
    }

    /**
     * Constructor with timestamp
     *
     * @param ts
     *            The vertex's timestamp
     */
    public TmfVertex(final long ts) {
        fEdges = (new TmfEdge[4]);
        this.fTimestamp = ts;
        this.fId = count++;
    }

    /**
     * Copy constructor. Keeps same timestamp, but does not keep edges
     *
     * @param node
     *            vertex to copy
     */
    public TmfVertex(TmfVertex node) {
        this(node.fTimestamp);
    }

    /**
     * Copy constructor, but changes the timestamp
     *
     * @param node
     *            vertex to copy
     * @param ts
     *            The timestamp of this new node
     */
    public TmfVertex(TmfVertex node, final long ts) {
        fEdges = (new TmfEdge[4]);
        this.fTimestamp = ts;
        this.fId = count++;
        fEdges[OUTV] = node.fEdges[OUTV];
        fEdges[INV] = node.fEdges[INV];
        fEdges[OUTH] = node.fEdges[OUTH];
        fEdges[INH] = node.fEdges[INH];
    }

    /*
     * Getters and setters
     */

    /**
     * Returns the timestamps of this node
     *
     * @return the timstamp
     */
    public long getTs() {
        return fTimestamp;
    }

    /**
     * Returns the unique ID of this node
     *
     * @return the vertex's id
     */
    public long getID() {
        return fId;
    }

    /**
     * Returns the links from this vertex
     *
     * @return the edges
     */
    public TmfEdge[] getEdges() {
        return fEdges;
    }

    /**
     * Set OUTH and INH pointers
     *
     * @param to
     *            The vertex to link to, belongs to the same object
     *
     * @return The new edge
     */
    public TmfEdge linkHorizontal(TmfVertex to) {
        checkTimestamps(to);
        return linkHorizontalRaw(to);
    }

    private TmfEdge linkHorizontalRaw(TmfVertex node) {
        TmfEdge link = new TmfEdge(this, node);
        fEdges[OUTH] = link;
        node.fEdges[INH] = link;
        return link;
    }

    /**
     * Set OUTV and INV pointers
     *
     * @param to
     *            The vertex to link to, belongs to a different object
     * @return The new edge
     */
    public TmfEdge linkVertical(TmfVertex to) {
        checkTimestamps(to);
        return linkVerticalRaw(to);
    }

    private TmfEdge linkVerticalRaw(TmfVertex to) {
        TmfEdge link = new TmfEdge(this, to);
        fEdges[OUTV] = link;
        to.fEdges[INV] = link;
        return link;
    }

    private void checkTimestamps(TmfVertex to) {
        if (this.fTimestamp > to.fTimestamp) {
            throw new IllegalArgumentException(Messages.TmfVertex_ArgumentTimestampLower +
                    String.format("(curr=%d,next=%d,elapsed=%d)", fTimestamp, to.fTimestamp, to.fTimestamp - fTimestamp)); //$NON-NLS-1$
        }
    }

    /**
     * Returns the neighbor vertex at a given direction
     *
     * @param dir
     *            The neighbor direction
     * @return The neighbor vertex
     */
    public TmfVertex neighbor(int dir) {
        switch (dir) {
        case OUTV:
            return outv();
        case INV:
            return inv();
        case OUTH:
            return outh();
        case INH:
            return inh();
        default:
            break;
        }
        return null;
    }

    /**
     * Returns whether the vertex has a neighbor at a given direction
     *
     * @param dir
     *            The neighbor direction
     * @return Whether this vertex has neighbor at direction
     */
    public boolean hasNeighbor(int dir) {
        return neighbor(dir) != null;
    }

    /**
     * Returns the number of non null neighbor of the current vertex
     *
     * @return The number of neighbor
     */
    public int numberOfNeighbor() {
        int i = 0;
        for (int dir = 0; dir < fEdges.length; dir++) {
            if (fEdges[dir] != null) {
                i++;
            }
        }
        return i;
    }

    /**
     * Return the neighbor outgoing vertical vertex
     *
     * @return The neighbor vertex
     */
    public TmfVertex outv() {
        if (fEdges[OUTV] != null) {
            return fEdges[OUTV].getVertexTo();
        }
        return null;
    }

    /**
     * Return the neighbor incoming vertical vertex
     *
     * @return The neighbor vertex
     */
    public TmfVertex inv() {
        if (fEdges[INV] != null) {
            return fEdges[INV].getVertexFrom();
        }
        return null;
    }

    /**
     * Return the neighbor outgoing horizontal vertex
     *
     * @return The neighbor vertex
     */
    public TmfVertex outh() {
        if (fEdges[OUTH] != null) {
            return fEdges[OUTH].getVertexTo();
        }
        return null;
    }

    /**
     * Return the neighbor incoming horizontal vertex
     *
     * @return The neighbor vertex
     */
    public TmfVertex inh() {
        if (fEdges[INH] != null) {
            return fEdges[INH].getVertexFrom();
        }
        return null;
    }

    @Override
    public int compareTo(TmfVertex other) {
        return this.fTimestamp > other.fTimestamp ? 1 : (this.fTimestamp == other.fTimestamp ? 0 : -1);
    }

    @SuppressWarnings("nls")
    @Override
    public String toString() {
        return "[" + fId + "," + fTimestamp + "]";
    }

}
