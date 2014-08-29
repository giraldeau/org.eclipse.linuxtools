package org.eclipse.linuxtools.internal.lttng2.kernel.core.cli;

import org.eclipse.linuxtools.internal.tmf.core.synchronization.TmfTimestampTransformLinearFast;
import org.eclipse.linuxtools.tmf.analysis.graph.core.ctf.CtfTraceFinder;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;

public class DoReadFast extends DoRead {

    @Override
    public void setup(BenchContext ctx) {
        super.setup(ctx);
        TmfExperiment experiment = ctx.get(TmfExperiment.class);
        CtfTraceFinder.makeFastTransform(experiment);
    }

    @Override
    public void run(BenchContext ctx) {
        super.run(ctx);
        TmfExperiment experiment = ctx.get(TmfExperiment.class);
        String tag = ctx.get(String.class, BenchContext.TAG_TASK_NAME);
        Integer size = ctx.get(Integer.class, BenchContext.TAG_SIZE);
        BenchResult res = ctx.get(BenchResult.class);
        long totalMiss = 0;
        for (ITmfTrace trace: experiment.getTraces()) {
            TmfTimestampTransformLinearFast xform = (TmfTimestampTransformLinearFast) trace.getTimestampTransform();
            totalMiss += xform.getScaleMiss();
            xform.setScaleMiss(0);
        }
        res.addDataRaw(tag, BenchResult.METRIC_SCALEMISS, size, totalMiss);
    }

}
