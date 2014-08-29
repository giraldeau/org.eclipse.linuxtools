package org.eclipse.linuxtools.tmf.core.synchronization;

import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.trace.ITmfContext;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;

/**
 * @since 4.0
 */
public class TraceShifterDisjoint implements IFunction<TmfExperiment> {

    private static final IFunction<TmfExperiment> reset = new TraceShifterReset();

    @Override
    public void apply(TmfExperiment exp) {
        reset.apply(exp);
        ITmfTrace[] traces = exp.getTraces();

        for (int i = 0; i < traces.length - 1; i++) {
            ITmfTrace t1 = traces[i];
            ITmfTrace t2 = traces[i + 1];

            ITmfContext ctx = t1.seekEvent(0L);
            t1.getNext(ctx);
            ctx = t1.seekEvent(Long.MAX_VALUE);
            ctx = t1.seekEvent(t1.getNbEvents() - 1);
            ITmfEvent lastEv = t1.getNext(ctx);
            assert(lastEv != null);

            ctx = t2.seekEvent(0L);
            ITmfEvent first = t2.getNext(ctx);

            long beta = lastEv.getTimestamp().getValue() - first.getTimestamp().getValue();
            ITmfTimestampTransform xform = TimestampTransformFactory.createWithOffset(beta);
            t2.setTimestampTransform(xform);
        }

        // FIXME: move to unit tests
//        for (int i = 0; i < traces.length - 1; i++) {
//            ITmfTrace t1 = traces[i];
//            ITmfTrace t2 = traces[i + 1];
//
//            // force seek to the end
//            ITmfContext ctx = t1.seekEvent(0L);
//            ctx = t1.seekEvent(Long.MAX_VALUE);
//            ctx = t1.seekEvent(t1.getNbEvents() - 1);
//            ITmfEvent last = t1.getNext(ctx);
//
//            ctx = t2.seekEvent(0L);
//            ITmfEvent first = t2.getNext(ctx);
//
//            long delta = last.getTimestamp().getValue() - first.getTimestamp().getValue();
//            // FIXME: getStartTime() and
//            // getEndTime() are not updated on setTimestampTransform()
//            assertTrue(Math.abs(delta) < 1000);
//        }

    }

}
