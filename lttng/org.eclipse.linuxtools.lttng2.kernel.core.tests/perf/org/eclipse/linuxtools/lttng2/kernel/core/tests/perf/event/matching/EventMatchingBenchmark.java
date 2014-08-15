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
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.linuxtools.lttng2.kernel.core.event.matching.TcpEventMatching;
import org.eclipse.linuxtools.lttng2.kernel.core.event.matching.TcpLttngEventMatching;
import org.eclipse.linuxtools.tmf.core.event.matching.ExpireCleanupMonitor;
import org.eclipse.linuxtools.tmf.core.event.matching.IMatchMonitor;
import org.eclipse.linuxtools.tmf.core.event.matching.NullCleanupStrategy;
import org.eclipse.linuxtools.tmf.core.event.matching.StopEarlyMonitor;
import org.eclipse.linuxtools.tmf.core.event.matching.TmfEventMatching;
import org.eclipse.linuxtools.tmf.core.event.matching.TmfNetworkEventMatching;
import org.eclipse.linuxtools.tmf.core.synchronization.IFunction;
import org.eclipse.linuxtools.tmf.core.synchronization.ITmfTimestampTransform;
import org.eclipse.linuxtools.tmf.core.synchronization.SyncAlgorithmFullyIncremental;
import org.eclipse.linuxtools.tmf.core.synchronization.SynchronizationAlgorithm;
import org.eclipse.linuxtools.tmf.core.synchronization.TraceShifterDisjoint;
import org.eclipse.linuxtools.tmf.core.synchronization.TraceShifterNone;
import org.eclipse.linuxtools.tmf.core.synchronization.TraceShifterOrigin;
import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
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

    private static final Map<String, IFunction<TmfExperiment>> funcMap = new HashMap<>();
    static {
        funcMap.put("origin", new TraceShifterOrigin());
        funcMap.put("disjoint", new TraceShifterDisjoint());
        funcMap.put("nothing", new TraceShifterNone());
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
        for (Entry<String, IFunction<TmfExperiment>> func : funcMap.entrySet()) {
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
            for (Entry<String, IFunction<TmfExperiment>> entry: funcMap.entrySet()) {
                System.out.println("func " + entry.getKey());
                HashMap<ITmfTrace, ITmfTimestamp> result = new HashMap<>();
                IFunction<TmfExperiment> func = entry.getValue();
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
            HashMap<ITmfTrace, Long> begin = new HashMap<>();
            HashMap<ITmfTrace, Long> end = new HashMap<>();
            System.out.println(String.format("%-8s %6s %6s", "step", "hit", "miss"));

            ITmfTrace[] traces = { trace1, trace2, trace3 };
            TmfExperiment experiment = new TmfExperiment(CtfTmfEvent.class, "Test experiment", traces, 1000);

//            IFunction<TmfExperiment> reset = new TraceShifterReset();
            IFunction<TmfExperiment> origin = new TraceShifterOrigin();
            IFunction<TmfExperiment> distjoint = new TraceShifterDisjoint();

            origin.apply(experiment);
            SynchronizationAlgorithm algo = new SyncAlgorithmFullyIncremental();
            TmfNetworkEventMatching matching = new TmfNetworkEventMatching(Collections.singleton(experiment), algo);
            matching.matchEvents();
            printStat("legacy", matching);
//            for (ITmfTrace trace : traces) {
//                ITmfTimestampTransform xform = algo.getTimestampTransform(trace).composeWith(trace.getTimestampTransform());
//                System.out.println(xform);
//            }

            // worst-case sync
            distjoint.apply(experiment);
            algo = new SyncAlgorithmFullyIncremental();
            matching = new TmfNetworkEventMatching(Collections.singleton(experiment), algo);
            matching.matchEvents();
            printStat("worst", matching);
            for (ITmfTrace trace : traces) {
                ITmfTimestampTransform xform = algo.getTimestampTransform(trace).composeWith(trace.getTimestampTransform());
                trace.setTimestampTransform(xform);
            }

            for (ITmfTrace trace: traces) {
                begin.put(trace, trace.getNext(trace.seekEvent(0L)).getTimestamp().getValue());
                end.put(trace, trace.getNext(trace.seekEvent(trace.getNbEvents() - 1)).getTimestamp().getValue());
//                System.out.println(trace.getTimestampTransform());
            }

            // coarse pre-sync step
            origin.apply(experiment);
            algo = new SyncAlgorithmFullyIncremental();
            matching = new TmfNetworkEventMatching(Collections.singleton(experiment), algo);
            matching.addMatchMonitor(new StopEarlyMonitor());
            matching.matchEvents();
            printStat("presync", matching);

            // compose the new timestamp transform with the current transform
            for (ITmfTrace trace : traces) {
                ITmfTimestampTransform xform = algo.getTimestampTransform(trace).composeWith(trace.getTimestampTransform());
                trace.setTimestampTransform(xform);
//                System.out.println(xform);
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
//                System.out.println(trace.getTimestampTransform());
            }

            // check that the two synchronization methods produces almost the same result
            for (ITmfTrace trace: traces) {
                long diffBegin = trace.getNext(trace.seekEvent(0L)).getTimestamp().getValue() - begin.get(trace);
                long diffEnd = trace.getNext(trace.seekEvent(trace.getNbEvents() - 1)).getTimestamp().getValue() - end.get(trace);
                System.out.println("diffbegin: "+ diffBegin + " diffend: " + diffEnd);
                assertTrue(Math.abs(diffBegin) < 10);
                assertTrue(Math.abs(diffEnd) < 10);
            }

        }
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
