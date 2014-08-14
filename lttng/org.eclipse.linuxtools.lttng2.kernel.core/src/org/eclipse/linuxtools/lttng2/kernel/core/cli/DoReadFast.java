package org.eclipse.linuxtools.lttng2.kernel.core.cli;

import org.eclipse.linuxtools.tmf.analysis.graph.core.ctf.CtfTraceFinder;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;

public class DoReadFast extends DoRead {

    @Override
    public void setup(BenchContext ctx) {
        TmfExperiment experiment = ctx.get(TmfExperiment.class);
        CtfTraceFinder.synchronizeExperimentWithPreSync(experiment);
        CtfTraceFinder.makeFastTransform(experiment);
    }

}
