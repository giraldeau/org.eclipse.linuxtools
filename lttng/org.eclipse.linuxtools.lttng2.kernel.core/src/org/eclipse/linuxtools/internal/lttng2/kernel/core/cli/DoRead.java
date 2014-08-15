package org.eclipse.linuxtools.internal.lttng2.kernel.core.cli;

import org.eclipse.linuxtools.tmf.analysis.graph.core.ctf.CtfTraceFinder;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;

public class DoRead implements IBenchRunner {

    @Override
    public void setup(BenchContext ctx) {
        TmfExperiment experiment = ctx.get(TmfExperiment.class);
        CtfTraceFinder.synchronizeExperimentWithPreSync(experiment);
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
