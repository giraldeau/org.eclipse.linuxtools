package org.eclipse.linuxtools.tmf.analysis.graph.core.staging.bench;

import org.eclipse.linuxtools.tmf.analysis.graph.core.ctf.CtfTraceFinder;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;

public class DoSyncRaw implements IBenchRunner {

    @Override
    public void setup(BenchContext ctx) {
    }

    @Override
    public void run(BenchContext ctx) {
        BenchResult res = ctx.get(BenchResult.class);
        TmfExperiment experiment = ctx.get(TmfExperiment.class);
        res.begin(ctx);
        CtfTraceFinder.synchronizeExperiment(experiment);
        res.done(ctx);
    }

    @Override
    public void teardown(BenchContext ctx) {
    }

}
