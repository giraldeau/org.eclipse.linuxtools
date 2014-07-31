/*******************************************************************************
 * Copyright (c) 2014 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.lttng2.kernel.core.tests.perf.event.matching;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.linuxtools.lttng2.kernel.core.event.matching.TcpEventMatching;
import org.eclipse.linuxtools.lttng2.kernel.core.event.matching.TcpLttngEventMatching;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.matching.ExpireCleanupStrategy;
import org.eclipse.linuxtools.tmf.core.event.matching.ICleanupStrategy;
import org.eclipse.linuxtools.tmf.core.event.matching.NullCleanupStrategy;
import org.eclipse.linuxtools.tmf.core.event.matching.TmfEventMatching;
import org.eclipse.linuxtools.tmf.core.event.matching.TmfNetworkEventMatching;
import org.eclipse.linuxtools.tmf.core.synchronization.ITmfTimestampTransform;
import org.eclipse.linuxtools.tmf.core.synchronization.TmfTimestampTransformLinear;
import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.trace.ITmfContext;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfTrace;
import org.eclipse.linuxtools.tmf.ctf.core.tests.shared.CtfTmfTestTrace;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

/**
 * Benchmark simple event matching, without trace synchronization
 *
 * @author Geneviève Bastien
 */
public class EventMatchingBenchmark {

    private static final String TEST_ID = "org.eclipse.linuxtools#Event matching#";
    private static final String TIME = " (time)";
    private static final String MEMORY = " (memory usage)";
    private static final String TEST_SUMMARY = "Event matching";

    /**
     * Initialize some data
     */
    @BeforeClass
    public static void setUp() {
        TmfEventMatching.registerMatchObject(new TcpEventMatching());
        TmfEventMatching.registerMatchObject(new TcpLttngEventMatching());
    }

    /**
     * Run the benchmark with 2 small traces
     */
    @Test
    public void testSmallTraces() {
        assumeTrue(CtfTmfTestTrace.SYNC_SRC.exists());
        assumeTrue(CtfTmfTestTrace.SYNC_DEST.exists());
        try (CtfTmfTrace trace1 = CtfTmfTestTrace.SYNC_SRC.getTrace();
                CtfTmfTrace trace2 = CtfTmfTestTrace.SYNC_DEST.getTrace();) {
            Set<ITmfTrace> traces = ImmutableSet.of((ITmfTrace) trace1, trace2);
            runCpuTest(traces, "Match TCP events", 40);
        }
    }

    public static interface Function<T> {
        public void apply(T obj);
    }

    public static class ShiftOriginFunction implements Function<TmfExperiment> {
        @Override
        public void apply(TmfExperiment exp) {
            ITmfTrace[] traces = exp.getTraces();
            for (ITmfTrace trace : traces) {
                ITmfContext ctx = trace.seekEvent(0L);
                ITmfTimestamp startBegin = trace.getStartTime();
                ITmfTimestamp firstEv = trace.getNext(ctx).getTimestamp();
                assertEquals(startBegin.getValue(), firstEv.getValue());

                double beta = -1.0 * firstEv.getValue();
                ITmfTimestampTransform xform = new TmfTimestampTransformLinear(1.0, beta);
                trace.setTimestampTransform(xform);

                ctx = trace.seekEvent(0L);
                firstEv = trace.getNext(ctx).getTimestamp();
                ITmfTimestamp start = trace.getStartTime();
                assertEquals(start.getValue(), firstEv.getValue());
                assertTrue(start.getValue() != startBegin.getValue());
            }
        }
    }

    public static class ShiftNothingFunction implements Function<TmfExperiment> {
        @Override
        public void apply(TmfExperiment exp) {

        }
    }

    public static class ShiftDisjointFunction implements Function<TmfExperiment> {
        @Override
        public void apply(TmfExperiment exp) {
            System.out.println(exp);
            ITmfTrace[] traces = exp.getTraces();
            for (int i = 0; i < traces.length - 1; i++) {
                ITmfTrace t1 = traces[i];
                ITmfTrace t2 = traces[i + 1];

                ITmfContext ctx = t1.seekEvent(0L);
                t1.getNext(ctx);
                ctx = t1.seekEvent(Long.MAX_VALUE);
                ctx = t1.seekEvent(t1.getNbEvents() - 1);
                ITmfEvent lastEv = t1.getNext(ctx);
                assertNotNull(lastEv);

                ctx = t2.seekEvent(0L);
                t2.getNext(ctx);
                ITmfTimestamp start = t2.getStartTime();

                double beta = lastEv.getTimestamp().getValue() - start.getValue();
                ITmfTimestampTransform xform = new TmfTimestampTransformLinear(1.0, beta);
                t2.setTimestampTransform(xform);
                ctx = t2.seekEvent(0L);
                t2.getNext(ctx);
            }

            for (int i = 0; i < traces.length - 1; i++) {
                ITmfTrace t1 = traces[i];
                ITmfTrace t2 = traces[i + 1];

                // force seek to the end
                ITmfContext ctx = t1.seekEvent(0L);
                ctx = t1.seekEvent(Long.MAX_VALUE);
                ctx = t1.seekEvent(t1.getNbEvents() - 1);
                ITmfEvent last = t1.getNext(ctx);

                ctx = t2.seekEvent(0L);
                ITmfEvent first = t2.getNext(ctx);

                long delta = last.getTimestamp().getValue() - first.getTimestamp().getValue();
                // FIXME: the following fails because getStartTime() and
                // getEndTime() are not updated on setTimestampTransform()
                // long delta = last.getTimestamp().getValue() -
                // t2.getStartTime().getValue();
                assertTrue(Math.abs(delta) < 1000);
            }

        }
    }

    private static final Map<String, Function<TmfExperiment>> funcMap = new HashMap<>();
    static {
        funcMap.put("origin", new ShiftOriginFunction());
        funcMap.put("disjoint", new ShiftDisjointFunction());
        funcMap.put("nothing", new ShiftNothingFunction());
    }

    @Test
    public void testShiftOrigin() {
        ICleanupStrategy[] cleanup = new ICleanupStrategy[] { new ExpireCleanupStrategy(), new NullCleanupStrategy() };

        assumeTrue(CtfTmfTestTrace.DJANGO_CLIENT.exists());
        assumeTrue(CtfTmfTestTrace.DJANGO_DB.exists());
        assumeTrue(CtfTmfTestTrace.DJANGO_HTTPD.exists());
        for (Entry<String, Function<TmfExperiment>> func : funcMap.entrySet()) {
            try (CtfTmfTrace trace1 = CtfTmfTestTrace.DJANGO_CLIENT.getTrace();
                    CtfTmfTrace trace2 = CtfTmfTestTrace.DJANGO_DB.getTrace();
                    CtfTmfTrace trace3 = CtfTmfTestTrace.DJANGO_HTTPD.getTrace();) {
                ITmfTrace[] traces = { trace1, trace2, trace3 };
                TmfExperiment experiment = new TmfExperiment(CtfTmfEvent.class, "Test experiment", traces, 1000);
                func.getValue().apply(experiment);

                for (ICleanupStrategy clean: cleanup) {
                    TmfNetworkEventMatching traceMatch = new TmfNetworkEventMatching(Collections.singleton(experiment));
                    traceMatch.setCleanupStrategy(clean);
                    traceMatch.matchEvents();
                    // HeapDump.dumpHeap("django-" + func.getKey() + "-" +clean.getClass().getSimpleName() + ".hprof", true);
                    String str = String.format("%8s %25s matched:%6d unmatched:%6d",
                            func.getKey(),
                            clean.getClass().getSimpleName(),
                            traceMatch.getMatchedCount(),
                            traceMatch.getMaxUnmatchedCount());
                    System.out.println(str);
                }
                experiment.dispose();
            }
        }
    }

    /**
     * Run the benchmark with 3 bigger traces
     *
     * TODO: For now, this test takes a lot of RAM. To run, remove the @Ignore
     * and set at least 1024Mb RAM, or else there is OutOfMemoryError exception
     */
    @Test
    public void testDjangoTraces() {
        assumeTrue(CtfTmfTestTrace.DJANGO_CLIENT.exists());
        assumeTrue(CtfTmfTestTrace.DJANGO_DB.exists());
        assumeTrue(CtfTmfTestTrace.DJANGO_HTTPD.exists());
        try (CtfTmfTrace trace1 = CtfTmfTestTrace.DJANGO_CLIENT.getTrace();
                CtfTmfTrace trace2 = CtfTmfTestTrace.DJANGO_DB.getTrace();
                CtfTmfTrace trace3 = CtfTmfTestTrace.DJANGO_HTTPD.getTrace();) {
            ITmfTrace[] traces = { trace1, trace2, trace3 };
            TmfExperiment experiment = new TmfExperiment(CtfTmfEvent.class, "Test experiment", traces, 1000);
            runCpuTest(Collections.singleton(experiment), "Django traces", 1);
            runMemoryTest(traces, "Django traces", 10);
        }
    }

    private static void runCpuTest(Set<? extends ITmfTrace> testTraces, String testName, int loop_count) {
        Performance perf = Performance.getDefault();
        PerformanceMeter pm = perf.createPerformanceMeter(TEST_ID + testName + TIME);
        perf.tagAsSummary(pm, TEST_SUMMARY + ':' + testName + TIME, Dimension.CPU_TIME);

        for (int i = 0; i < loop_count; i++) {
            TmfNetworkEventMatching traceMatch = new TmfNetworkEventMatching(testTraces);

            pm.start();
            traceMatch.matchEvents();
            pm.stop();
            HeapDump.dumpHeap("event-matching-" + i + ".hprof", true);

        }
        pm.commit();

    }

    /* Benchmark memory used by the algorithm */
    private static void runMemoryTest(Set<? extends ITmfTrace> testTraces, String testName, int loop_count) {
        Performance perf = Performance.getDefault();
        PerformanceMeter pm = perf.createPerformanceMeter(TEST_ID + testName + MEMORY);
        perf.tagAsSummary(pm, TEST_SUMMARY + ':' + testName + MEMORY, Dimension.USED_JAVA_HEAP);

        for (int i = 0; i < loop_count; i++) {
            TmfNetworkEventMatching traceMatch = new TmfNetworkEventMatching(testTraces);
            ICleanupStrategy st = new ExpireCleanupStrategy();
            traceMatch.setCleanupStrategy(st);

            System.gc();
            pm.start();
            System.out.println("Max count for " + testName + ": " + traceMatch.getMaxUnmatchedCount());
            traceMatch.matchEvents();

            System.out.println("Max count for " + testName + ": " + traceMatch.getMaxUnmatchedCount());
            System.gc();
            pm.stop();
        }
        pm.commit();

    }
}
