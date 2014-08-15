package org.eclipse.linuxtools.internal.lttng2.kernel.core.cli;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building.LttngKernelExecutionGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.ctf.CtfTraceFinder;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;

@SuppressWarnings("javadoc")
public class DoBuild implements IBenchRunner {

    @Override
    public void setup(BenchContext ctx) {
        TmfExperiment exp = ctx.get(TmfExperiment.class);
        CtfTraceFinder.synchronizeExperimentWithPreSync(exp);
    }

    @Override
    public void run(BenchContext ctx) {
        BenchResult res = ctx.get(BenchResult.class);
        TmfExperiment exp = ctx.get(TmfExperiment.class);
        try (LttngKernelExecutionGraph module = new LttngKernelExecutionGraph()) {
            module.setId(LttngKernelExecutionGraph.ANALYSIS_ID);
            module.setTrace(exp);
            res.begin(ctx);
            TmfModuleRunnerHelper.executeAnalysis(module, new NullProgressMonitor());
            res.done(ctx);
        } catch (TmfAnalysisException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void teardown(BenchContext ctx) {
    }

}
