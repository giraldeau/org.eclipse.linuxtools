package org.eclipse.linuxtools.internal.lttng2.kernel.core.cli;

import java.util.Collections;

import org.eclipse.linuxtools.tmf.core.event.matching.TmfNetworkEventMatching;
import org.eclipse.linuxtools.tmf.core.synchronization.IFunction;
import org.eclipse.linuxtools.tmf.core.synchronization.SyncAlgorithmFullyIncremental;
import org.eclipse.linuxtools.tmf.core.synchronization.TraceShifterDisjoint;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;

public class DoSyncWorst implements IBenchRunner {

    @Override
    public void setup(BenchContext ctx) {
    }

    @Override
    public void run(BenchContext ctx) {
        BenchResult res = ctx.get(BenchResult.class);
        TmfExperiment experiment = ctx.get(TmfExperiment.class);
        IFunction<TmfExperiment> distjoint = new TraceShifterDisjoint();
        distjoint.apply(experiment);
        res.begin(ctx);
        SyncAlgorithmFullyIncremental algo = new SyncAlgorithmFullyIncremental();
        TmfNetworkEventMatching matching = new TmfNetworkEventMatching(Collections.singleton(experiment), algo);
        matching.matchEvents();
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
