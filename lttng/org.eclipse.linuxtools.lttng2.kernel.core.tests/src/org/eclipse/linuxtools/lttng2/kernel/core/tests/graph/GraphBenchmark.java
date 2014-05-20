package org.eclipse.linuxtools.lttng2.kernel.core.tests.graph;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.junit.Test;

public class GraphBenchmark {

    private final static String TRACE_DIR = "traces";
    private final static String DJANGO_BENCHMARK = "django-benchmark";

    @Test
    public void testGraphBenchmark() {

    }

    @Test
    public void testSyncBenchmark() {
        ExperimentLoader.process(Paths.get(TRACE_DIR, DJANGO_BENCHMARK), new ExperimentProcessor() {
            @Override
            public void before(Path tracePath) {
                System.out.println(tracePath);
            }
            @Override
            public void core(TmfExperiment exp) {

            }
            @Override
            public void after(TmfExperiment exp) {

            }
        });
    }

}
