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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;

import org.eclipse.linuxtools.lttng2.kernel.core.event.matching.TcpEventMatching;
import org.eclipse.linuxtools.lttng2.kernel.core.event.matching.TcpLttngEventMatching;
import org.eclipse.linuxtools.tmf.core.event.matching.TmfEventMatching;
import org.eclipse.linuxtools.tmf.core.synchronization.ITmfTimestampTransform;
import org.eclipse.linuxtools.tmf.core.synchronization.SynchronizationAlgorithm;
import org.eclipse.linuxtools.tmf.core.synchronization.SynchronizationManager;
import org.eclipse.linuxtools.tmf.core.synchronization.TmfTimestampTransformLinear;
import org.eclipse.linuxtools.tmf.core.synchronization.TmfTimestampTransformLinearFast;
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

/**
 * Benchmark trace synchronization
 *
 * @author Geneviève Bastien
 */
public class TraceSynchronizationBenchmark {

    private static final String TEST_ID = "org.eclipse.linuxtools#Trace synchronization#";
    private static final String TIME = " (time)";
    private static final String MEMORY = " (memory usage)";
    private static final String TEST_SUMMARY = "Trace synchronization";
    private static int BLOCK_SIZE = 1000;

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
            ITmfTrace[] traces = { trace1, trace2 };
            TmfExperiment experiment = new TmfExperiment(CtfTmfEvent.class, "Test experiment", traces, BLOCK_SIZE);
            runCpuTest(experiment, "Match TCP events", 40);
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
            TmfExperiment experiment = new TmfExperiment(CtfTmfEvent.class, "Test experiment", traces, BLOCK_SIZE);
            runCpuTest(experiment, "Django traces", 10);
            runMemoryTest(experiment, "Django traces", 10);
        }
    }

    private static void runCpuTest(TmfExperiment experiment, String testName, int loop_count) {
        Performance perf = Performance.getDefault();
        PerformanceMeter pm = perf.createPerformanceMeter(TEST_ID + testName + TIME);
        perf.tagAsSummary(pm, TEST_SUMMARY + ':' + testName + TIME, Dimension.CPU_TIME);

        for (int i = 0; i < loop_count; i++) {
            pm.start();
            SynchronizationManager.synchronizeTraces(null, Arrays.asList(experiment.getTraces()), true);
            pm.stop();
        }
        pm.commit();

    }

    /* Benchmark memory used by the algorithm */
    private static void runMemoryTest(TmfExperiment experiment, String testName, int loop_count) {
        Performance perf = Performance.getDefault();
        PerformanceMeter pm = perf.createPerformanceMeter(TEST_ID + testName + MEMORY);
        perf.tagAsSummary(pm, TEST_SUMMARY + ':' + testName + MEMORY, Dimension.USED_JAVA_HEAP);

        for (int i = 0; i < loop_count; i++) {

            System.gc();
            pm.start();
            SynchronizationAlgorithm algo = SynchronizationManager.synchronizeTraces(null, Arrays.asList(experiment.getTraces()), true);
            assertNotNull(algo);

            System.gc();
            pm.stop();
        }
        pm.commit();
    }

    @Test
    public void testTimestampTransformPerformance() {
        long iter = 1 << 25;
        TmfTimestampTransformLinear slow = new TmfTimestampTransformLinear(Math.PI, 1234);
        TmfTimestampTransformLinearFast fast = new TmfTimestampTransformLinearFast(slow);

        doTimestampTransformRun("xform-slow", slow, iter);
        doTimestampTransformRun("xform-fast", fast, iter);
    }

    private static void doTimestampTransformRun(String testName, ITmfTimestampTransform xform, long iter) {
        Performance perf = Performance.getDefault();
        PerformanceMeter pm = perf.createPerformanceMeter(TEST_ID + testName + TIME);
        perf.tagAsSummary(pm, TEST_SUMMARY + ':' + testName + TIME, Dimension.CPU_TIME);

        long start = (long) Math.pow(10, 18);
        for (int x = 0; x < 10; x++) {
            pm.start();
            for (long i = 0; i < iter; i++) {
                xform.transform(start + i * 200);
            }
            pm.stop();
        }
        pm.commit();
    }
}
