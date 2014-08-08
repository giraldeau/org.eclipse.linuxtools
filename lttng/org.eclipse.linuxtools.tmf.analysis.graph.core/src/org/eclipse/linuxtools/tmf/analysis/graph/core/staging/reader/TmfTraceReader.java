package org.eclipse.linuxtools.tmf.analysis.graph.core.staging.reader;

import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.request.TmfEventRequest;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.trace.ITmfContext;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

public class TmfTraceReader {

    public void readTraceRaw(ITmfTrace trace, TmfEventRequest request) {
        TmfTimeRange range = request.getRange();
        ITmfContext pos = trace.seekEvent(range.getStartTime());
        request.handleStarted();
        while(!request.isCancelled()) {
            ITmfEvent event = trace.getNext(pos);
            if (event == null || event.getTimestamp().compareTo(range.getEndTime()) > 0) {
                break;
            }
            request.handleData(event);
        }
        request.handleCompleted();
    }

}
