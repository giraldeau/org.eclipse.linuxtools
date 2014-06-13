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

import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfVertex;

/**
 * This algorithm traverse the input graph forward and backward with BFS and
 * returns the intersection of both traversal. The result is connected edges
 * from a given main task.
 *
 * @author Francis Giraldeau
 * @since 3.0
 */
public class ConnectedPathAlgorithm extends AbstractCriticalPathAlgorithm {

    /**
     * Constructor
     *
     * @param main
     *            The graph on which to calculate the critical path
     */
    public ConnectedPathAlgorithm(TmfGraph main) {
        super(main);
    }

    @Override
    public TmfGraph compute(TmfVertex start, TmfVertex end) {
        TmfGraph path = new TmfGraph();
        if (start == null) {
            return path;
        }
        // TODO implements the algorithm

        return path;
    }

}
