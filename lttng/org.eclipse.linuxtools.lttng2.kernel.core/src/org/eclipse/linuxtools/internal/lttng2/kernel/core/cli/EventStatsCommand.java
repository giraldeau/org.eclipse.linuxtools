package org.eclipse.linuxtools.internal.lttng2.kernel.core.cli;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.eclipse.linuxtools.tmf.analysis.graph.core.ctf.CtfTraceFinder;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventType;
import org.eclipse.linuxtools.tmf.core.request.ITmfEventRequest;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * Abstract base command handler
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
@SuppressWarnings("nls")
public class EventStatsCommand implements ICommand {

    /* FIXME: group common options */
    public static final String OPT_TRACESET = "traceset";

    /* Fight against Java immutable Long */
    class Count {
        long value = 0;
        public void inc() { value++; }
        public void add(long other) { value += other; }
        public long val() { return value; }
    }

    public final static String[][] myGroups = {
        { "inet_sock_local_in",     "net" },
        { "inet_sock_local_out",    "net" },
        { "sched_switch",           "sched" },
        { "sched_ttwu",             "sched" },
        { "sched_process_fork",     "sched" },
        { "sched_process_exec",     "sched" },
        { "sched_process_exit",     "sched" },
        { "softirq_entry",          "irq" },
        { "softirq_exit",           "irq" },
        { "hrtimer_expire_entry",   "irq" },
        { "hrtimer_expire_exit",    "irq" },
        { "irq_handler_entry",      "irq" },
        { "irq_handler_exit",       "irq" },
    };

    FileFilter dirFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {
            return file.isDirectory();
        }
    };

    @Override
    public void handle(CommandLine opts) {

        final Table<String, ITmfEventType, Count> table = HashBasedTable.create();

        System.out.println("stats");
        List<Path> paths = new ArrayList<>();
        if (opts.hasOption("t")) {
            for (Object path: opts.getArgList()) {
                File dir = new File((String) path);
                File[] listFiles = dir.listFiles(dirFilter);
                for(File subTrace : listFiles) {
                    paths.add(Paths.get(subTrace.getAbsolutePath()));
                }
            }
        } else {
            for (Object path: opts.getArgList()) {
                paths.add(Paths.get((String) path));
            }
        }
        System.out.println(paths);
        for (Path p: paths) {
            final TmfExperiment exp = CtfTraceFinder.makeTmfExperiment(p);
            final String name = p.toFile().getName();
            ITmfEventRequest req = new DummyRequest() {
                @Override
                public void handleData(final ITmfEvent event) {
                    if (!table.contains(name, event.getType())) {
                        table.put(name, event.getType(), new Count());
                    }
                    table.get(name, event.getType()).inc();
                }
            };
            exp.sendRequest(req);
            try {
                req.waitForCompletion();
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        File out = new File("/tmp/eventstats.csv");
        try (FileWriter w = new FileWriter(out)) {
            w.write("trace,event,count\n");
            for (String traceName: table.rowKeySet()) {
                Map<ITmfEventType, Count> row = table.row(traceName);
                HashMap<String, Count> stats = group(myGroups, row);
                for (Entry<String, Count> entry : stats.entrySet()) {
                    System.out.println(traceName + "," + entry.getKey() + "," + entry.getValue().val());
                    w.write(String.format("%s,%s,%d\n", traceName, entry.getKey(), entry.getValue().val()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private HashMap<String, Count> group(String[][] eventGroups, Map<ITmfEventType, Count> map) {
        HashMap<String, Count> result = new HashMap<>();
        for (int i = 0; i < eventGroups.length; i++) {
            result.put(eventGroups[i][1], new Count());
        }
        result.put("other", new Count());

        for (Entry<ITmfEventType, Count> entry : map.entrySet()) {
            String evName = entry.getKey().getName();
            String groupName = "other";
            for (int i = 0; i < eventGroups.length; i++) {
                if (evName.compareTo(eventGroups[i][0]) == 0) {
                    groupName = eventGroups[i][1];
                    break;
                }
            }
//            if (groupName.compareTo("other") == 0) {
//                System.out.println(evName);
//            }
            result.get(groupName).add(entry.getValue().val());
        }
        return result;
    }

    @Override
    public void createOptions(Options options) {
        options.addOption("t", OPT_TRACESET, false, "run for each subtraces of the given directory");
    }

}
