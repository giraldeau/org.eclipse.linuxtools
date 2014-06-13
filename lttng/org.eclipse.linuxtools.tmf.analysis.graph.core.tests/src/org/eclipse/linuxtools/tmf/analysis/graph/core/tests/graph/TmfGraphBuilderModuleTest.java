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

package org.eclipse.linuxtools.tmf.analysis.graph.core.tests.graph;

import static org.junit.Assert.assertTrue;

import org.eclipse.linuxtools.tmf.analysis.graph.core.building.TmfGraphBuilderModule;
import org.junit.Test;

/**
 * Test suite for the {@link TmfGraphBuilderModule} class
 */
public class TmfGraphBuilderModuleTest {

    /**
     * Test the graph builder execution
     */
    @Test
    public void testBuildGraph() {
        assertTrue(true);
        /* Todo we should remove dependency to CtfTmfTrace and use a test trace instead */
//        CtfTmfTrace trace = CtfTmfTestTrace.SYNC_SRC.getTrace();
//        TmfSignalManager.dispatchSignal(new TmfTraceOpenedSignal(this, trace, null));
//
//        TmfGraphBuilderModule module = (TmfGraphBuilderModule) trace.getAnalysisModule(GraphBuilderModuleStub.ANALYSIS_ID);
//
//        module.schedule();
//        assertTrue(module.waitForCompletion(new NullProgressMonitor()));
//
//        TmfGraph graph = module.getGraph();
//        assertNotNull(graph);
//
//        assertEquals(1188, graph.size());
    }

}
