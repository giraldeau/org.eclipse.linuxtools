package org.eclipse.linuxtools.lttng2.kernel.core.tests.graph;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.linuxtools.lttng2.kernel.core.trace.LttngKernelTrace;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.synchronization.ITmfTimestampTransform;
import org.eclipse.linuxtools.tmf.core.synchronization.SyncAlgorithmFullyIncremental;
import org.eclipse.linuxtools.tmf.core.synchronization.SynchronizationAlgorithm;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.eclipse.linuxtools.tmf.core.trace.TmfTrace;

/**
 * Find traces under directory
 *
 * @author Francis Giraldeau  <francis.giraldeau@gmail.com>
 */
public class CtfTraceFinder extends SimpleFileVisitor<Path> {
    private final PathMatcher matcher;
    private List<Path> results;
    CtfTraceFinder() {
        matcher = FileSystems.getDefault().getPathMatcher("glob:metadata");
        results = new LinkedList<>();
    }
    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
        Path name = path.getFileName();
        if (name != null && matcher.matches(name)) {
            results.add(path.getParent());
        }
        return FileVisitResult.CONTINUE;
    }

    /**
     * Return results
     * @return results
     */
    public List<Path> getResults() {
        return results;
    }

    /**
     * Make an experiment of CTF traces
     * @param base base directory of traces
     * @param traceKlass the trace type
     * @param evKlass the event type
     * @return experiment
     */
    public static TmfExperiment makeTmfExperiment(Path base, Class<? extends ITmfTrace> traceKlass, Class<? extends ITmfEvent> evKlass) {
        List<Path> paths = findCtfTrace(base);
        final ITmfTrace[] traces = new TmfTrace[paths.size()];
        int i = 0;
        for (Path p: paths) {
            ITmfTrace tmp = null;
            try {
                tmp = traceKlass.newInstance();
                tmp.initTrace(null, p.toString(), evKlass);
            } catch (InstantiationException | IllegalAccessException | TmfTraceException e) {
                fail(e.getMessage());
            }
            traces[i++] = tmp;
        }

        TmfExperiment experiment = new TmfExperiment();
        experiment.initExperiment(evKlass, "", traces, Integer.MAX_VALUE, null);
        SynchronizationAlgorithm algo = new SyncAlgorithmFullyIncremental();
        try {
            algo = experiment.synchronizeTraces(true, algo);
        } catch (TmfTraceException e) {
            e.printStackTrace();
        }
        for (ITmfTrace trace: experiment.getTraces()) {
            ITmfTimestampTransform tstrans = algo.getTimestampTransform(trace.getHostId());
            System.out.println(tstrans);
            trace.setTimestampTransform(tstrans);
        }
        return experiment;
    }

    /**
     * Make experiment from a trace directory with default trace and event type
     * @param base trace directory
     * @return the experiment
     */
    public static TmfExperiment makeTmfExperiment(Path base) {
        return makeTmfExperiment(base, LttngKernelTrace.class, ITmfEvent.class);
    }

    /**
     * Utility method to walk directories and find CTF traces
     *
     * @param base base directory
     * @return list of trace paths
     */
    public static List<Path> findCtfTrace(Path base) {
        CtfTraceFinder finder = new CtfTraceFinder();
        try {
            Files.walkFileTree(base, finder);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        return finder.getResults();

    }
}