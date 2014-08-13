package org.eclipse.linuxtools.tmf.core.synchronization;

import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;

/**
 * @since 4.0
 */
public class TraceShifterOrigin implements IFunction<TmfExperiment> {

    private static final IFunction<TmfExperiment> reset = new TraceShifterReset();

    @Override
    public void apply(TmfExperiment experiment) {
        reset.apply(experiment);
        ITmfTrace[] traces = experiment.getTraces();
        for (int i = 0; i < traces.length - 1; i++) {
            ITmfTrace t1 = traces[i];
            ITmfTrace t2 = traces[i + 1];

            long v1 = t1.getNext(t1.seekEvent(0L)).getTimestamp().getValue();
            long v2 = t2.getNext(t2.seekEvent(0L)).getTimestamp().getValue();
            double beta = -1.0 * (v2 - v1);
            ITmfTimestampTransform xform = new TmfTimestampTransformLinear(1.0, beta);
            t2.setTimestampTransform(xform);
            // FIXME: move to unit tests
//            long v3 = t2.getNext(t2.seekEvent(0L)).getTimestamp().getValue();
//            assertTrue(Math.abs(v1 - v3) < 1000);
        }
    }

}
