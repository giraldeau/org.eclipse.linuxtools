package org.eclipse.linuxtools.tmf.core.event.matching;

import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.request.ITmfEventRequest;
import org.eclipse.linuxtools.tmf.core.request.TmfEventRequest;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

/**
 * @since 4.0
 */
public class EventMatchingBuildRequest extends TmfEventRequest {

    private final TmfEventMatching matching;
    private final ITmfTrace trace;

    public EventMatchingBuildRequest(TmfEventMatching matching, ITmfTrace trace) {
        super(ITmfEvent.class,
                TmfTimeRange.ETERNITY,
                0,
                ITmfEventRequest.ALL_DATA,
                ITmfEventRequest.ExecutionType.FOREGROUND);
        this.matching = matching;
        this.trace = trace;
    }

    @Override
    public void handleData(final ITmfEvent event) {
        super.handleData(event);
        matching.matchEvent(event, trace);
    }

    @Override
    public void handleSuccess() {
        super.handleSuccess();
    }

    @Override
    public void handleCancel() {
        super.handleCancel();
    }

    @Override
    public void handleFailure() {
        super.handleFailure();
    }
}