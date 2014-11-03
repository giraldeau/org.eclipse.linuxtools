package org.eclipse.linuxtools.internal.lttng2.kernel.core.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.eclipse.linuxtools.tmf.analysis.graph.core.ctf.CtfTraceFinder;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventType;
import org.eclipse.linuxtools.tmf.core.request.ITmfEventRequest;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

/**
 * Abstract base command handler
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
public class EventStatsCommand implements ICommand {

    /* FIXME: group common options */
    public static final String OPT_TRACESET = "traceset";

    /* Fight against Java immutable Long */
    class Count {
        long value = 0;
        public void inc() { value++; }
        public long val() { return value; }
    }

    @Override
    public void handle(CommandLine opts) {

        final Table<ITmfTrace, ITmfEventType, Count> table = HashBasedTable.create();

        System.out.println("stats");
        List<Path> paths = new ArrayList<>();
        for (Object path: opts.getArgList()) {
            paths.add(Paths.get((String) path));
        }
        System.out.println(paths);
        for (Path p: paths) {
            final TmfExperiment exp = CtfTraceFinder.makeTmfExperiment(p);
            ITmfEventRequest req = new DummyRequest() {
                @Override
                public void handleData(final ITmfEvent event) {
                    if (!table.contains(exp, event.getType())) {
                        table.put(exp, event.getType(), new Count());
                    }
                    table.get(exp, event.getType()).inc();
                }
            };
            exp.sendRequest(req);
            try {
                req.waitForCompletion();
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
            }

            for (Cell<ITmfTrace, ITmfEventType, Count> cell: table.cellSet()){
                System.out.println(cell.getRowKey().getName() + " " + cell.getColumnKey().getName() + " " + cell.getValue().val());
            }
        }
    }

    @Override
    public void createOptions(Options options) {
        options.addOption("t", OPT_TRACESET, false, "run for each subtraces of the given directory");
    }

}
