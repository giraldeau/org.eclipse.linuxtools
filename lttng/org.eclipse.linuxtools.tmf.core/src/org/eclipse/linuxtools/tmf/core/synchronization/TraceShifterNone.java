package org.eclipse.linuxtools.tmf.core.synchronization;

import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;

/**
 * @since 4.0
 */
public class TraceShifterNone implements IFunction<TmfExperiment> {

    @Override
    public void apply(TmfExperiment object) {
        // do nothing
    }

}
