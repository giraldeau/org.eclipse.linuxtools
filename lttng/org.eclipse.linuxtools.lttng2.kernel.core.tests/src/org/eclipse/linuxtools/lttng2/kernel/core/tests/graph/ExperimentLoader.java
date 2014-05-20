package org.eclipse.linuxtools.lttng2.kernel.core.tests.graph;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfTrace;

public class ExperimentLoader {

    public static void process(Path dir, ExperimentProcessor processor) {
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

        for (Path item: paths.subList(0, 3)) {
            processor.before(item);
            TmfExperiment exp = CtfTraceFinder.makeTmfExperiment(item, CtfTmfTrace.class, CtfTmfEvent.class);
            processor.core(exp);
            processor.after(exp);
        }
    }

}
