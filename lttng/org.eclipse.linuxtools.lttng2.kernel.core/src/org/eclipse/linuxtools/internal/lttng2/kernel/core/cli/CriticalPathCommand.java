package org.eclipse.linuxtools.internal.lttng2.kernel.core.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building.LttngKernelExecutionGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfVertex;
import org.eclipse.linuxtools.tmf.analysis.graph.core.criticalpath.CriticalPathAlgorithmBounded;
import org.eclipse.linuxtools.tmf.analysis.graph.core.ctf.CtfTraceFinder;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.Dot;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfWorker;
import org.eclipse.linuxtools.tmf.core.analysis.IAnalysisModule;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;

/**
 * Compute critical path on the command line
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
@SuppressWarnings("nls")
public class CriticalPathCommand extends BaseCommand {

    private static final String OPT_TID = "tid";

    @Override
    public void handle(CommandLine opts) {
        String[] tids = opts.getOptionValue(OPT_TID).split(",");
        Long[] tidsLong = new Long[tids.length];
        for (int i = 0; i < tids.length; i++) {
            tidsLong[i] = Long.parseLong(tids[i]);
        }
        ArrayList<Path> paths = new ArrayList<>();
        for (String str: opts.getArgs()) {
            paths.add(Paths.get(str));
        }
        TmfExperiment exp = CtfTraceFinder.makeTmfExperiment(paths);
        CtfTraceFinder.synchronizeExperiment(exp);
        TmfTraceOpenedSignal signal = new TmfTraceOpenedSignal(new Object(), exp, null);
        exp.traceOpened(signal);

        IAnalysisModule module = exp.getAnalysisModule(LttngKernelExecutionGraph.ANALYSIS_ID);
        if (module == null) {
            throw new RuntimeException("the analysis module is null and should not be");
        }
        TmfModuleRunnerHelper.executeAnalysis(module, new NullProgressMonitor());
        TmfGraph graph = ((LttngKernelExecutionGraph) module).getGraph();
        Set<Object> keySet = graph.getNodesMap().keySet();
        int toFind = tidsLong.length;
        ArrayList<TmfWorker> workersToAnalyze = new ArrayList<>();
        for (Object obj: keySet) {
            if (obj instanceof TmfWorker) {
                TmfWorker worker = (TmfWorker) obj;
                for (Long tid: tidsLong) {
                    if (worker.getId() == tid) {
                        workersToAnalyze.add(worker);
                        toFind--;
                    }
                }
            }
            if (toFind == 0) {
                break;
            }
        }
        if (workersToAnalyze.isEmpty()) {
            module.dispose();
            throw new RuntimeException("worker not found");
        }

        CriticalPathAlgorithmBounded algo = new CriticalPathAlgorithmBounded(graph);
        for (TmfWorker worker: workersToAnalyze) {
            TmfVertex head = graph.getHead(worker);
            TmfVertex tail = graph.getTail(worker);
            TmfGraph result = algo.compute(head, tail);
            Dot.writeString("output", String.format("path-%d.dot", worker.getId()), Dot.todot(result));
        }

        Dot.writeString("output", "graph.dot", Dot.todot(graph, workersToAnalyze));
        module.dispose();

    }

    @Override
    public void createOptions(Options options) {
        options.addOption("t", OPT_TID, true, "analyze this tid");
    }


}
