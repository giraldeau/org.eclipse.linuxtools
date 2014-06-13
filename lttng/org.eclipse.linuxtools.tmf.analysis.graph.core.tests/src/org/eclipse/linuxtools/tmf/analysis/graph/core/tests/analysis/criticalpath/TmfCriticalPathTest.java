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

package org.eclipse.linuxtools.tmf.analysis.graph.core.tests.analysis.criticalpath;

import static org.junit.Assert.assertTrue;

import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfVertex;
import org.eclipse.linuxtools.tmf.analysis.graph.core.criticalpath.CriticalPathAlgorithmBounded;
import org.eclipse.linuxtools.tmf.analysis.graph.core.criticalpath.ICriticalPathAlgorithm;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.Ops;
import org.eclipse.linuxtools.tmf.analysis.graph.core.stubs.GraphBuilder;
import org.eclipse.linuxtools.tmf.analysis.graph.core.stubs.GraphFactory;
import org.junit.Test;

/**
 * Test functionalities of TmfCriticalPath class
 */
@SuppressWarnings("javadoc")
public class TmfCriticalPathTest {

    static GraphFactory factory = new GraphFactory();

    public boolean testCriticalPathOne(GraphBuilder builder) {
        builder.buildData();
        for (int i = 0; i<builder.getDataSize(); i++) {
            builder.build(i);
            builder.criticalPathBounded(i);
            TmfGraph main = builder.toGraph(i);
            ICriticalPathAlgorithm cp = new CriticalPathAlgorithmBounded(main);
            TmfGraph bounded = cp.compute(main.getHead(), null);
            TmfVertex actBounded = bounded.getHead(0L);
            boolean status = Ops.validate(actBounded);
            status = status & Ops.match(builder.getData(i).bounded, actBounded);
            assertTrue(status);
        }
        return true;
    }

    @Test
    public void testCriticalPathBasic() {
        assertTrue(testCriticalPathOne(GraphFactory.GRAPH_BASIC));
    }

    @Test
    public void testCriticalPathWakeupSelf() {
        assertTrue(testCriticalPathOne(GraphFactory.GRAPH_WAKEUP_SELF));
    }

    @Test
    public void testCriticalPathWakeupNew() {
        assertTrue(testCriticalPathOne(GraphFactory.GRAPH_WAKEUP_NEW));
    }

    @Test
    public void testCriticalPathWakeupUnknown() {
        assertTrue(testCriticalPathOne(GraphFactory.GRAPH_WAKEUP_UNKNOWN));
    }

    @Test
    public void testCriticalPathWakeupMutual() {
        assertTrue(testCriticalPathOne(GraphFactory.GRAPH_WAKEUP_MUTUAL));
    }

    @Test
    public void testCriticalPathWakeupNested() {
        assertTrue(testCriticalPathOne(GraphFactory.GRAPH_NESTED));
    }

    @Test
    public void testCriticalPathWakeupOpened() {
        assertTrue(testCriticalPathOne(GraphFactory.GRAPH_OPENED));
    }

    @Test
    public void testCriticalPathWakeupMissing() {
        assertTrue(testCriticalPathOne(GraphFactory.GRAPH_WAKEUP_MISSING));
    }

    @Test
    public void testCriticalPathWakeupEmbeded() {
        assertTrue(testCriticalPathOne(GraphFactory.GRAPH_WAKEUP_EMBEDED));
    }

    @Test
    public void testCriticalPathWakeupInterleave() {
        assertTrue(testCriticalPathOne(GraphFactory.GRAPH_WAKEUP_INTERLEAVE));
    }

    @Test
    public void testCriticalPathWakeupNet1() {
        assertTrue(testCriticalPathOne(GraphFactory.GRAPH_NET1));
    }

}
