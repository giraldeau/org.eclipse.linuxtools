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
import org.eclipse.linuxtools.tmf.core.event.matching.ExpireCleanupMonitor;
import org.eclipse.linuxtools.tmf.core.event.matching.IMatchMonitor;
import org.eclipse.linuxtools.tmf.core.event.matching.NullCleanupStrategy;
import org.eclipse.linuxtools.tmf.core.event.matching.StopEarlyMonitor;
import org.eclipse.linuxtools.tmf.core.event.matching.TmfEventMatching;
import org.eclipse.linuxtools.tmf.core.event.matching.TmfNetworkEventMatching;
import org.eclipse.linuxtools.tmf.core.synchronization.ITmfTimestampTransform;
import org.eclipse.linuxtools.tmf.core.synchronization.SyncAlgorithmFullyIncremental;
import org.eclipse.linuxtools.tmf.core.synchronization.TmfTimestampTransform;
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

    public static class ShiftResetFunction implements Function<TmfExperiment> {
        @Override
        public void apply(TmfExperiment exp) {
            ITmfTrace[] traces = exp.getTraces();
            for (ITmfTrace iTmfTrace : traces) {
                iTmfTrace.setTimestampTransform(TmfTimestampTransform.IDENTITY);
            }
        }
    }

    public static class ShiftOriginFunction implements Function<TmfExperiment> {
        @Override
        public void apply(TmfExperiment exp) {
            new ShiftResetFunction().apply(exp);
            ITmfTrace[] traces = exp.getTraces();
            for (int i = 0; i < traces.length - 1; i++) {
                ITmfTrace t1 = traces[i];
                ITmfTrace t2 = traces[i + 1];

                long v1 = t1.getNext(t1.seekEvent(0L)).getTimestamp().getValue();
                long v2 = t2.getNext(t2.seekEvent(0L)).getTimestamp().getValue();
                double beta = -1.0 * (v2 - v1);
                ITmfTimestampTransform xform = new TmfTimestampTransformLinear(1.0, beta);
                t2.setTimestampTransform(xform);
                long v3 = t2.getNext(t2.seekEvent(0L)).getTimestamp().getValue();
                assertTrue(Math.abs(v1 - v3) < 1000);
            }
            // getStartTime() does not return the transformed timestamp
            // assertEquals(startBegin.getValue(), firstEv.getValue());
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
            new ShiftResetFunction().apply(exp);
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
                ITmfEvent first = t2.getNext(ctx);

                double beta = lastEv.getTimestamp().getValue() - first.getTimestamp().getValue();
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
                // FIXME: getStartTime() and
                // getEndTime() are not updated on setTimestampTransform()
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

    /**
     * Test the effect of trace alignment and cleanup on the maximum number
     * unmatched packets.
     */
    @Test
    public void testShiftTrace() {
        IMatchMonitor[] cleanup = new IMatchMonitor[] { new ExpireCleanupMonitor(), new NullCleanupStrategy() };

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

                for (IMatchMonitor clean : cleanup) {
                    TmfNetworkEventMatching traceMatch = new TmfNetworkEventMatching(Collections.singleton(experiment));
                    traceMatch.addMatchMonitor(clean);
                    traceMatch.matchEvents();
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


    private static void printStat(String msg, TmfNetworkEventMatching matching) {
        System.out.println(String.format("%8s %6d %6d", msg,
                matching.getMatchedCount(), matching.getMaxUnmatchedCount()));
    }

    /**
     * Apply the shift twice should yield the same result
     */
    @Test
    public void testShiftIdepotent() {
        assumeTrue(CtfTmfTestTrace.DJANGO_CLIENT.exists());
        assumeTrue(CtfTmfTestTrace.DJANGO_DB.exists());
        assumeTrue(CtfTmfTestTrace.DJANGO_HTTPD.exists());
        try (CtfTmfTrace trace1 = CtfTmfTestTrace.DJANGO_CLIENT.getTrace();
                CtfTmfTrace trace2 = CtfTmfTestTrace.DJANGO_DB.getTrace();
                CtfTmfTrace trace3 = CtfTmfTestTrace.DJANGO_HTTPD.getTrace();) {
            ITmfTrace[] traces = { trace1, trace2, trace3 };
            TmfExperiment experiment = new TmfExperiment(CtfTmfEvent.class, "Test experiment", traces, 1000);
            for (Entry<String, Function<TmfExperiment>> entry: funcMap.entrySet()) {
                System.out.println("func " + entry.getKey());
                HashMap<ITmfTrace, ITmfTimestamp> result = new HashMap<>();
                Function<TmfExperiment> func = entry.getValue();
                func.apply(experiment);
                for (ITmfTrace trace: traces) {
                    ITmfTimestamp value = trace.getNext(trace.seekEvent(0L)).getTimestamp();
                    result.put(trace, value);
                }
                entry.getValue().apply(experiment);
                for (ITmfTrace trace: traces) {
                    ITmfTimestamp value = trace.getNext(trace.seekEvent(0L)).getTimestamp();
                    assertEquals(result.get(trace), value);
                }
            }
        }
    }

    @Test
    public void testPreSync() {
        assumeTrue(CtfTmfTestTrace.DJANGO_CLIENT.exists());
        assumeTrue(CtfTmfTestTrace.DJANGO_DB.exists());
        assumeTrue(CtfTmfTestTrace.DJANGO_HTTPD.exists());
        try (CtfTmfTrace trace1 = CtfTmfTestTrace.DJANGO_CLIENT.getTrace();
                CtfTmfTrace trace2 = CtfTmfTestTrace.DJANGO_DB.getTrace();
                CtfTmfTrace trace3 = CtfTmfTestTrace.DJANGO_HTTPD.getTrace();) {

            // print header
            HashMap<ITmfTrace, Long> results = new HashMap<>();
            System.out.println(String.format("%-8s %6s %6s", "step", "hit", "miss"));

            ITmfTrace[] traces = { trace1, trace2, trace3 };
            TmfExperiment experiment = new TmfExperiment(CtfTmfEvent.class, "Test experiment", traces, 1000);

            // worst-case sync
            Function<TmfExperiment> func = new ShiftDisjointFunction();
            func.apply(experiment);
            SyncAlgorithmFullyIncremental algo = new SyncAlgorithmFullyIncremental();
            TmfNetworkEventMatching matching = new TmfNetworkEventMatching(Collections.singleton(experiment), algo);
            matching.matchEvents();
            printStat("worst", matching);
            for (ITmfTrace trace : traces) {
                ITmfTimestampTransform xform = algo.getTimestampTransform(trace).composeWith(trace.getTimestampTransform());
                trace.setTimestampTransform(xform);
            }

            for (ITmfTrace trace: traces) {
                results.put(trace, trace.getNext(trace.seekEvent(0L)).getTimestamp().getValue());
            }

            // coarse pre-sync step
            func = new ShiftOriginFunction();
            func.apply(experiment);
            algo = new SyncAlgorithmFullyIncremental();
            matching = new TmfNetworkEventMatching(Collections.singleton(experiment), algo);
            matching.addMatchMonitor(new StopEarlyMonitor());
            matching.matchEvents();
            printStat("presync", matching);

            // compose the new timestamp transform with the current transform
            for (ITmfTrace trace : traces) {
                ITmfTimestampTransform xform = algo.getTimestampTransform(trace).composeWith(trace.getTimestampTransform());
                trace.setTimestampTransform(xform);
            }

            // do the fine grained sync
            algo = new SyncAlgorithmFullyIncremental();
            matching = new TmfNetworkEventMatching(Collections.singleton(experiment), algo);
            matching.addMatchMonitor(new ExpireCleanupMonitor());
            matching.matchEvents();
            printStat("realsync", matching);
            for (ITmfTrace trace : traces) {
                ITmfTimestampTransform xform = algo.getTimestampTransform(trace).composeWith(trace.getTimestampTransform());
                trace.setTimestampTransform(xform);
            }

            // check that the two synchronization methods produces almost the same result
            for (ITmfTrace trace: traces) {
                long diff = trace.getNext(trace.seekEvent(0L)).getTimestamp().getValue() - results.get(trace);
                assertTrue(Math.abs(diff) < 10000); // 10us
            }
        }

        // FIXME: assert that both method yield almost the same result
        // very strange: alpha close to 2!!!
    }


    /**
     * Run the benchmark with 3 bigger traces
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
            runCpuTest(Collections.singleton(experiment), "Django traces", 10);
            runMemoryTest(Collections.singleton(experiment), "Django traces", 10);
            experiment.dispose();
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
            System.gc();
            pm.start();
            traceMatch.matchEvents();
            System.gc();
            pm.stop();
        }
        pm.commit();

    }
}
