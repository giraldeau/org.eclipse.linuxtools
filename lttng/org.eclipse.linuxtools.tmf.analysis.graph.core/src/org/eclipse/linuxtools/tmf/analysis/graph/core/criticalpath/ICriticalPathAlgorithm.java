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
 * Interface for all critical path algorithms
 *
 * @author Francis Giraldeau
 * @since 3.0
 */
public interface ICriticalPathAlgorithm {

    /**
     * Computes the critical path
     *
     * @param start
     *            The starting vertex
     * @param end
     *            The end vertex
     * @return The graph of the critical path
     */
    public TmfGraph compute(TmfVertex start, TmfVertex end);

    /**
     * Unique ID of this analysis
     *
     * @return the ID string
     */
    public String getID();

    /**
     * Human readable display name
     *
     * @return display name
     */
    public String getDisplayName();

}
