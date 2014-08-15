/*******************************************************************************
 * Copyright (c) 2014 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francis Giraldeau - Initial implementation and API
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.analysis.graph.core.model;

import org.eclipse.jdt.annotation.NonNull;

/**
 * @author Francis Giraldeau
 *
 */
public interface ITmfWorkerFactory {

    /**
     * Create a new model element
     *
     * @param host The host ID of the trace this element belongs to
     * @param cpu The CPU
     * @param wid The ID of the element to create
     * @return A new worker
     */
    public TmfWorker createModelElement(@NonNull String host, int cpu, long wid);

}
