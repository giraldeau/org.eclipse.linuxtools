/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien & Francis Giraldeau- Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.analysis.graph.core.base;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Class that computes statistics on time spent in the elements (objects) of a
 * graph
 *
 * @since 3.0
 */
public class TmfGraphStatistics implements ITmfGraphVisitor {

    private static String STATS_TOTAL = "total"; //$NON-NLS-1$

    private final Multimap<Object, Long> fWorkerStats;
    private TmfGraph fGraph;

    /**
     * Constructor
     */
    public TmfGraphStatistics() {
        fWorkerStats = ArrayListMultimap.create();
    }

    /**
     * Compute the statistics for a graph
     *
     * @param graph
     *            The graph on which to calculate statistics
     * @param current
     *            The element from which to start calculations
     */
    public void getGraphStatistics(TmfGraph graph, Object current) {
        if (graph == null) {
            return;
        }
        fGraph = graph;
        fGraph.scanLineTraverse(fGraph.getHead(current), this);
    }

    @Override
    public void visitHead(TmfVertex node) {

    }

    @Override
    public void visit(TmfVertex node) {

    }

    @Override
    public void visit(TmfEdge edge, boolean horizontal) {
        // Add the duration of the link only if it is horizontal
        if (horizontal) {
            fWorkerStats.put(fGraph.getParentOf(edge.getVertexFrom()),
                    edge.getVertexTo().getTs() - edge.getVertexFrom().getTs());
            fWorkerStats.put(STATS_TOTAL,
                    edge.getVertexTo().getTs() - edge.getVertexFrom().getTs());
        }
    }

    /**
     * Get the total duration spent by one element of the graph
     *
     * @param worker
     *            The object to get the time spent for
     * @return The sum of all durations
     */
    public Long getSum(Object worker) {
        Long sum = 0L;
        for (Long duration : fWorkerStats.get(worker)) {
            sum += duration;
        }
        return sum;
    }

    /**
     * Get the total duration of the graph vertices
     *
     * @return The sum of all durations
     */
    public Long getSum() {
        return getSum(STATS_TOTAL);
    }

    /**
     * Get the percentage of time by one element of the graph
     *
     * @param worker
     *            The object to get the percentage for
     * @return The percentage time spent in this element
     */
    public Double getPercent(Object worker) {
        if (getSum() == 0) {
            return 0D;
        }
        return (double) getSum(worker) / (double) getSum();
    }

}
