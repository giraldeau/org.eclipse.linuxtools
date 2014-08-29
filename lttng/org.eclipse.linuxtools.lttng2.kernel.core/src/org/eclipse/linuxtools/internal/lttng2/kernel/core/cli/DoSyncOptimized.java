package org.eclipse.linuxtools.internal.lttng2.kernel.core.cli;

import java.util.Collections;

import org.eclipse.linuxtools.tmf.analysis.graph.core.ctf.CtfTraceFinder;
import org.eclipse.linuxtools.tmf.core.event.matching.ExpireCleanupMonitor;
import org.eclipse.linuxtools.tmf.core.event.matching.StopEarlyMonitor;
import org.eclipse.linuxtools.tmf.core.event.matching.TmfNetworkEventMatching;
import org.eclipse.linuxtools.tmf.core.synchronization.IFunction;
import org.eclipse.linuxtools.tmf.core.synchronization.SynchronizationAlgorithm;
import org.eclipse.linuxtools.tmf.core.synchronization.SynchronizationAlgorithmFactory;
import org.eclipse.linuxtools.tmf.core.synchronization.TraceShifterOrigin;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;

public class DoSyncOptimized implements IBenchRunner {

    @Override
    public void setup(BenchContext ctx) {
    }

    @Override
    public void run(BenchContext ctx) {
        BenchResult res = ctx.get(BenchResult.class);
        TmfExperiment experiment = ctx.get(TmfExperiment.class);

        SynchronizationAlgorithm algo;
        TmfNetworkEventMatching matching;

        res.begin(ctx);

        IFunction<TmfExperiment> func = new TraceShifterOrigin();
        func.apply(experiment);
        algo = SynchronizationAlgorithmFactory.getFullyIncrementalAlgorithm();
        matching = new TmfNetworkEventMatchingRawReader(Collections.singleton(experiment), algo);
        matching.addMatchMonitor(new StopEarlyMonitor());
        matching.matchEvents();
        CtfTraceFinder.applyComposeTransform(algo, experiment);

        algo = SynchronizationAlgorithmFactory.getFullyIncrementalAlgorithm();
        matching = new TmfNetworkEventMatchingRawReader(Collections.singleton(experiment), algo);
        matching.addMatchMonitor(new ExpireCleanupMonitor());
        matching.matchEvents();
        CtfTraceFinder.applyComposeTransform(algo, experiment);

        res.done(ctx);
        String tag = ctx.get(String.class, BenchContext.TAG_TASK_NAME);
        Integer size = ctx.get(Integer.class, BenchContext.TAG_SIZE);
        res.addDataRaw(tag, BenchResult.METRIC_UNMATCHED, size, matching.getMaxUnmatchedCount());
        res.addDataRaw(tag, BenchResult.METRIC_MATCHED, size, matching.getMatchedCount());
    }

    @Override
    public void teardown(BenchContext ctx) {
    }

}
