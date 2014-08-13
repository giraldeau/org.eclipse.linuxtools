package org.eclipse.linuxtools.tmf.core.synchronization;

import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;

/**
 * @since 4.0
 */
public class TraceShifterReset implements IFunction<TmfExperiment> {

    @Override
    public void apply(TmfExperiment experiment) {
        ITmfTrace[] traces = experiment.getTraces();
        for (ITmfTrace iTmfTrace : traces) {
            iTmfTrace.setTimestampTransform(TmfTimestampTransform.IDENTITY);
        }
    }

}
