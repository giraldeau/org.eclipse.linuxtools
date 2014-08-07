package org.eclipse.linuxtools.tmf.analysis.graph.core.ctf;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.matching.ExpireCleanupMonitor;
import org.eclipse.linuxtools.tmf.core.event.matching.StopEarlyMonitor;
import org.eclipse.linuxtools.tmf.core.event.matching.TmfNetworkEventMatching;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.synchronization.IFunction;
import org.eclipse.linuxtools.tmf.core.synchronization.ITmfTimestampTransform;
import org.eclipse.linuxtools.tmf.core.synchronization.SyncAlgorithmFullyIncremental;
import org.eclipse.linuxtools.tmf.core.synchronization.SynchronizationAlgorithm;
import org.eclipse.linuxtools.tmf.core.synchronization.TraceShifterOrigin;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.eclipse.linuxtools.tmf.core.trace.TmfTrace;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfTrace;

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
     * @param paths directory of traces
     * @param traceKlass the trace type
     * @param evKlass the event type
     * @return experiment
     */
    public static TmfExperiment makeTmfExperiment(List<Path> paths, Class<? extends ITmfTrace> traceKlass, Class<? extends ITmfEvent> evKlass) {
        final ITmfTrace[] traces = new TmfTrace[paths.size()];
        int i = 0;
        for (Path p: paths) {
            ITmfTrace tmp = null;
            try {
                tmp = traceKlass.newInstance();
                tmp.initTrace(null, p.toString(), evKlass);
            } catch (InstantiationException | IllegalAccessException | TmfTraceException e) {
                throw new RuntimeException(e.getMessage());
            }
            traces[i++] = tmp;
        }

        TmfExperiment experiment = new TmfExperiment();
        experiment.initExperiment(evKlass, "default", traces, Integer.MAX_VALUE, null); //$NON-NLS-1$
        return experiment;
    }

    /**
     * Make experiment from trace directories. The paths is searched recursively
     * for traces.
     *
     * @param paths
     *            the list of paths to search for traces
     * @return the experiment
     */
    public static TmfExperiment makeTmfExperiment(List<Path> paths) {
        List<Path> expanded = findCtfTrace(paths);
        return makeTmfExperiment(expanded, CtfTmfTrace.class, CtfTmfEvent.class);
    }

    /**
     * Make experiment from a trace directory with default trace and event type.
     * The path is searched recursively.
     *
     * @param base
     *            trace directory
     * @return the experiment
     */
    public static TmfExperiment makeTmfExperiment(Path base) {
        List<Path> expanded = findCtfTrace(base);
        return makeTmfExperiment(expanded, CtfTmfTrace.class, CtfTmfEvent.class);
    }

    /**
     * Synchronize experiment
     * @param experiment the experiment to synchronize
     * @return the synchronization algorithm used
     */
    public static void synchronizeExperiment(TmfExperiment experiment) {
        SynchronizationAlgorithm algo = new SyncAlgorithmFullyIncremental();
        TmfNetworkEventMatching matching = new TmfNetworkEventMatching(Collections.singleton(experiment), algo);
        matching.matchEvents();
        applyComposeTransform(algo, experiment);
    }

    public static void synchronizeExperimentWithPreSync(TmfExperiment experiment) {
        synchronizeExperimentCoarse(experiment);
        SynchronizationAlgorithm algo = new SyncAlgorithmFullyIncremental();
        TmfNetworkEventMatching matching = new TmfNetworkEventMatching(Collections.singleton(experiment), algo);
        matching.addMatchMonitor(new ExpireCleanupMonitor());
        matching.matchEvents();
        applyComposeTransform(algo, experiment);
    }

    public static void synchronizeExperimentCoarse(TmfExperiment experiment) {
        IFunction<TmfExperiment> func = new TraceShifterOrigin();
        func.apply(experiment);
        SyncAlgorithmFullyIncremental algo = new SyncAlgorithmFullyIncremental();
        TmfNetworkEventMatching matching = new TmfNetworkEventMatching(Collections.singleton(experiment), algo);
        matching.addMatchMonitor(new StopEarlyMonitor());
        matching.matchEvents();
        applyComposeTransform(algo, experiment);
    }

    public static void applyComposeTransform(SynchronizationAlgorithm algo, TmfExperiment experiment) {
        for (ITmfTrace trace : experiment.getTraces()) {
            ITmfTimestampTransform xform = algo.getTimestampTransform(trace).composeWith(trace.getTimestampTransform());
            trace.setTimestampTransform(xform);
        }
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
            throw new RuntimeException(e.getMessage());
        }
        return finder.getResults();
    }

    public static List<Path> findCtfTrace(List<Path> paths) {
        HashSet<Path> result = new HashSet<>();
        for (Path path: paths) {
            result.addAll(findCtfTrace(path));
        }

        return new ArrayList<>(result);
    }

    /**
     * Find all trace paths below a given directory
     *
     * @param dir parent directory
     * @return ordered list of paths
     */
    public static List<Path> getTracePathsByCreationTime(Path dir) {
        List<Path> paths = new ArrayList<>();
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path item: stream) {
                BasicFileAttributes attrs = Files.readAttributes(item, BasicFileAttributes.class);
                if (attrs.isDirectory()) {
                    paths.add(item);
                }
            }
        } catch (IOException e) { }

        Collections.sort(paths, new Comparator<Path>() {
            @Override
            public int compare(Path p0, Path p1) {
                FileTime t0;
                FileTime t1;
                try {
                    t0 = Files.readAttributes(p0, BasicFileAttributes.class).creationTime();
                    t1 = Files.readAttributes(p1, BasicFileAttributes.class).creationTime();
                    return t0.compareTo(t1);
                } catch (IOException e) {
                }
                return 0;
            }
        });
        return paths;
    }
}