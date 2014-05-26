/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial implementation
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.core.tests.synchronization;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.linuxtools.tmf.core.event.matching.TmfEventDependency;
import org.eclipse.linuxtools.tmf.core.synchronization.ITmfTimestampTransform;
import org.eclipse.linuxtools.tmf.core.synchronization.SyncAlgorithmFullyIncremental;
import org.eclipse.linuxtools.tmf.core.synchronization.SynchronizationAlgorithm;
import org.eclipse.linuxtools.tmf.core.synchronization.SynchronizationAlgorithm.SyncQuality;
import org.eclipse.linuxtools.tmf.core.synchronization.TmfTimestampTransform;
import org.eclipse.linuxtools.tmf.core.synchronization.TmfTimestampTransformLinear;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.tests.stubs.event.TmfSyncEventStub;
import org.eclipse.linuxtools.tmf.tests.stubs.trace.TmfTraceStub;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link SynchronizationAlgorithm} and its descendants
 *
 * @author Geneviève Bastien
 */
@SuppressWarnings("nls")
public class SyncTest {

    private TmfTraceStub t1, t2;
    private Collection<ITmfTrace> fTraces;

    /**
     * Initializing the traces
     */
    @Before
    public void init() {
        t1 = new TmfTraceStub();
        t1.init("t1");
        t2 = new TmfTraceStub();
        t2.init("t2");

        Collection<ITmfTrace> traces = new LinkedList<>();
        traces.add(t1);
        traces.add(t2);
        fTraces = traces;
    }

    /**
     * Testing fully incremental algorithm with communication between the two
     * traces
     */
    @Test
    public void testFullyIncremental() {

        SynchronizationAlgorithm syncAlgo = new SyncAlgorithmFullyIncremental();

        syncAlgo.init(fTraces);

        assertEquals(SyncQuality.ABSENT, syncAlgo.getSynchronizationQuality(t1, t2));
        syncAlgo.addMatch(
                new TmfEventDependency(new TmfSyncEventStub(t2, new TmfTimestamp(1)),
                        new TmfSyncEventStub(t1, new TmfTimestamp(1))
                ));
        assertEquals("SyncAlgorithmFullyIncremental [Between t1 and t2 [ alpha 1 beta 0 ]]", syncAlgo.toString());
        assertEquals(SyncQuality.INCOMPLETE, syncAlgo.getSynchronizationQuality(t1, t2));

        syncAlgo.addMatch(
                new TmfEventDependency(new TmfSyncEventStub(t1, new TmfTimestamp(1)),
                        new TmfSyncEventStub(t2, new TmfTimestamp(3))
                ));
        assertEquals("SyncAlgorithmFullyIncremental [Between t1 and t2 [ alpha 1 beta 0 ]]", syncAlgo.toString());
        assertEquals(SyncQuality.INCOMPLETE, syncAlgo.getSynchronizationQuality(t1, t2));

        syncAlgo.addMatch(
                new TmfEventDependency(new TmfSyncEventStub(t2, new TmfTimestamp(2)),
                        new TmfSyncEventStub(t1, new TmfTimestamp(3))
                ));
        assertEquals("SyncAlgorithmFullyIncremental [Between t1 and t2 [ alpha 1 beta 0.5 ]]", syncAlgo.toString());
        assertEquals(SyncQuality.APPROXIMATE, syncAlgo.getSynchronizationQuality(t1, t2));

        syncAlgo.addMatch(
                new TmfEventDependency(new TmfSyncEventStub(t1, new TmfTimestamp(3)),
                        new TmfSyncEventStub(t2, new TmfTimestamp(5))
                ));
        assertEquals("SyncAlgorithmFullyIncremental [Between t1 and t2 [ alpha 0.75 beta 1.25 ]]", syncAlgo.toString());
        assertEquals(SyncQuality.ACCURATE, syncAlgo.getSynchronizationQuality(t1, t2));

        syncAlgo.addMatch(
                new TmfEventDependency(new TmfSyncEventStub(t1, new TmfTimestamp(4)),
                        new TmfSyncEventStub(t2, new TmfTimestamp(8))
                ));
        assertEquals("SyncAlgorithmFullyIncremental [Between t1 and t2 [ alpha 0.75 beta 1.25 ]]", syncAlgo.toString());
        assertEquals(SyncQuality.ACCURATE, syncAlgo.getSynchronizationQuality(t1, t2));

        syncAlgo.addMatch(
                new TmfEventDependency(new TmfSyncEventStub(t2, new TmfTimestamp(4)),
                        new TmfSyncEventStub(t1, new TmfTimestamp(5))
                ));
        assertEquals("SyncAlgorithmFullyIncremental [Between t1 and t2 [ alpha 1.125 beta 0.875 ]]", syncAlgo.toString());
        assertEquals(SyncQuality.ACCURATE, syncAlgo.getSynchronizationQuality(t1, t2));

        syncAlgo.addMatch(
                new TmfEventDependency(new TmfSyncEventStub(t2, new TmfTimestamp(4)),
                        new TmfSyncEventStub(t1, new TmfTimestamp(6))
                ));
        assertEquals("SyncAlgorithmFullyIncremental [Between t1 and t2 [ alpha 1.125 beta 0.875 ]]", syncAlgo.toString());
        assertEquals(SyncQuality.ACCURATE, syncAlgo.getSynchronizationQuality(t1, t2));

        syncAlgo.addMatch(
                new TmfEventDependency(new TmfSyncEventStub(t1, new TmfTimestamp(6)),
                        new TmfSyncEventStub(t2, new TmfTimestamp(7))
                ));
        assertEquals("SyncAlgorithmFullyIncremental [Between t1 and t2 [ alpha 0.725 beta 1.275 ]]", syncAlgo.toString());
        assertEquals(SyncQuality.ACCURATE, syncAlgo.getSynchronizationQuality(t1, t2));

        ITmfTimestampTransform tt2 = syncAlgo.getTimestampTransform(t2);
        ITmfTimestampTransform tt1 = syncAlgo.getTimestampTransform(t1);

        assertEquals(syncAlgo.getTimestampTransform(t1.getHostId()), tt1);
        assertEquals(TmfTimestampTransform.IDENTITY, tt1);
        assertEquals(syncAlgo.getTimestampTransform(t2.getHostId()), tt2);

        /* Make the two hulls intersect */
        syncAlgo.addMatch(
                new TmfEventDependency(new TmfSyncEventStub(t1, new TmfTimestamp(7)),
                        new TmfSyncEventStub(t2, new TmfTimestamp(4))
                ));
        syncAlgo.addMatch(
                new TmfEventDependency(new TmfSyncEventStub(t2, new TmfTimestamp(7)),
                        new TmfSyncEventStub(t1, new TmfTimestamp(3))
                ));
        assertEquals(SyncQuality.FAIL, syncAlgo.getSynchronizationQuality(t1, t2));
    }

    /**
     * Testing the fully incremental synchronization algorithm when
     * communication goes in only one direction
     */
    @Test
    public void testOneHull() {

        SynchronizationAlgorithm syncAlgo = new SyncAlgorithmFullyIncremental();

        syncAlgo.init(fTraces);

        assertEquals(SyncQuality.ABSENT, syncAlgo.getSynchronizationQuality(t1, t2));

        syncAlgo.addMatch(
                new TmfEventDependency(new TmfSyncEventStub(t1, new TmfTimestamp(1)),
                        new TmfSyncEventStub(t2, new TmfTimestamp(3)))
                );
        assertEquals(SyncQuality.INCOMPLETE, syncAlgo.getSynchronizationQuality(t1, t2));

        syncAlgo.addMatch(
                new TmfEventDependency(new TmfSyncEventStub(t1, new TmfTimestamp(2)),
                        new TmfSyncEventStub(t2, new TmfTimestamp(5)))
                );

        assertEquals(SyncQuality.INCOMPLETE, syncAlgo.getSynchronizationQuality(t1, t2));

        syncAlgo.addMatch(
                new TmfEventDependency(new TmfSyncEventStub(t1, new TmfTimestamp(3)),
                        new TmfSyncEventStub(t2, new TmfTimestamp(5)))
                );
        assertEquals(SyncQuality.INCOMPLETE, syncAlgo.getSynchronizationQuality(t1, t2));

        syncAlgo.addMatch(
                new TmfEventDependency(new TmfSyncEventStub(t1, new TmfTimestamp(4)),
                        new TmfSyncEventStub(t2, new TmfTimestamp(7)))
                );
        assertEquals(SyncQuality.INCOMPLETE, syncAlgo.getSynchronizationQuality(t1, t2));
        assertEquals("SyncAlgorithmFullyIncremental [Between t1 and t2 [ alpha 1 beta 0 ]]", syncAlgo.toString());

    }

    /**
     * Testing the fully incremental synchronization algorithm when all
     * communication from trace1 to trace2 happens before all communication from
     * trace2 to trace1
     */
    @Test
    public void testDisjoint() {

        SynchronizationAlgorithm syncAlgo = new SyncAlgorithmFullyIncremental();

        syncAlgo.init(fTraces);

        assertEquals(SyncQuality.ABSENT, syncAlgo.getSynchronizationQuality(t1, t2));

        syncAlgo.addMatch(
                new TmfEventDependency(new TmfSyncEventStub(t1, new TmfTimestamp(1)),
                        new TmfSyncEventStub(t2, new TmfTimestamp(3)))
                );
        assertEquals(SyncQuality.INCOMPLETE, syncAlgo.getSynchronizationQuality(t1, t2));

        syncAlgo.addMatch(
                new TmfEventDependency(new TmfSyncEventStub(t1, new TmfTimestamp(2)),
                        new TmfSyncEventStub(t2, new TmfTimestamp(5)))
                );

        assertEquals(SyncQuality.INCOMPLETE, syncAlgo.getSynchronizationQuality(t1, t2));

        syncAlgo.addMatch(
                new TmfEventDependency(new TmfSyncEventStub(t1, new TmfTimestamp(3)),
                        new TmfSyncEventStub(t2, new TmfTimestamp(5)))
                );
        assertEquals(SyncQuality.INCOMPLETE, syncAlgo.getSynchronizationQuality(t1, t2));

        syncAlgo.addMatch(
                new TmfEventDependency(new TmfSyncEventStub(t1, new TmfTimestamp(4)),
                        new TmfSyncEventStub(t2, new TmfTimestamp(7)))
                );
        assertEquals(SyncQuality.INCOMPLETE, syncAlgo.getSynchronizationQuality(t1, t2));
        assertEquals("SyncAlgorithmFullyIncremental [Between t1 and t2 [ alpha 1 beta 0 ]]", syncAlgo.toString());

        syncAlgo.addMatch(
                new TmfEventDependency(new TmfSyncEventStub(t2, new TmfTimestamp(7)),
                        new TmfSyncEventStub(t1, new TmfTimestamp(6)))
                );
        assertEquals(SyncQuality.APPROXIMATE, syncAlgo.getSynchronizationQuality(t1, t2));

        syncAlgo.addMatch(
                new TmfEventDependency(new TmfSyncEventStub(t2, new TmfTimestamp(8)),
                        new TmfSyncEventStub(t1, new TmfTimestamp(6)))
                );
        assertEquals(SyncQuality.APPROXIMATE, syncAlgo.getSynchronizationQuality(t1, t2));

        syncAlgo.addMatch(
                new TmfEventDependency(new TmfSyncEventStub(t2, new TmfTimestamp(10)),
                        new TmfSyncEventStub(t1, new TmfTimestamp(8)))
                );
        assertEquals(SyncQuality.APPROXIMATE, syncAlgo.getSynchronizationQuality(t1, t2));
        assertEquals("SyncAlgorithmFullyIncremental [Between t1 and t2 [ alpha 1 beta 2.5 ]]", syncAlgo.toString());
    }

    @Test
    public void testComposeWithInverse() {
        TmfTimestampTransformLinear xform = new TmfTimestampTransformLinear(10.0, 1000.0);
        ITmfTimestampTransform xformInv = xform.inverse();
        ITmfTimestampTransform iden = xform.composeWith(xformInv);
        /*
         * The same timestamp should be returned for the identity but account
         * for any error margin
         */
        long err = 1;
        long exp = 1000000;
        long act = iden.transform(exp);
        assertTrue(Math.abs(exp - act) < err);
    }

    /**
     * Test synchronization combinations
     */
    @Test
    public void testChainedTransforms() {
        ArrayList<ITmfTrace> traces = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            TmfTraceStub trace = new TmfTraceStub();
            trace.init(String.format("T%d", i));
            traces.add(trace);
        }
        int[][] links0 = new int[][] { { 0, 1 }, { 1, 2 }, { 0, 2 } }; // full
        int[][] links1 = new int[][] { { 1, 0 }, { 2, 1 }, { 2, 0 } }; // inverted
        int[][] links2 = new int[][] { { 0, 1 }, { 0, 2 } }; // edges from T0
        int[][] links3 = new int[][] { { 1, 0 }, { 2, 0 } }; // edges toward T0
        int[][] links4 = new int[][] { { 0, 2 }, { 2, 1 } }; // transitivity
        int[][] links5 = new int[][] { { 0, 2 }, { 2, 1 } }; // transitivity

        int[][][] links = new int[][][] { links0, links1, links2, links3, links4, links5 };
        for (int[][] link : links) {
            assertIdentity(traces, link);
        }
    }

    private static void assertIdentity(List<ITmfTrace> traces, int[][] links) {
        SyncAlgorithmFullyIncremental algo = new SyncAlgorithmFullyIncremental();
        algo.init(traces);
        int clk = 100000;
        for (int[] link : links) {
            int id0 = link[0];
            int id1 = link[1];
            ITmfTrace x = traces.get(id0);
            ITmfTrace y = traces.get(id1);
            for (int i = 0; i < 3; i++) {
                int off = i * 1000;
                genMatchEvent(algo, x, y, (id0 * clk) + off + ((i + 0) * 10), (id1 * clk) + off + ((i + 1) * 10));
                genMatchEvent(algo, y, x, (id1 * clk) + off + ((i + 2) * 10), (id0 * clk) + off + ((i + 3) * 10));
            }
            assertEquals(SyncQuality.ACCURATE, algo.getSynchronizationQuality(x, y));
        }

        int identity = 0;
        for (ITmfTrace trace : traces) {
            ITmfTimestampTransform xform = algo.getTimestampTransform(trace);
            if (xform == TmfTimestampTransform.IDENTITY) {
                identity++;
            }
        }
        System.out.println("dump:");
        Map<String, Map<String, Object>> stats = algo.getStats();
        for (String key : stats.keySet()) {
            System.out.println(stats.get(key));
        }
        for (ITmfTrace trace : traces) {
            ITmfTimestampTransform xform = algo.getTimestampTransform(trace);
            System.out.println(trace.getHostId() + " " + xform);
        }
        try {
            assertEquals(1, identity);
            System.out.println("PASS:" + Arrays.deepToString(links));
        } catch (AssertionError e) {
            System.out.println("FAIL:" + Arrays.deepToString(links));
            throw e;
        }
    }

    private static void genMatchEvent(SynchronizationAlgorithm algo, ITmfTrace sender, ITmfTrace receiver, long send, long recv) {
        algo.addMatch(
                new TmfEventDependency(
                        new TmfSyncEventStub(sender, new TmfTimestamp(send)),
                        new TmfSyncEventStub(receiver, new TmfTimestamp(recv))
                )
                );
    }

}
