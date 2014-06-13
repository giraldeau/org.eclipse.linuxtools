/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francis Giraldeau- Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.analysis.graph.core.criticalpath;

import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfEdge;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfVertex;

/**
 * Abstract class for critical path algorithms
 *
 * @author Francis Giraldeau
 * @since 3.0
 */
public abstract class AbstractCriticalPathAlgorithm implements ICriticalPathAlgorithm {

    private final TmfGraph fGraph;

    /**
     * Constructor
     *
     * @param graph
     *            The graph on which to calculate critical path
     */
    public AbstractCriticalPathAlgorithm(TmfGraph graph) {
        this.fGraph = graph;
    }

    /**
     * Get the graph
     *
     * @return the graph
     */
    public TmfGraph getGraph() {
        return fGraph;
    }

    /**
     * Copy link of type TYPE between nodes FROM and TO in the graph PATH. The
     * return value is the tail node for the new path.
     *
     * @param path
     *            The graph on which to add the link
     * @param anchor
     *            The anchor vertex from the path graph
     * @param from
     *            The origin vertex in the main graph
     * @param to
     *            The destination vertex in the main graph
     * @param ts
     *            The timestamp of the edge
     * @param type
     *            The type of the edge to create
     * @return The destination vertex in the path graph
     */
    public TmfVertex copyLink(TmfGraph path, TmfVertex anchor, TmfVertex from, TmfVertex to, long ts, TmfEdge.EdgeType type) {
        Object parentFrom = getGraph().getParentOf(from);
        Object parentTo = getGraph().getParentOf(to);
        TmfVertex tmp = new TmfVertex(ts);
        path.add(parentTo, tmp);
        if (parentFrom == parentTo) {
            anchor.linkHorizontal(tmp).setType(type);
        } else {
            anchor.linkVertical(tmp).setType(type);
        }
        return tmp;
    }

    /**
     * Find the next incoming vertex from another object (in vertical) from a
     * node in a given direction
     *
     * @param node
     *            The starting vertex
     * @param dir
     *            The direction in which to search
     * @return The next incoming vertex
     */
    public static TmfVertex findIncoming(TmfVertex node, int dir) {
        TmfVertex currentVertex = node;
        while (true) {
            if (currentVertex.hasNeighbor(TmfVertex.INV)) {
                return currentVertex;
            }
            /* skip epsilon edges */
            if (!(currentVertex.hasNeighbor(dir) && currentVertex.getEdges()[dir].getType() == TmfEdge.EdgeType.EPS)) {
                break;
            }
            currentVertex = currentVertex.neighbor(dir);
        }
        return null;
    }

    @Override
    public String getID() {
        return getClass().getName();
    }

    @Override
    public String getDisplayName() {
        return getClass().getSimpleName();
    }

}
