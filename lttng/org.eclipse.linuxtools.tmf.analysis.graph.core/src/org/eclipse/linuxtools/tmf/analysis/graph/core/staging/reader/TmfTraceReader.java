package org.eclipse.linuxtools.tmf.analysis.graph.core.staging.reader;

import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.request.TmfEventRequest;
import org.eclipse.linuxtools.tmf.core.trace.ITmfContext;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

public class TmfTraceReader {

    public TmfTraceReader() {
    }

    public void readTraceRaw(ITmfTrace trace, TmfEventRequest request) {
        ITmfContext pos = trace.seekEvent(0L);
        request.handleStarted();
        int count = 0;
        while(!request.isCancelled()) {
            ITmfEvent event = trace.getNext(pos);
            if (event == null) {
                break;
            }
            count++;
            request.handleData(event);
        }
        request.handleCompleted();
        System.out.println("\nevents reads: " + count);
    }

}
