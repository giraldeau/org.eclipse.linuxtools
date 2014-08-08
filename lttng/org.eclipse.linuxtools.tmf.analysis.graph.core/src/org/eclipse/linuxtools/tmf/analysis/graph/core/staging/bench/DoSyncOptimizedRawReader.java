package org.eclipse.linuxtools.tmf.analysis.graph.core.staging.bench;

import java.util.Collections;

import org.eclipse.linuxtools.tmf.analysis.graph.core.ctf.CtfTraceFinder;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.reader.TmfNetworkEventMatchingRawReader;
import org.eclipse.linuxtools.tmf.core.event.matching.ExpireCleanupMonitor;
import org.eclipse.linuxtools.tmf.core.event.matching.StopEarlyMonitor;
import org.eclipse.linuxtools.tmf.core.event.matching.TmfNetworkEventMatching;
import org.eclipse.linuxtools.tmf.core.synchronization.IFunction;
import org.eclipse.linuxtools.tmf.core.synchronization.SyncAlgorithmFullyIncremental;
import org.eclipse.linuxtools.tmf.core.synchronization.TraceShifterOrigin;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;

public class DoSyncOptimizedRawReader implements IBenchRunner {

    @Override
    public void setup(BenchContext ctx) {
    }

    @Override
    public void run(BenchContext ctx) {
        BenchResult res = ctx.get(BenchResult.class);
        TmfExperiment experiment = ctx.get(TmfExperiment.class);

        SyncAlgorithmFullyIncremental algo;
        TmfNetworkEventMatching matching;

        res.begin(ctx);

        IFunction<TmfExperiment> func = new TraceShifterOrigin();
        func.apply(experiment);
        algo = new SyncAlgorithmFullyIncremental();
        matching = new TmfNetworkEventMatchingRawReader(Collections.singleton(experiment), algo);
        matching.addMatchMonitor(new StopEarlyMonitor());
        matching.matchEvents();
        CtfTraceFinder.applyComposeTransform(algo, experiment);

        algo = new SyncAlgorithmFullyIncremental();
        matching = new TmfNetworkEventMatchingRawReader(Collections.singleton(experiment), algo);
        matching.addMatchMonitor(new ExpireCleanupMonitor());
        matching.matchEvents();
        CtfTraceFinder.applyComposeTransform(algo, experiment);

        res.done(ctx);
    }

    @Override
    public void teardown(BenchContext ctx) {
    }

}
