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

import org.eclipse.linuxtools.internal.tmf.core.synchronization.SyncAlgorithmFullyIncremental;
import org.eclipse.linuxtools.internal.tmf.core.synchronization.TmfTimestampTransformLinear;
import org.eclipse.linuxtools.tmf.core.event.matching.TmfEventDependency;
import org.eclipse.linuxtools.tmf.core.synchronization.ITmfTimestampTransform;
import org.eclipse.linuxtools.tmf.core.synchronization.SynchronizationAlgorithm;
import org.eclipse.linuxtools.tmf.core.synchronization.SynchronizationAlgorithm.SyncQuality;
import org.eclipse.linuxtools.tmf.core.synchronization.SynchronizationAlgorithmFactory;
import org.eclipse.linuxtools.tmf.core.synchronization.TimestampTransformFactory;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.tests.stubs.event.TmfSyncEventStub;
import org.eclipse.linuxtools.tmf.tests.stubs.trace.TmfTraceStub;
import org.junit.Before;
import org.junit.Ignore;
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
        SynchronizationAlgorithm.setSyncThreshold(100);
    }

    /**
     * Testing fully incremental algorithm with communication between the two
     * traces
     */
    @Test
    public void testFullyIncremental() {
        SynchronizationAlgorithm syncAlgo = SynchronizationAlgorithmFactory.getFullyIncrementalAlgorithm();

        syncAlgo.init(fTraces);
        assertEquals(SyncQuality.ABSENT, syncAlgo.getSynchronizationQuality(t1, t2));
        assertEquals(TimestampTransformFactory.getDefaultTransform(), syncAlgo.getTimestampTransform(t1));
        assertEquals(TimestampTransformFactory.getDefaultTransform(), syncAlgo.getTimestampTransform(t1));

        genMatchEvent(syncAlgo, t1, t2, 10, 20);
        assertEquals(SyncQuality.INCOMPLETE, syncAlgo.getSynchronizationQuality(t1, t2));

        genMatchEvent(syncAlgo, t2, t1, 30, 40);
        assertEquals(SyncQuality.INCOMPLETE, syncAlgo.getSynchronizationQuality(t1, t2));

        genMatchEvent(syncAlgo, t1, t2, 50, 60);
        assertEquals(SyncQuality.APPROXIMATE, syncAlgo.getSynchronizationQuality(t1, t2));

        // generate enough matches to make reach SyncQuality.ACCURATE
        int off = 1000;
        for (int i = 0; i < 100; i++) {
            genMatchEvent(syncAlgo, t1, t2, off + ((i + 0) * 10), off + ((i + 1) * 10));
            genMatchEvent(syncAlgo, t2, t1, off + ((i + 2) * 10), off + ((i + 3) * 10));
            off += 40;
        }
        assertEquals(SyncQuality.ACCURATE, syncAlgo.getSynchronizationQuality(t1, t2));
        /* Make the two hulls intersect */

        genMatchEvent(syncAlgo, t1, t2, off + 80, off + 1000);
        genMatchEvent(syncAlgo, t2, t1, off + 80, off);
        assertEquals(SyncQuality.FAIL, syncAlgo.getSynchronizationQuality(t1, t2));
    }

    /**
     * Testing the fully incremental synchronization algorithm when
     * communication goes in only one direction
     */
    @Ignore // FIXME: To test convex-hull, the class should be exposed
    @Test
    public void testOneHull() {
        SynchronizationAlgorithm syncAlgo = SynchronizationAlgorithmFactory.getFullyIncrementalAlgorithm();
        syncAlgo.init(fTraces);
        assertEquals(SyncQuality.ABSENT, syncAlgo.getSynchronizationQuality(t1, t2));

        int[][] points = new int[][] { {1, 3}, {2, 5}, { 3, 5 }, { 4, 7 } };
        for (int i = 0; i < points.length; i++) {
            genMatchEvent(syncAlgo, t1, t2, points[i][0], points[i][1]);
            assertEquals(SyncQuality.INCOMPLETE, syncAlgo.getSynchronizationQuality(t1, t2));
        }

        TmfTimestampTransformLinear xform = null;
        for (ITmfTrace trace: fTraces) {
            ITmfTimestampTransform obj = syncAlgo.getTimestampTransform(trace);
            if (obj instanceof TmfTimestampTransformLinear) {
                xform = (TmfTimestampTransformLinear) obj;
                break;
            }
        }
        assertNotNull(xform);
        assertEquals(1.0 + 1.0/3, xform.getAlpha().doubleValue(), 0.00001);
        assertEquals(1.0, xform.getBeta().doubleValue(), 0.00001);
    }

    /**
     * Testing the fully incremental synchronization algorithm when all
     * communication from trace1 to trace2 happens before all communication from
     * trace2 to trace1
     */
    @Test
    public void testDisjoint() {
        SynchronizationAlgorithm syncAlgo = SynchronizationAlgorithmFactory.getFullyIncrementalAlgorithm();

        syncAlgo.init(fTraces);
        assertEquals(SyncQuality.ABSENT, syncAlgo.getSynchronizationQuality(t1, t2));

        int[][] forward = new int[][] { { 1, 3 }, { 2, 5 }, { 3, 5 }, { 4, 7 } };
        int[][] reverse = new int[][] { { 7, 6 }, { 8, 6 }, { 10, 8 } };

        for (int i = 0; i < forward.length; i++) {
            genMatchEvent(syncAlgo, t1, t2, forward[i][0], forward[i][1]);
        }
        for (int i = 0; i < reverse.length; i++) {
            genMatchEvent(syncAlgo, t2, t1, reverse[i][0], reverse[i][1]);
        }
        assertEquals(SyncQuality.APPROXIMATE, syncAlgo.getSynchronizationQuality(t1, t2));
        //assertEquals("SyncAlgorithmFullyIncremental [Between t1 and t2 [ alpha 1 beta 2.5 ]]", syncAlgo.toString());
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

        int[][][] links = new int[][][] { links0, links1, links2, links3, links4 };
        for (int[][] link : links) {
            assertIdentity(traces, link);
        }
    }

    private static void assertIdentity(List<ITmfTrace> traces, int[][] links) {
        SyncAlgorithmFullyIncremental algo = (SyncAlgorithmFullyIncremental) SynchronizationAlgorithmFactory.getFullyIncrementalAlgorithm();

        algo.init(traces);
        int clk = 100000;
        for (int[] link : links) {
            int id0 = link[0];
            int id1 = link[1];
            ITmfTrace x = traces.get(id0);
            ITmfTrace y = traces.get(id1);
            for (int i = 0; i < 10; i++) {
                int off = i * 1000;
                genMatchEvent(algo, x, y, (id0 * clk) + off + ((i + 0) * 10), (id1 * clk) + off + ((i + 1) * 10));
                genMatchEvent(algo, y, x, (id1 * clk) + off + ((i + 2) * 10), (id0 * clk) + off + ((i + 3) * 10));
            }
            assertEquals(SyncQuality.ACCURATE, algo.getSynchronizationQuality(x, y));
        }
        // generated events should make the graph fully connected
        assertEquals(1, algo.getNumPartitions());

        int identity = 0;
        for (ITmfTrace trace : traces) {
            ITmfTimestampTransform xform = algo.getTimestampTransform(trace);
            if (xform == TimestampTransformFactory.getDefaultTransform()) {
                identity++;
            }
        }
        // FIXME: remove the output
        System.out.println("dump:");
        Map<String, Map<String, Object>> stats = algo.getStats();
        for (String key : stats.keySet()) {
            System.out.println(stats.get(key));
        }
        for (ITmfTrace trace : traces) {
            ITmfTimestampTransform xform = algo.getTimestampTransform(trace);
            System.out.println("new " + trace.getHostId() + " " + xform);
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


//    @Test
//    public void checkSyncBounds() {
//        ArrayList<ITmfTrace> traces = new ArrayList<>();
//        for (int i = 0; i < 2; i++) {
//            TmfTraceStub trace = new TmfTraceStub();
//            trace.init(String.format("T%d", i));
//            traces.add(trace);
//        }
//        SyncAlgorithmFullyIncremental algo = new SyncAlgorithmFullyIncremental();
//        algo.init(traces);
//        ITmfTrace x1 = traces.get(0);
//        ITmfTrace x2 = traces.get(1);
//
//        genMatchEvent(algo, x1, x2, 0, 10);
//        genMatchEvent(algo, x2, x1, 20, 30);
//        genMatchEvent(algo, x1, x2, 40, 50);
//        genMatchEvent(algo, x2, x1, 60, 70);
//        assertEquals(SyncQuality.ACCURATE, algo.getSynchronizationQuality(x1, x2));
//        //TmfTimestampTransformLinear xform1 = (TmfTimestampTransformLinear) algo.getTimestampTransform(x1);
//        TmfTimestampTransformLinear xform2 = (TmfTimestampTransformLinear) algo.getTimestampTransform(x2);
//        TmfTimestampTransformLinear xform3 = (TmfTimestampTransformLinear) xform2.inverse();
//        //System.out.println("xform1 " + xform1.getAlpha() + " " + xform1.getBeta());
//        System.out.println("xform2 " + xform2.getAlpha() + " " + xform2.getBeta());
//        System.out.println("xform3 " + xform3.getAlpha() + " " + xform3.getBeta());
//        // I want to know the fAlphamax, fAlphamin, fBetamax, fBetamin
//
//        // fAlphamax =  3
//        // fBetamax  = -70
//
//        // fAlphamin = 0.7
//        // fBetamin  = 10
//    }

}
