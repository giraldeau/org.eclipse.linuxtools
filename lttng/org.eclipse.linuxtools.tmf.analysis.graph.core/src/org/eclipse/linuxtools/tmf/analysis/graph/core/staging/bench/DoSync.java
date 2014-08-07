package org.eclipse.linuxtools.tmf.analysis.graph.core.staging.bench;

import org.eclipse.linuxtools.tmf.analysis.graph.core.ctf.CtfTraceFinder;
import org.eclipse.linuxtools.tmf.core.synchronization.SynchronizationAlgorithm;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;

public class DoSync implements IBenchRunner {

    @Override
    public void setup(BenchContext ctx) {
    }

    @Override
    public void run(BenchContext ctx) {
        BenchResult res = ctx.get(BenchResult.class);
        String name = ctx.get(String.class, BenchContext.TAG_TASK_NAME);
        TmfExperiment experiment = ctx.get(TmfExperiment.class);
        res.begin();
        SynchronizationAlgorithm algo = CtfTraceFinder.synchronizeExperiment(experiment);
        res.done(name, 1);
        assert(algo != null);
    }

    @Override
    public void teardown(BenchContext ctx) {
    }

}
