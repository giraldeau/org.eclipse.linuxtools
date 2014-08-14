package org.eclipse.linuxtools.tmf.analysis.graph.core.tests.staging;

import static org.junit.Assert.assertNotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.linuxtools.statesystem.core.ITmfStateSystem;
import org.eclipse.linuxtools.tmf.analysis.graph.core.ctf.CtfTraceFinder;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.ExecGraphModule;
import org.eclipse.linuxtools.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.linuxtools.tmf.core.synchronization.ITmfTimestampTransform;
import org.eclipse.linuxtools.tmf.core.synchronization.TmfTimestampTransform;
import org.eclipse.linuxtools.tmf.core.synchronization.TmfTimestampTransformLinear;
import org.eclipse.linuxtools.tmf.core.synchronization.TmfTimestampTransformLinearFast;
import org.eclipse.linuxtools.tmf.core.tests.shared.TmfTestHelper;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.junit.Test;

public class GraphBenchmark {

    private static final String SAMPLE_LOAD_TIME = "load_ms";
    private static final String SAMPLE_BUILD_TIME = "build_ms";
    private static final String SAMPLE_EXTRACT_TIME = "extract_ms";
    private static final String SAMPLE_LOAD_MEM = "load_mem";
    private static final String SAMPLE_BUILD_MEM = "build_mem";
//    private static final String SAMPLE_EXTRACT_MEM = "extract_mem";
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

    public static class Data {
        public long time;
        public long mem;
        public long count;
    }

    public static interface Func {
        public void func();
    }

    public static class Run {
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
    TmfAbstractAnalysisModule module;
    Pattern p = Pattern.compile("django-benchmark-([0-9]+)");

    @Test
    public void testGraphStateSystemBenchmark() throws TmfAnalysisException {
        final Samples<Integer, Long> s = new Samples<>();
        int repeat = 3;
        Path path = Paths.get(TraceStrings.TRACE_DIR, TraceStrings.DJANGO_BENCHMARK, "django-benchmark-10");
        Integer power = parseTracePath(p, path);
        for (int i = 0; i < repeat; i++) {
            graphStateHistoryBenchHelper(path, power, s);
        }
        s.save(Paths.get("django-benchmark-ss.out"));
    }

    public Data step1(final Path path) {
        /*
         * Step 1: synchronize the trace
         */
        Data data = Run.go(new Func() {
            @Override
            public void func() {
                exp = CtfTraceFinder.makeTmfExperiment(path);
                CtfTraceFinder.synchronizeExperiment(exp);
            }
        });
        return data;
    }

    public Data step2ss() throws TmfAnalysisException {
        /*
         * Step 2: build the execution graph
         */
        TmfTraceOpenedSignal signal = new TmfTraceOpenedSignal(this, exp, null);
        exp.traceOpened(signal);

        module = new ExecGraphModule();
        module.setId(ExecGraphModule.ID);
        module.setTrace(exp);

        Data data = Run.go(new Func() {
            @Override
            public void func() {
                try {
                    TmfTestHelper.executeAnalysis(module);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return data;
    }

    public void graphStateHistoryBenchHelper(final Path path, int power, Samples<Integer, Long> s) throws TmfAnalysisException {
        System.out.println("processing " + path.toFile().getName() + " power=" + power);
        Data data = step1(path);
        s.addSample(SAMPLE_LOAD_TIME, power, data.time);
        s.addSample(SAMPLE_LOAD_MEM, power, data.mem);

        data = step2ss();
        ITmfStateSystem ss = ((ExecGraphModule) module).getStateSystem();
        if (ss == null) {
            return;
        }
        s.addSample(SAMPLE_BUILD_TIME, power, data.time);
        s.addSample(SAMPLE_BUILD_MEM, power, data.mem);
        s.addSample(SAMPLE_GRAPH_SIZE, power, (long) ss.getNbAttributes());
        Path ht = Paths.get(String.format("/tmp/null/%s.ht", ExecGraphModule.ID));
        ht.toFile().delete();
    }

    private static Integer parseTracePath(Pattern pattern, Path tracePath) {
        String name = tracePath.toFile().getName();
        Matcher m = pattern.matcher(name);
        m.find();
        return Integer.parseInt(m.group(1));
    }

    @Test
    public void testBenchmarkTransform() {
        exp = CtfTraceFinder.makeTmfExperiment(Paths.get("traces/django-index"));
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
