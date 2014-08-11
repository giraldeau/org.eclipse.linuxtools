package org.eclipse.linuxtools.lttng2.kernel.core.cli;

import java.util.Collection;

import org.eclipse.linuxtools.tmf.core.event.matching.EventMatchingBuildRequest;
import org.eclipse.linuxtools.tmf.core.event.matching.IMatchProcessingUnit;
import org.eclipse.linuxtools.tmf.core.event.matching.TmfNetworkEventMatching;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

public class TmfNetworkEventMatchingRawReader extends TmfNetworkEventMatching {

    public TmfNetworkEventMatchingRawReader(Collection<? extends ITmfTrace> traces, IMatchProcessingUnit tmfEventMatches) {
        super(traces, tmfEventMatches);
    }

    @Override
    public boolean matchEvents() {
        if (fTraces.size() == 0) {
            return false;
        }
        initMatching();
        for (ITmfTrace trace : fTraces) {
            EventMatchingBuildRequest request = new EventMatchingBuildRequest(this, trace);
            startingRequest(request);
            TmfTraceReader reader = new TmfTraceReader();
            reader.readTraceRaw(trace, request);
        }

        finalizeMatching();
        return true;
    }

}
