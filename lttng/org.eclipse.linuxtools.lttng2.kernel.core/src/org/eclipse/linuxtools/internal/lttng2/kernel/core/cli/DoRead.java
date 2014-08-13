package org.eclipse.linuxtools.internal.lttng2.kernel.core.cli;

import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventField;
import org.eclipse.linuxtools.tmf.core.request.ITmfEventRequest;
import org.eclipse.linuxtools.tmf.core.request.TmfEventRequest;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;

public class DoRead implements IBenchRunner {

    private static class DummyRequest extends TmfEventRequest {

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

    @Override
    public void setup(BenchContext ctx) {
    }

    @Override
    public void run(BenchContext ctx) {
        BenchResult res = ctx.get(BenchResult.class);
        TmfExperiment experiment = ctx.get(TmfExperiment.class);
        res.begin(ctx);
        DummyRequest rq = new DummyRequest();
        experiment.sendRequest(rq);
        try {
            rq.waitForCompletion();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        res.done(ctx);
    }

    @Override
    public void teardown(BenchContext ctx) {
    }

}
