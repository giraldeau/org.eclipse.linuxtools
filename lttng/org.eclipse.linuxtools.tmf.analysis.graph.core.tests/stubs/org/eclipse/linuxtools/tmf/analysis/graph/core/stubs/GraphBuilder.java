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

package org.eclipse.linuxtools.tmf.analysis.graph.core.stubs;

import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.Ops;


/**
 * Base class for graph building graph test data
 */
public abstract class GraphBuilder {
    private final String fName;

    final GraphBuilderData[] fData;

    /**
     * Constructor
     *
     * @param name
     *            Name of the graph builder
     * @param dataSize
     *            Size of the data
     */
    public GraphBuilder(String name, int dataSize) {
        this.fName = name;
        fData = new GraphBuilderData[dataSize];
    }

    /**
     * Get the graph builder name
     *
     * @return The graph builder name
     */
    public String getName() {
        return fName;
    }

    /**
     * Build a graph with the test data
     *
     * @param index
     *            The index of the test data to use
     */
    public abstract void build(int index);

    /**
     * Computes the critical path with bounded algorithm
     *
     * @param index
     *            The index of the test data to use
     */
    public abstract void criticalPathBounded(int index);

    /**
     * Computes the critical path with unbounded algorithm
     *
     * @param index
     *            The index of the test data to use
     */
    public abstract void criticalPathUnbounded(int index);

    /**
     * Get the test data for this graph builder
     */
    public abstract void buildData();

    /**
     * Gets the size of the test data
     *
     * @return Size of data
     */
    public int getDataSize() {
        return fData.length;
    }

    /**
     * Transforms the set of vertices and edges into a graph
     *
     * @param index
     *            The index of the test data to use
     * @return The resulting graph
     */
    public TmfGraph toGraph(int index) {
        return Ops.toGraphInPlace(fData[index].head);
    }

    /**
     * Get the builder data at index
     *
     * @param index
     *            The index of the test data to use
     * @return The builder data
     */
    public GraphBuilderData getData(int index) {
        return fData[index];
    }
}
