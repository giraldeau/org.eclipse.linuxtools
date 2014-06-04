/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.analysis.graph.core.staging;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.linuxtools.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.linuxtools.tmf.core.statesystem.TmfStateSystemAnalysisModule;

/**
 * Module for the virtual machine state systems
 *
 * @author Geneviève Bastien
 * @since 3.0
 */
public class ExecGraphModule extends TmfStateSystemAnalysisModule {

    /** ID of this analysis module */
    public static final String ID = "org.eclipse.linuxtools.lttng2.kernel.core.graph.staging.ExecGraphModule"; //$NON-NLS-1$

    /**
     * Constructor
     */
    public ExecGraphModule() {
        // FIXME: provide a view
        //this.registerOutput(new TmfAnalysisViewOutput("org.eclipse.linuxtools.tmf.analysis.vm.vmview")); //$NON-NLS-1$
    }

    @Override
    protected @NonNull
    ITmfStateProvider createStateProvider() {
        return new ExecGraphStateProvider(getTrace());
    }

    @Override
    protected @NonNull StateSystemBackendType getBackendType() {
        return StateSystemBackendType.FULL;
    }

}
