package org.eclipse.linuxtools.tmf.analysis.graph.core.staging.bench;

import java.util.Collections;

import org.eclipse.linuxtools.tmf.analysis.graph.core.ctf.CtfTraceFinder;
import org.eclipse.linuxtools.tmf.core.event.matching.TmfNetworkEventMatching;
import org.eclipse.linuxtools.tmf.core.synchronization.SyncAlgorithmFullyIncremental;
import org.eclipse.linuxtools.tmf.core.synchronization.SynchronizationAlgorithm;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;

public class DoSync implements IBenchRunner {

    @Override
    public void setup(BenchContext ctx) {
    }

    @Override
    public void run(BenchContext ctx) {
        BenchResult res = ctx.get(BenchResult.class);
        TmfExperiment experiment = ctx.get(TmfExperiment.class);
        res.begin(ctx);
        SynchronizationAlgorithm algo = new SyncAlgorithmFullyIncremental();
        TmfNetworkEventMatching matching = new TmfNetworkEventMatching(Collections.singleton(experiment), algo);
        matching.matchEvents();
        CtfTraceFinder.applyComposeTransform(algo, experiment);
        res.done(ctx);
    }

    @Override
    public void teardown(BenchContext ctx) {
    }

}
