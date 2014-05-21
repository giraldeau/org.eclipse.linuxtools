package org.eclipse.linuxtools.lttng2.kernel.core.tests.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.linuxtools.internal.lttng2.kernel.core.LttngStrings;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.TcpEventStrings;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building.LttngKernelExecGraphProvider;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building.LttngKernelExecutionGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfVertex;
import org.eclipse.linuxtools.tmf.analysis.graph.core.building.AbstractTmfGraphProvider;
import org.eclipse.linuxtools.tmf.analysis.graph.core.building.AbstractTraceEventHandler;
import org.eclipse.linuxtools.tmf.analysis.graph.core.building.AnalysisPhase;
import org.eclipse.linuxtools.tmf.analysis.graph.core.building.ITmfGraphProvider;
import org.eclipse.linuxtools.tmf.analysis.graph.core.criticalpath.CriticalPathAlgorithmBounded;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelRegistry;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfSystemModelWithCpu;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfWorker;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.request.ITmfEventRequest;
import org.eclipse.linuxtools.tmf.core.request.ITmfEventRequest.ExecutionType;
import org.eclipse.linuxtools.tmf.core.request.TmfEventRequest;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.linuxtools.tmf.core.tests.shared.TmfTestHelper;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.junit.Test;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

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

    /**
     * django-index experiment on three hosts
     * http://secretaire.dorsal.polymtl.ca/~fgiraldeau/traces/wget-100M.tar.gz
     */
    private static String EXP_WGET = "wget-100M";

    private static String TRACE_DIR = "traces";

    public static String DJANGO_CLIENT_NAME = "/home/ubuntu/.virtualenvs/wkdb/bin/python";

    /**
     * Test that traces are correctly found
     */
    @Test
    public void testFindCtfTraces() {
        List<Path> findCtfTrace = CtfTraceFinder.findCtfTrace(Paths.get(TRACE_DIR, EXP_PHD_HOG));
        assertEquals(2, findCtfTrace.size());
    }

    /**
     * Test experiment instantiation
     */
    @Test
    public void testMakeExperiment() {
        TmfExperiment experiment = CtfTraceFinder.makeTmfExperiment(Paths.get(TRACE_DIR, EXP_PHD_HOG));
        assertEquals(2, experiment.getTraces().length);
        experiment = CtfTraceFinder.makeTmfExperiment(Paths.get(TRACE_DIR, EXP_DJANGO_INDEX));
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
        TmfExperiment experiment = CtfTraceFinder.makeTmfExperiment(Paths.get(TRACE_DIR, EXP_PHD_HOG));
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
        TmfExperiment experiment = CtfTraceFinder.makeTmfExperiment(Paths.get(TRACE_DIR, EXP_PHD_HOG));
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
        TmfExperiment experiment = CtfTraceFinder.makeTmfExperiment(Paths.get(TRACE_DIR, EXP_DJANGO_INDEX));
        TmfTraceOpenedSignal signal = new TmfTraceOpenedSignal(this, experiment, null);
        experiment.traceOpened(signal);

        LttngKernelExecutionGraph module = new LttngKernelExecutionGraph();
        module.setId(LttngKernelExecutionGraph.ANALYSIS_ID);
        module.setTrace(experiment);
        TmfTestHelper.executeAnalysis(module);
        TmfGraph graph = module.getGraph();

        // search for the client thread
        TmfWorker client = findWorkerByName(graph, DJANGO_CLIENT_NAME);
        System.out.println("client = " + client);
        assertNotNull(client);
    }

    /**
     * TmfWorker linear search by process name
     * @param graph the graph
     * @param name the worker process name
     * @return worker object if found, null otherwise
     */
    public static TmfWorker findWorkerByName(TmfGraph graph, String name) {
        ArrayListMultimap<Object, TmfVertex> nodesMap = graph.getNodesMap();
        for (Object obj: nodesMap.keySet()) {
            if (obj instanceof TmfWorker) {
                TmfWorker worker = (TmfWorker) obj;
                if (worker.getName().equals(name)) {
                    return worker;
                }
            }
        }
        return null;
    }

    /**
     * TmfWorker linear search by TID
     * @param graph the graph
     * @param tid the worker process id
     * @return worker object if found, null otherwise
     */
    public static TmfWorker findWorkerByTID(TmfGraph graph, Long tid) {
        ArrayListMultimap<Object, TmfVertex> nodesMap = graph.getNodesMap();
        for (Object obj : nodesMap.keySet()) {
            if (obj instanceof TmfWorker) {
                TmfWorker worker = (TmfWorker) obj;
                if (worker.getId() == tid) {
                    return worker;
                }
            }
        }
        return null;
    }
    /**
     * Test wget trace
     * @throws Throwable exception
     */
    @Test
    public void testWgetTrace() throws Throwable {
        TmfExperiment experiment = CtfTraceFinder.makeTmfExperiment(Paths.get(TRACE_DIR, EXP_WGET));
        TmfTraceOpenedSignal signal = new TmfTraceOpenedSignal(this, experiment, null);
        experiment.traceOpened(signal);

        LttngKernelExecutionGraph module = new LttngKernelExecutionGraph();
        module.setId(LttngKernelExecutionGraph.ANALYSIS_ID);
        module.setTrace(experiment);
        TmfTestHelper.executeAnalysis(module);
        TmfGraph graph = module.getGraph();

        // search for the client thread
        TmfWorker client = findWorkerByTID(graph, 1409L);
        TmfVertex head = graph.getHead(client);
        TmfVertex tail = graph.getTail(client);
        CriticalPathAlgorithmBounded algo = new CriticalPathAlgorithmBounded(graph);
        TmfGraph path = algo.compute(head, tail);
        System.out.println("client = " + client);
        assertNotNull(client);
        assertNotNull(path);
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
        TmfExperiment experiment = CtfTraceFinder.makeTmfExperiment(Paths.get(TRACE_DIR, EXP_DJANGO_INDEX));
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
        private Table<String, Integer, Boolean> schedState;

        public CheckTTWU(AbstractTmfGraphProvider provider) {
            super();
            schedState = HashBasedTable.create();
            fProvider = provider;
            fSystem = provider.getModelRegistry().getModel(TmfSystemModelWithCpu.class);
            System.out.println(fProvider + " " + fSystem); // (void)
        }

        @Override
        public void handleEvent(ITmfEvent event) {
            String host = event.getTrace().getHostId();
            Integer cpu = Integer.parseInt(event.getSource());
            if (!schedState.contains(host, cpu) && event.getType().getName().equals("sched_switch")) {
                schedState.put(host, cpu, true);
            }
            if (schedState.contains(host, cpu)) {
                Long exp = (Long) event.getContent().getField("context._tid").getValue();
                TmfWorker worker = fSystem.getWorkerCpu(event.getTrace().getHostId(), Integer.parseInt(event.getSource()));
                Long act = worker.getId();
                assertEquals(exp, act);
            }
        }

    }

}
