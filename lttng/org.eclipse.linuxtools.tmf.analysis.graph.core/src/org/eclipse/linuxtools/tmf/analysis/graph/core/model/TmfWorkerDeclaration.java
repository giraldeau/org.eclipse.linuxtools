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
 * Element declaration specific for workers
 * @since 3.0
 *
 */
public class TmfWorkerDeclaration extends TmfModelElementDeclaration {

    /**
     * Constructor
     *
     * @param name
     *            The name of this declaration
     */
    public TmfWorkerDeclaration(String name) {
        super(name);
    }

    @Override
    public TmfWorker create() {
        return new TmfWorker(0, null, this);
    }

    /**
     * Create a new worker with id and trace
     *
     * @param id The id of the worker
     * @param trace The trace
     * @return The new worker
     */
    public TmfWorker create(long id, ITmfTrace trace) {
        return new TmfWorker(id, trace, this);
    }

}
