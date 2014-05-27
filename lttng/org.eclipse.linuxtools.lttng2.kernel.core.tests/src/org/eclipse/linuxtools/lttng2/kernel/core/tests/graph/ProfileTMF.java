package org.eclipse.linuxtools.lttng2.kernel.core.tests.graph;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.linuxtools.ctf.core.event.EventDefinition;
import org.eclipse.linuxtools.ctf.core.trace.CTFReaderException;
import org.eclipse.linuxtools.ctf.core.trace.CTFTrace;
import org.eclipse.linuxtools.ctf.core.trace.CTFTraceReader;
import org.eclipse.linuxtools.lttng2.kernel.core.tests.graph.GraphBenchmark.Data;
import org.eclipse.linuxtools.lttng2.kernel.core.tests.graph.GraphBenchmark.Func;
import org.eclipse.linuxtools.lttng2.kernel.core.tests.graph.GraphBenchmark.Run;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.request.ITmfEventRequest;
import org.eclipse.linuxtools.tmf.core.request.TmfEventRequest;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfTrace;
import org.junit.Test;

public class ProfileTMF {

    private static final String TRACE_DIR = "traces/django-benchmark/";

    int tmfCount = 0;

    private Data readRaw(final File tracePath) {
        Data data = Run.go(new Func() {
            @Override
            public void func() {
                tmfCount = 0;
                try (CTFTrace trace = new CTFTrace(tracePath);
                        CTFTraceReader reader = new CTFTraceReader(trace)) {
                    while(reader.hasMoreEvents()) {
                        EventDefinition event = reader.getCurrentEventDef();
                        if (event == null || event.getFields().getDeclaration() == null) {
                            throw new RuntimeException("null event");
                        }
                        reader.advance();
                        tmfCount++;
                    }
                } catch (CTFReaderException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("read raw count: " + tmfCount);
            }
        });
        data.count = tmfCount;
        return data;
    }

    @SuppressWarnings("all")
    private Data readTmf(final File tracePath) {
        try (final CtfTmfTrace trace = new CtfTmfTrace()) {
            trace.initTrace(null, tracePath.getAbsolutePath(), CtfTmfEvent.class);
            Data data = Run.go(new Func() {
                @Override
                public void func() {
                    tmfCount = 0;
                    TmfEventRequest rq =  new TmfEventRequest(ITmfEvent.class,
                            TmfTimeRange.ETERNITY, 0L, ITmfEventRequest.ALL_DATA,
                            ITmfEventRequest.ExecutionType.BACKGROUND) {
                        @Override
                        public void handleData(final ITmfEvent event) {
                            if (event != null) {
                                tmfCount++;
                            }
                        }
                        @Override
                        public void handleCompleted() {
                            System.out.println("read tmf count: " + tmfCount);
                        }
                    };
                    trace.sendRequest(rq);
                    try {
                        rq.waitForCompletion();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        assertTrue(false);
                    }
                }
            });
            data.count = tmfCount;
            return data;
        } catch (TmfTraceException e1) {
            throw new RuntimeException(e1);
        }
    }

    @Test
    public void testCompareCtfReaderToTMF() throws IOException {
        List<Path> path = CtfTraceFinder.getTracePathsByCreationTime(Paths.get(TRACE_DIR));
        try (FileWriter out = new FileWriter(new File("read-trace.data"))) {
            for (Path p: path) {
                String dir = p.toFile().getCanonicalPath() + "/django-httpd";
                File in = CtfTraceFinder.findCtfTrace(Paths.get(dir)).get(0).toFile();
                int repeat = 10;
                for (int i = 0; i < repeat; i++) {
                    Data raw = readRaw(in);
                    Data tmf = readTmf(in);
                    out.write(String.format("%s;%d;%d\n", "raw", raw.count, raw.time));
                    out.write(String.format("%s;%d;%d\n", "tmf", tmf.count, tmf.time));
                    out.flush();
                }
            }
        }
    }

}
