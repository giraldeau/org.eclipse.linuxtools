package org.eclipse.linuxtools.internal.lttng2.kernel.core.cli;

import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building.LttngKernelExecutionGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfVertex;
import org.eclipse.linuxtools.tmf.analysis.graph.core.criticalpath.CriticalPathAlgorithmBounded;
import org.eclipse.linuxtools.tmf.analysis.graph.core.ctf.CtfTraceFinder;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfWorker;
import org.eclipse.linuxtools.tmf.core.analysis.IAnalysisModule;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;

@SuppressWarnings("javadoc")
public class DoExtract implements IBenchRunner {

    private TmfGraph fGraph;
    private TmfWorker fWorker;

    @Override
    public void setup(BenchContext ctx) {
        TmfExperiment exp = ctx.get(TmfExperiment.class);
        CtfTraceFinder.synchronizeExperimentWithPreSync(exp);
        TmfTraceOpenedSignal signal = new TmfTraceOpenedSignal(this, exp, null);
        exp.traceOpened(signal);
        IAnalysisModule module = exp.getAnalysisModule(LttngKernelExecutionGraph.ANALYSIS_ID);
        if (module == null) {
            throw new RuntimeException("the analysis module is null and should not be");
        }
        TmfModuleRunnerHelper.executeAnalysis(module, new NullProgressMonitor());
        fGraph = ((LttngKernelExecutionGraph) module).getGraph();
        Set<Object> keySet = fGraph.getNodesMap().keySet();
        for (Object obj: keySet) {
            if (obj instanceof TmfWorker) {
                TmfWorker worker = (TmfWorker) obj;
                if (worker.getName().endsWith("python")) {
                    fWorker = worker;
                    break;
                }
            }
        }
        if (fWorker == null) {
            throw new RuntimeException("worker not found");
        }

        module.dispose();
    }

    @Override
    public void run(BenchContext ctx) {
        BenchResult res = ctx.get(BenchResult.class);
        res.begin(ctx);
        CriticalPathAlgorithmBounded algo = new CriticalPathAlgorithmBounded(fGraph);
        TmfVertex head = fGraph.getHead(fWorker);
        TmfVertex tail = fGraph.getTail(fWorker);
        TmfGraph result = algo.compute(head, tail);
        System.out.println(result.size());
        res.done(ctx);
    }

    @Override
    public void teardown(BenchContext ctx) {
    }

}
