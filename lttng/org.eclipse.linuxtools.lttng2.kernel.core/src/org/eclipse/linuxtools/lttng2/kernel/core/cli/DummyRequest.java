package org.eclipse.linuxtools.lttng2.kernel.core.cli;

import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventField;
import org.eclipse.linuxtools.tmf.core.request.ITmfEventRequest;
import org.eclipse.linuxtools.tmf.core.request.TmfEventRequest;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;

public class DummyRequest extends TmfEventRequest {

    private int count = 0;

    public DummyRequest() {
        super(ITmfEvent.class,
                TmfTimeRange.ETERNITY,
                0L,
                ITmfEventRequest.ALL_DATA,
                ITmfEventRequest.ExecutionType.FOREGROUND);
    }

    @Override
    public void handleData(final ITmfEvent event) {
        if (event.getContent() != null) {
            count += event.getContent().getFields().size();
            count += event.getTimestamp().getValue();
            for (ITmfEventField field: event.getContent().getFields()) {
                Object value = field.getValue();
                count += value.hashCode();
            }
        } else {
            throw new RuntimeException("null ctx");
        }
    }

    @Override
    public void handleCompleted() {
        System.out.println("magic number: " + count);
    }
}