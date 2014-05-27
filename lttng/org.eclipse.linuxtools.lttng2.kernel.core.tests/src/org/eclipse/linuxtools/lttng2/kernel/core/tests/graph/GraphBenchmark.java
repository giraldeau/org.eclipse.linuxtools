package org.eclipse.linuxtools.lttng2.kernel.core.tests.graph;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.linuxtools.lttng2.kernel.core.graph.building.LttngKernelExecutionGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfVertex;
import org.eclipse.linuxtools.tmf.analysis.graph.core.criticalpath.CriticalPathAlgorithmBounded;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfWorker;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.linuxtools.tmf.core.request.ITmfEventRequest;
import org.eclipse.linuxtools.tmf.core.request.TmfEventRequest;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.linuxtools.tmf.core.synchronization.ITmfTimestampTransform;
import org.eclipse.linuxtools.tmf.core.synchronization.TmfTimestampTransform;
import org.eclipse.linuxtools.tmf.core.synchronization.TmfTimestampTransformLinear;
import org.eclipse.linuxtools.tmf.core.tests.shared.TmfTestHelper;
import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfTrace;
import org.junit.Test;

public class GraphBenchmark {

    private static final String TRACE_DIR = "traces";
    private static final String DJANGO_BENCHMARK = "django-benchmark";

    private static final String SAMPLE_LOAD_TIME = "load_ms";
    private static final String SAMPLE_BUILD_TIME = "build_ms";
    private static final String SAMPLE_EXTRACT_TIME = "extract_ms";
    private static final String SAMPLE_LOAD_MEM = "load_mem";
    private static final String SAMPLE_BUILD_MEM = "build_mem";
    private static final String SAMPLE_EXTRACT_MEM = "extract_mem";
    private static final String SAMPLE_GRAPH_SIZE = "graph_size";
    private static final String SAMPLE_PATH_SIZE = "path_size";

    @Test
    public void testSamples() {
        Samples<Integer, Long> s = new Samples<>();
        String[] cols = new String[] { SAMPLE_LOAD_TIME, SAMPLE_BUILD_TIME, SAMPLE_EXTRACT_TIME, SAMPLE_GRAPH_SIZE, SAMPLE_PATH_SIZE };
        for (String col: cols) {
            for (int i = 0; i < 10; i++) {
                s.addSample(col, i, 10L * i);
            }
        }
        s.save(Paths.get("test.out"));
    }

    private static class Data {
        public long time;
        public long mem;
    }

    private static interface Func {
        public void func();
    }

    private static class Run {
        public static Data go(Func f) {
            Data d = new Data();
            System.gc();
            Runtime runtime = Runtime.getRuntime();
            long memStart = runtime.totalMemory() - runtime.freeMemory();
            long timeStart = System.currentTimeMillis();
            f.func();
            d.time = System.currentTimeMillis() - timeStart;
            System.gc();
            d.mem = (runtime.totalMemory() - runtime.freeMemory()) - memStart;
            return d;
        }
    }

    TmfExperiment exp;
    TmfGraph graph;
    TmfGraph criticalPath;
    Pattern p = Pattern.compile("django-benchmark-([0-9]+)");

    @Test
    public void testGraphBenchmark() throws TmfAnalysisException {
        final Samples<Integer, Long> s = new Samples<>();
        int repeat = 3;
        List<Path> input = CtfTraceFinder.getTracePathsByCreationTime(Paths.get(TRACE_DIR, DJANGO_BENCHMARK));
        for (final Path path: input) {
            Integer power = parseTracePath(p, path);
            for (int i = 0; i < repeat; i++) {
                System.out.println("processing " + path.toFile().getName() + " power=" + power + " repeat=" + i);
                /*
                 * Step 1: synchronize the trace
                 */
                Data data = Run.go(new Func() {
                    @Override
                    public void func() {
                        exp = CtfTraceFinder.makeTmfExperiment(path, CtfTmfTrace.class, CtfTmfEvent.class);
                        CtfTraceFinder.synchronizeExperiment(exp);
                    }
                });
                s.addSample(SAMPLE_LOAD_TIME, power, data.time);
                s.addSample(SAMPLE_LOAD_MEM, power, data.mem);

                /*
                 * Step 2: build the execution graph
                 */
                TmfTraceOpenedSignal signal = new TmfTraceOpenedSignal(this, exp, null);
                exp.traceOpened(signal);

                final LttngKernelExecutionGraph module = new LttngKernelExecutionGraph();
                module.setId(LttngKernelExecutionGraph.ANALYSIS_ID);
                module.setTrace(exp);

                data = Run.go(new Func() {
                    @Override
                    public void func() {
                        try {
                            TmfTestHelper.executeAnalysis(module);
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

                graph = module.getGraph();
                s.addSample(SAMPLE_BUILD_TIME, power, data.time);
                s.addSample(SAMPLE_BUILD_MEM, power, data.mem);
                s.addSample(SAMPLE_GRAPH_SIZE, power, (long) graph.size());
                /*
                 * Step 3: compute the critical path
                 */
                data = Run.go(new Func() {
                    @Override
                    public void func() {
                        TmfWorker client = TestGraphMultiHost.findWorkerByName(graph, TestGraphMultiHost.DJANGO_CLIENT_NAME);
                        TmfVertex head = graph.getHead(client);
                        TmfVertex tail = graph.getTail(client);
                        CriticalPathAlgorithmBounded algo = new CriticalPathAlgorithmBounded(graph);
                        criticalPath = algo.compute(head, tail);
                    }
                });
                s.addSample(SAMPLE_EXTRACT_TIME, power, data.time);
                s.addSample(SAMPLE_EXTRACT_MEM, power, data.mem);
                s.addSample(SAMPLE_PATH_SIZE, power, (long) criticalPath.size());
            }
        }
        s.save(Paths.get("django-benchmark.out"));
        assertTrue(true);
    }

    private static Integer parseTracePath(Pattern pattern, Path tracePath) {
        String name = tracePath.toFile().getName();
        Matcher m = pattern.matcher(name);
        m.find();
        return Integer.parseInt(m.group(1));
    }

    private static class TmfTimestampTransformLinearFast extends TmfTimestampTransformLinear {
        private static final long serialVersionUID = 2398540405078949738L;
        private static int PWR = 10;
        private long m = 0;
        private long b = 0;
        public TmfTimestampTransformLinearFast(TmfTimestampTransformLinear xform) {
            super();
            m = BigDecimal.valueOf(1 << PWR).multiply(xform.getAlpha(), xform.getMathContext()).longValue();
            b = xform.getBeta().longValue();
        }

        private long apply(long ts) {
            return ((m * ts) >> PWR) + b;
        }

        @Override
        public ITmfTimestamp transform(ITmfTimestamp timestamp) {
            return new TmfTimestamp(timestamp, apply(timestamp.getValue()));
        }

        @Override
        public long transform(long timestamp) {
            return apply(timestamp);
        }
    }

    int count;
    @Test
    public void testBenchmarkReadTrace() {
        String name = "traces/django-benchmark-7";
        TmfExperiment nosync = CtfTraceFinder.makeTmfExperiment(Paths.get(name), CtfTmfTrace.class, CtfTmfEvent.class);
        TmfTraceOpenedSignal signal = new TmfTraceOpenedSignal(this, nosync, null);
        nosync.traceOpened(signal);

        TmfExperiment sync = CtfTraceFinder.makeTmfExperiment(Paths.get(name), CtfTmfTrace.class, CtfTmfEvent.class);
        signal = new TmfTraceOpenedSignal(this, sync, null);
        sync.traceOpened(signal);
        CtfTraceFinder.synchronizeExperiment(sync);

        int repeat = 10;
        Samples<Integer, Long> samples = new Samples<>();
        for (int i = 0; i < repeat; i++) {
            Data d1 = readBenchHelper(nosync);
            Data d2 = readBenchHelper(sync);
            samples.addSample("nosync", i, d1.time);
            samples.addSample("sync", i, d2.time);
        }
        samples.save(Paths.get("sync-benchmark.data"));
    }

    private Data readBenchHelper(final TmfExperiment experiment) {
        Data data = Run.go(new Func() {
            @Override
            public void func() {
                count = 0;
                TmfEventRequest rq =  new TmfEventRequest(ITmfEvent.class,
                        TmfTimeRange.ETERNITY, 0L, ITmfEventRequest.ALL_DATA,
                        ITmfEventRequest.ExecutionType.BACKGROUND) {
                    @Override
                    public void handleData(final ITmfEvent event) {
                        if (event != null) {
                            count++;
                        }
                    }
                    @Override
                    public void handleCompleted() {
                        System.out.println("done " + count);
                    }
                };
                experiment.sendRequest(rq);
                try {
                    rq.waitForCompletion();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    assertTrue(false);
                }
            }
        });
        return data;
    }

    @Test
    public void testBenchmarkTransform() {
        exp = CtfTraceFinder.makeTmfExperiment(Paths.get("traces/django-index"), CtfTmfTrace.class, CtfTmfEvent.class);
        CtfTraceFinder.synchronizeExperiment(exp);

        ITmfTimestampTransform orig = null;
        ITmfTimestampTransform fast = null;
        for (ITmfTrace trace: exp.getTraces()) {
            ITmfTimestampTransform xform = trace.getTimestampTransform();
            if (xform != TmfTimestampTransform.IDENTITY) {
                orig = xform;
                break;
            }
        }
        assertNotNull(orig);
        fast = new TmfTimestampTransformLinearFast((TmfTimestampTransformLinear)orig);
        int iter = (1 << 25);
        Data dataOrig = xformBenchHelper(orig, iter);
        Data dataFast = xformBenchHelper(fast, iter);
        System.out.println("Results:");
        System.out.println("orig: " + dataOrig.time + " ms");
        System.out.println("fast: " + dataFast.time + " ms");
        System.out.println("ratio: " + ((double) dataOrig.time) / dataFast.time);
    }

    private static Data xformBenchHelper(final ITmfTimestampTransform xform, final int iter) {
        Data data = Run.go(new Func() {
            @Override
            public void func() {
                for (int i = 0; i < iter; i++) {
                    xform.transform(i);
                }
            }
        });
        return data;
    }

}
