package org.eclipse.linuxtools.lttng2.kernel.core.cli;

import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.linuxtools.tmf.analysis.graph.core.ctf.CtfTraceFinder;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.EventHandler;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.EventHandler.Ctx;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.ExecGraphModule;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.ITaskListener;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Task;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Task.StateEnum;
import org.eclipse.linuxtools.tmf.core.analysis.IAnalysisModule;
import org.eclipse.linuxtools.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.linuxtools.tmf.core.request.ITmfEventRequest;
import org.eclipse.linuxtools.tmf.core.request.ITmfEventRequest.ExecutionType;
import org.eclipse.linuxtools.tmf.core.request.TmfEventRequest;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfEvent;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

/**
 * List TID of tasks in a kernel trace
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
public class ListTaskCommand implements ICommand {

    class InventoryTaskListener implements ITaskListener {

        @Override
        public void stateChange(Ctx ctx, Task task, StateEnum state) {
            if (!fTaskTable.contains(ctx.hostId, task.getTID())) {
                fTaskTable.put(ctx.hostId, task.getTID(), task);
            }
        }

        @Override
        public void stateFlush(Task task) {
        }

    }

    EventHandler fHandler;
    ITaskListener fTaskListener;
    Table<String, Long, Task> fTaskTable;

    class ListTaskModule extends TmfAbstractAnalysisModule {

        @Override
        protected boolean executeAnalysis(final IProgressMonitor monitor) throws TmfAnalysisException {
            final ITmfTrace trace = getTrace();
            TmfEventRequest tmfEventRequest = new TmfEventRequest(ITmfEvent.class, 0, ITmfEventRequest.ALL_DATA, ExecutionType.BACKGROUND) {
                int refresh = 0;
                @Override
                public void handleStarted() {
                    monitor.beginTask("analysis", 100);
                }

                @Override
                public void handleData(ITmfEvent ev) {
                    fHandler.eventHandle((CtfTmfEvent) ev);
                    if (refresh == 0) {
                        long duration = trace.getEndTime().getValue() - trace.getStartTime().getValue();
                        long here = ev.getTimestamp().getValue() - trace.getStartTime().getValue();
                        monitor.worked((int) (100 * here / duration));
                    }
                    refresh = refresh++ % 10000;
                }
            };
            trace.sendRequest(tmfEventRequest);
            try {
                tmfEventRequest.waitForCompletion();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            monitor.done();
            return true;
        }

        @Override
        protected void canceling() {
        }

    }

    @Override
    public void handle(CommandLine opts) {
        List<String> argList = opts.getArgList();
        System.out.println(argList);
        if (argList.isEmpty()) {
            System.out.println("specify a trace");
            return;
        }
        fTaskTable = HashBasedTable.create();
        fTaskListener = new InventoryTaskListener();
        fHandler = new EventHandler();
        fHandler.addListener(fTaskListener);
        TmfExperiment exp = CtfTraceFinder.makeTmfExperiment(Paths.get(argList.get(0)));
        TmfTraceOpenedSignal signal = new TmfTraceOpenedSignal(this, exp, null);
        exp.traceOpened(signal);
        try (IAnalysisModule module = new ListTaskModule()) {
            module.setId(ExecGraphModule.ID);
            module.setTrace(exp);
            TmfModuleRunnerHelper.executeAnalysis(module, new CliProgressMonitor());
        } catch (TmfAnalysisException e) {
            throw new RuntimeException(e);
        }
        exp.dispose();

        /*
         * Print the report
         */
        System.out.println(String.format("%s: %d", "traces", exp.getTraces().length));
        System.out.println(String.format("%s: %d", "hosts", fTaskTable.rowKeySet().size()));

        for (Cell<String, Long, Task> cell: fTaskTable.cellSet()){
            Task val = cell.getValue();
            String hostID = val.getHostID();
            hostID = hostID.substring(1, hostID.length() - 1);
            String entry = String.format("%s %-5d %s", hostID, val.getTID(), val.getComm());
            System.out.println(entry);
        }
    }

    @Override
    public void createOptions(Options options) {
    }

}
