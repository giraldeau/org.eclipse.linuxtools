package org.eclipse.linuxtools.tmf.analysis.graph.core.tests.staging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfVertex;
import org.eclipse.linuxtools.tmf.analysis.graph.core.ctf.CtfTraceFinder;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfWorker;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.request.ITmfEventRequest;
import org.eclipse.linuxtools.tmf.core.request.ITmfEventRequest.ExecutionType;
import org.eclipse.linuxtools.tmf.core.request.TmfEventRequest;
import org.eclipse.linuxtools.tmf.core.synchronization.ITmfTimestampTransform;
import org.eclipse.linuxtools.tmf.core.synchronization.SyncAlgorithmFullyIncremental;
import org.eclipse.linuxtools.tmf.core.synchronization.SynchronizationAlgorithm.SyncQuality;
import org.eclipse.linuxtools.tmf.core.synchronization.TmfTimestampTransform;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
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
     * Test that traces are correctly found
     */
    @Test
    public void testFindCtfTraces() {
        Path path = Paths.get(TraceStrings.TRACE_DIR, TraceStrings.EXP_PHD_HOG);
        List<Path> findCtfTrace = CtfTraceFinder.findCtfTrace(path);
        assertEquals(2, findCtfTrace.size());
    }

    /**
     * Test experiment instantiation
     */
    @Test
    public void testMakeExperiment() {
        Path path1 = Paths.get(TraceStrings.TRACE_DIR, TraceStrings.EXP_PHD_HOG);
        Path path2 = Paths.get(TraceStrings.TRACE_DIR, TraceStrings.EXP_DJANGO_INDEX);
        TmfExperiment experiment = CtfTraceFinder.makeSynchronizedTmfExperiment(path1);
        assertEquals(2, experiment.getTraces().length);
        experiment = CtfTraceFinder.makeSynchronizedTmfExperiment(path2);
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
        Path path = Paths.get(TraceStrings.TRACE_DIR, TraceStrings.EXP_PHD_HOG);
        TmfExperiment experiment = CtfTraceFinder.makeSynchronizedTmfExperiment(path);
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
     * Bug with trace synchronization: only one trace should have IDENTITY transform
     */
    @Test
    public void testSyncTransform() {
        Path path = Paths.get(TraceStrings.TRACE_DIR, TraceStrings.EXP_BUG_SYNC);
        TmfExperiment experiment = CtfTraceFinder.makeTmfExperiment(path);
        SyncAlgorithmFullyIncremental algo = (SyncAlgorithmFullyIncremental) CtfTraceFinder.synchronizeExperiment(experiment);

        for (ITmfTrace i: experiment.getTraces()) {
            for (ITmfTrace j: experiment.getTraces()) {
                SyncQuality q = algo.getSynchronizationQuality(i, j);
                System.out.println(i.getHostId() + " ---> " + j.getHostId() + " " + q);
            }
        }

        int identity = 0;
        ArrayList<String> hostsList = new ArrayList<>();
        for (ITmfTrace trace: experiment.getTraces()) {
            hostsList.add(trace.getHostId());
        }
        Collections.sort(hostsList);
        System.out.println(hostsList);
        System.out.println("base trace: " + hostsList.get(0));
        for (ITmfTrace trace: experiment.getTraces()) {
            String host = trace.getHostId();
            //algo.getTimestampTransform(0, host);
            ITmfTimestampTransform xform = algo.getTimestampTransform(host);
            System.out.println("result " + trace.getHostId() + " " + xform);
            if (xform == TmfTimestampTransform.IDENTITY) {
                identity++;
            }
        }
        assertEquals(1, identity);
    }

}
