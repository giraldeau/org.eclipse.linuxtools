package org.eclipse.linuxtools.lttng2.kernel.core.tests.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.linuxtools.internal.lttng2.kernel.core.LttngStrings;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.TcpEventStrings;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building.LttngKernelExecGraphProvider;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building.LttngKernelExecutionGraph;
import org.eclipse.linuxtools.lttng2.kernel.core.trace.LttngKernelTrace;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfVertex;
import org.eclipse.linuxtools.tmf.analysis.graph.core.building.AbstractTmfGraphProvider;
import org.eclipse.linuxtools.tmf.analysis.graph.core.building.AbstractTraceEventHandler;
import org.eclipse.linuxtools.tmf.analysis.graph.core.building.AnalysisPhase;
import org.eclipse.linuxtools.tmf.analysis.graph.core.building.ITmfGraphProvider;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelRegistry;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfSystemModelWithCpu;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfWorker;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.request.ITmfEventRequest;
import org.eclipse.linuxtools.tmf.core.request.ITmfEventRequest.ExecutionType;
import org.eclipse.linuxtools.tmf.core.request.TmfEventRequest;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.linuxtools.tmf.core.synchronization.ITmfTimestampTransform;
import org.eclipse.linuxtools.tmf.core.synchronization.SyncAlgorithmFullyIncremental;
import org.eclipse.linuxtools.tmf.core.synchronization.SynchronizationAlgorithm;
import org.eclipse.linuxtools.tmf.core.tests.shared.TmfTestHelper;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.eclipse.linuxtools.tmf.core.trace.TmfTrace;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfTrace;
import org.junit.Test;

import com.google.common.collect.ArrayListMultimap;

/**
 * Class to test multiple host support for graph
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
public class TestGraphMultiHost {

    /**
     * rcp-hog experiment on two hosts
     * http://secretaire.dorsal.polymtl.ca/~fgiraldeau/traces/phd-hog.tar.gz
     */
    private static String EXP_PHD_HOG = "phd-hog";

    /**
     * django-index experiment on three hosts
     * http://secretaire.dorsal.polymtl.ca
     * /~fgiraldeau/traces/django-index.tar.gz
     */
    private static String EXP_DJANGO_INDEX = "django-index";

    private static String TRACE_DIR = "traces";

    private static class CtfTraceFinder extends SimpleFileVisitor<Path> {
        private final PathMatcher matcher;
        private List<Path> results;

        CtfTraceFinder() {
            matcher = FileSystems.getDefault().getPathMatcher("glob:metadata");
            results = new LinkedList<>();
        }

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
            Path name = path.getFileName();
            if (name != null && matcher.matches(name)) {
                results.add(path.getParent());
            }
            return FileVisitResult.CONTINUE;
        }

        /**
         * Return results
         *
         * @return results
         */
        public List<Path> getResults() {
            return results;
        }
    }

    private static List<Path> findCtfTrace(Path base) {
        CtfTraceFinder finder = new CtfTraceFinder();
        try {
            Files.walkFileTree(base, finder);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        return finder.getResults();

    }

    /**
     * Make an experiment of CTF traces
     *
     * @param base
     *            base directory of traces
     * @param traceKlass
     *            the trace type
     * @param evKlass
     *            the event type
     * @return experiment
     */
    public static TmfExperiment makeTmfExperiment(String base, Class<? extends ITmfTrace> traceKlass, Class<? extends ITmfEvent> evKlass) {
        List<Path> paths = findCtfTrace(Paths.get(TRACE_DIR, base));
        final ITmfTrace[] traces = new TmfTrace[paths.size()];
        int i = 0;
        for (Path p : paths) {
            ITmfTrace tmp = null;
            try {
                tmp = traceKlass.newInstance();
                tmp.initTrace(null, p.toString(), evKlass);
            } catch (InstantiationException | IllegalAccessException | TmfTraceException e) {
                fail(e.getMessage());
            }
            traces[i++] = tmp;
        }

        TmfExperiment experiment = new TmfExperiment();
        try (CtfTmfTrace bidon = new CtfTmfTrace()) {
            bidon.getName();
            experiment.initExperiment(evKlass, "", traces, Integer.MAX_VALUE, null);
            SynchronizationAlgorithm algo = new SyncAlgorithmFullyIncremental();
            try {
                algo = experiment.synchronizeTraces(true, algo);
            } catch (TmfTraceException e) {
                e.printStackTrace();
            }
            for (ITmfTrace trace : experiment.getTraces()) {
                ITmfTimestampTransform tstrans = algo.getTimestampTransform(trace.getHostId());
                System.out.println(tstrans);
                trace.setTimestampTransform(tstrans);
            }
        }
        return experiment;
    }

    /**
     * Make experiment from a trace directory with default trace and event type
     *
     * @param base
     *            trace directory
     * @return the experiment
     */
    public static TmfExperiment makeTmfExperiment(String base) {
        return makeTmfExperiment(base, LttngKernelTrace.class, ITmfEvent.class);
    }

    /**
     * Test that traces are correctly found
     */
    @Test
    public void testFindCtfTraces() {
        List<Path> findCtfTrace = findCtfTrace(Paths.get(TRACE_DIR, EXP_PHD_HOG));
        assertEquals(2, findCtfTrace.size());
    }

    /**
     * Test experiment instantiation
     */
    @Test
    public void testMakeExperiment() {
        TmfExperiment experiment = makeTmfExperiment(EXP_PHD_HOG);
        assertEquals(2, experiment.getTraces().length);
        experiment = makeTmfExperiment(EXP_DJANGO_INDEX);
        assertEquals(3, experiment.getTraces().length);
    }

    long errors = 0;
    long events = 0;

    /**
     * Check synchronization works properly
     *
     * @throws InterruptedException
     *             error
     */
    @Test
    public void testTraceSynchronization() throws InterruptedException {
        TmfExperiment experiment = makeTmfExperiment(EXP_PHD_HOG);
        TmfEventRequest request = new TmfEventRequest(ITmfEvent.class, 0, ITmfEventRequest.ALL_DATA, ExecutionType.BACKGROUND) {
            public long prev = 0;

            @Override
            public void handleData(ITmfEvent event) {
                events++;
                if (prev > event.getTimestamp().getValue()) {
                    errors++;
                }
                prev = event.getTimestamp().getValue();
            }
        };
        experiment.sendRequest(request);
        request.waitForCompletion();
        assertTrue(events > 0);
        assertEquals(0, errors);
    }

    /**
     * Test swappers are correctly created (with analysis module helper)
     *
     * The EXP_PHD_HOG traces does have only one CPU each
     *
     * @throws Throwable
     *             exception
     */
    @Test
    public void testSwapperExistsWithHelper() throws Throwable {
        TmfExperiment experiment = makeTmfExperiment(EXP_PHD_HOG);
        TmfTraceOpenedSignal signal = new TmfTraceOpenedSignal(this, experiment, null);
        experiment.traceOpened(signal);

        LttngKernelExecutionGraph module = new LttngKernelExecutionGraph();
        module.setId(LttngKernelExecutionGraph.ANALYSIS_ID);
        module.setTrace(experiment);
        TmfTestHelper.executeAnalysis(module);

        TmfModelRegistry registry = module.getModelRegistry();
        TmfSystemModelWithCpu model = registry.getModel(TmfSystemModelWithCpu.class);
        assertEquals(2, experiment.getTraces().length);
        for (ITmfTrace trace : experiment.getTraces()) {
            assertEquals(1, model.getSwappers(trace.getHostId()).size());
        }
    }

    /**
     * Test Django scenario graph construction
     *
     * @throws Throwable
     *             exception
     */
    @Test
    public void testGraphDjango() throws Throwable {
        TmfExperiment experiment = makeTmfExperiment(EXP_DJANGO_INDEX);
        TmfTraceOpenedSignal signal = new TmfTraceOpenedSignal(this, experiment, null);
        experiment.traceOpened(signal);

        LttngKernelExecutionGraph module = new LttngKernelExecutionGraph();
        module.setId(LttngKernelExecutionGraph.ANALYSIS_ID);
        module.setTrace(experiment);
        TmfTestHelper.executeAnalysis(module);
        TmfGraph graph = module.getGraph();

        // search for the client thread
        TmfWorker client = null;
        ArrayListMultimap<Object, TmfVertex> nodesMap = graph.getNodesMap();
        for (Object obj : nodesMap.keySet()) {
            if (obj instanceof TmfWorker) {
                TmfWorker worker = (TmfWorker) obj;
                if (worker.getName().equals("/home/ubuntu/.virtualenvs/wkdb/bin/python")) {
                    client = worker;
                    break;
                }
            }
        }
        System.out.println("client = " + client);
        assertNotNull(client);
    }

    /**
     * Check that outgoing packets matches the currently scheduled process
     * Requires the trace to have
     *
     * @throws Throwable
     *             exception
     */
    @Test
    public void testAssertOutgoingNet() throws Throwable {
        TmfExperiment experiment = makeTmfExperiment(EXP_DJANGO_INDEX);
        TmfTraceOpenedSignal signal = new TmfTraceOpenedSignal(this, experiment, null);
        experiment.traceOpened(signal);

        LttngKernelExecutionGraph module = new LttngKernelExecutionGraph() {
            @Override
            protected ITmfGraphProvider getGraphProvider() {
                return new LttngKernelExecGraphProvider(getModelRegistry(), getTrace()) {
                    @Override
                    public List<AnalysisPhase> makeAnalysis() {
                        List<AnalysisPhase> phases = super.makeAnalysis();
                        AnalysisPhase last = phases.get(phases.size() - 1);
                        last.registerHandler(new String[] { TcpEventStrings.INET_SOCK_LOCAL_OUT },
                                new CheckNetOutgoing(this));
                        last.registerHandler(new String[] { LttngStrings.SCHED_TTWU, LttngStrings.SCHED_WAKEUP },
                                new CheckTTWU(this));
                        return phases;
                    }
                };
            }
        };

        module.setId(LttngKernelExecutionGraph.ANALYSIS_ID);
        module.setTrace(experiment);
        TmfTestHelper.executeAnalysis(module);
        System.out.println(module); // (void) module;
    }

    private class CheckNetOutgoing extends AbstractTraceEventHandler {

        private AbstractTmfGraphProvider fProvider;
        private TmfSystemModelWithCpu fSystem;

        public CheckNetOutgoing(AbstractTmfGraphProvider provider) {
            super();
            fProvider = provider;
            fSystem = provider.getModelRegistry().getModel(TmfSystemModelWithCpu.class);
            System.out.println(fProvider + " " + fSystem); // (void)
        }

        @Override
        public void handleEvent(ITmfEvent event) {
            Long exp = (Long) event.getContent().getField("context._tid").getValue();
            TmfWorker worker = fSystem.getWorkerCpu(event.getTrace().getHostId(), Integer.parseInt(event.getSource()));
            Long act = worker.getId();
            assertEquals(exp, act);
        }

    }

    private class CheckTTWU extends AbstractTraceEventHandler {

        private AbstractTmfGraphProvider fProvider;
        private TmfSystemModelWithCpu fSystem;

        public CheckTTWU(AbstractTmfGraphProvider provider) {
            super();
            fProvider = provider;
            fSystem = provider.getModelRegistry().getModel(TmfSystemModelWithCpu.class);
            System.out.println(fProvider + " " + fSystem); // (void)
        }

        @Override
        public void handleEvent(ITmfEvent event) {
            Long exp = (Long) event.getContent().getField("context._tid").getValue();
            TmfWorker worker = fSystem.getWorkerCpu(event.getTrace().getHostId(), Integer.parseInt(event.getSource()));
            Long act = worker.getId();
            assertEquals(exp, act);
        }

    }

}
