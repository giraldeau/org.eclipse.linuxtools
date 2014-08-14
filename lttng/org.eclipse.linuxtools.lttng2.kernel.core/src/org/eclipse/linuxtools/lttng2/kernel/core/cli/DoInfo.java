package org.eclipse.linuxtools.lttng2.kernel.core.cli;

import org.eclipse.linuxtools.tmf.analysis.graph.core.ctf.CtfTraceFinder;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.trace.ITmfContext;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;

public class DoInfo implements IBenchRunner {

    @Override
    public void setup(BenchContext ctx) {
        BenchResult res = ctx.get(BenchResult.class);
        TmfExperiment exp = ctx.get(TmfExperiment.class);
        CtfTraceFinder.synchronizeExperimentWithPreSync(exp);
        ITmfContext pos = exp.seekEvent(0L);
        ITmfEvent first = exp.getNext(pos);
        ITmfEvent next, last = null;
        int count = 0;
        while ((next = exp.getNext(pos)) != null) {
            count++;
            last = next;
        }
        if (last == null) {
            throw new RuntimeException("last event is null");
        }
        ITmfTimestamp delta = last.getTimestamp().getDelta(first.getTimestamp());
        System.out.println(first.getTimestamp().getValue());
        String tag = ctx.get(String.class, BenchContext.TAG_TASK_NAME);
        Integer size = ctx.get(Integer.class, BenchContext.TAG_SIZE);
        res.addDataRaw(tag, BenchResult.METRIC_EVENTS, size, count);
        res.addDataRaw(tag, BenchResult.METRIC_DURATION, size, delta.normalize(0, ITmfTimestamp.MILLISECOND_SCALE).getValue());
        System.out.println(res.dumpData());
    }

    @Override
    public void run(BenchContext ctx) {
    }

    @Override
    public void teardown(BenchContext ctx) {
    }

}
