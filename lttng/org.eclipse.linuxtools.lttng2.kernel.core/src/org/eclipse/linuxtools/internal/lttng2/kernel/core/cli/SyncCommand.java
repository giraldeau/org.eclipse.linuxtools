package org.eclipse.linuxtools.internal.lttng2.kernel.core.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building.LttngKernelExecutionGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.ctf.CtfTraceFinder;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;

/**
 * Abstract base command handler
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
public class SyncCommand implements ICommand {

    @Override
    public void handle(CommandLine opts) {
        String[] basePaths = opts.getArgs();
        for (String path: basePaths) {
            List<Path> subtraces = CtfTraceFinder.getTracePathsByCreationTime(Paths.get(path));
            TmfExperiment exp = CtfTraceFinder.makeTmfExperiment(subtraces);

            /* sync */
            CtfTraceFinder.synchronizeExperimentWithPreSync(exp);
            for (ITmfTrace trace : exp.getTraces()) {
                System.out.println(trace.getPath() + ": " + trace.getTimestampTransform().toString());
            }

            /* main analysis */
            try (LttngKernelExecutionGraph module = new LttngKernelExecutionGraph()) {
                module.setId(LttngKernelExecutionGraph.ANALYSIS_ID);
                module.setTrace(exp);
                TmfModuleRunnerHelper.executeAnalysis(module, new NullProgressMonitor());
            } catch (TmfAnalysisException e) {
                System.out.println("Analysis exception: " + e.toString());
            }
        }
    }

    @Override
    public void createOptions(Options options) {
    }

}
