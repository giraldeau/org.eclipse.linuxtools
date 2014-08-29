package org.eclipse.linuxtools.tmf.analysis.graph.core.tests.staging;

import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.eclipse.linuxtools.tmf.analysis.graph.core.ctf.CtfTraceFinder;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.matching.TmfNetworkEventMatching;
import org.eclipse.linuxtools.tmf.core.synchronization.ITmfTimestampTransform;
import org.eclipse.linuxtools.tmf.core.synchronization.TimestampTransformFactory;
import org.eclipse.linuxtools.tmf.core.trace.ITmfContext;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.junit.Test;

/**
 * Test if pre-sync can reduce the memory used for the synchronization (and
 * packet matching in general)
 *
 * The synchronization algorithm reads events one trace at a time, and
 * accumulate the unmatched events. The memory consumption is linear with the
 * trace size. The desired behavior is to work in constant memory.
 *
 * Even if traces are to be read as an experiment, their offset may be large,
 * such that there is no overlap in time, and thus yield the same result.
 *
 * There are many possible solutions:
 *
 * 1- cleanup old unmatched events (age > 6 sigma of round-trip for example):
 * can be done for packet matching once the traces are synchronized (not before
 * synchronization, because there may not have a time overlap between traces)
 *
 * 2- sampling: we don't know in advance the sample rate that we should have in
 * order to use a fixed memory. We would need dynamic sampling rate for a target
 * size. Non-deterministic algorithm, may fail to find a match.
 *
 * 3- assume the traces are started almost at the same time. Apply a timestamps
 * transform in order to shift all traces to the origin, then read them
 * together. Matches should be found while reading the trace. Once the
 * synchronization algorithm is completed, compose the resulting transform with
 * the one used to bring the trace to the origin.
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
public class SyncMemoryScalability {

    public static void makeTest() {
        Path path = Paths.get(TraceStrings.TRACE_DIR, TraceStrings.EXP_DJANGO_INDEX);
        TmfExperiment exp = CtfTraceFinder.makeTmfExperiment(path);
        TmfNetworkEventMatching traceMatch = new TmfNetworkEventMatching(Arrays.asList(exp.getTraces()));
        System.out.println(traceMatch);
        traceMatch.matchEvents();
    }

    @Test
    public void testPacketMatch() {
        makeTest();
        HeapDump.dumpHeap("test-packet-match.hprof", true);
    }

    @Test
    public void testSyncOrigin() {
        Path path = Paths.get(TraceStrings.TRACE_DIR, TraceStrings.EXP_DJANGO_INDEX);
        TmfExperiment exp = CtfTraceFinder.makeTmfExperiment(path);

        // shift all traces to the origin
        ITmfTrace[] traces = exp.getTraces();
        for (ITmfTrace trace : traces) {
            ITmfContext ctx = trace.seekEvent(0L);
            ITmfEvent ev = trace.getNext(ctx);

            long beta = -1 * ev.getTimestamp().getValue();
            ITmfTimestampTransform xform = TimestampTransformFactory.createWithOffset(beta);
            trace.setTimestampTransform(xform);
        }

        // check that resulting offset is close to zero (less than 1us)
        for (int i = 0; i < traces.length; i++) {
            for (int j = i; j < traces.length; j++) {
                ITmfContext ctx1 = traces[i].seekEvent(0L);
                ITmfContext ctx2 = traces[j].seekEvent(0L);
                ITmfEvent ev1 = traces[i].getNext(ctx1);
                ITmfEvent ev2 = traces[j].getNext(ctx2);
                long delta = Math.abs(ev1.getTimestamp().getDelta(ev2.getTimestamp()).getValue());
                assertTrue(delta < 1000);
            }
        }
    }

}
