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

package org.eclipse.linuxtools.tmf.analysis.graph.core.stubs.module;

import org.eclipse.linuxtools.tmf.analysis.graph.core.building.ITmfGraphProvider;
import org.eclipse.linuxtools.tmf.analysis.graph.core.building.TmfGraphBuilderModule;

/**
 * Graph builder module stub
 *
 * @author Geneviève Bastien
 */
public class GraphBuilderModuleStub extends TmfGraphBuilderModule {

    ITmfGraphProvider fProvider;
    /** The analysis id */
    public static final String ANALYSIS_ID = "org.eclipse.linuxtools.tmf.analysis.graph.tests.stub";

    @Override
    protected ITmfGraphProvider getGraphProvider() {
        if (fProvider == null) {
            fProvider = new GraphProviderStub(null, getTrace(), "Graph provider stub");
        }
        return fProvider;
    }

}
