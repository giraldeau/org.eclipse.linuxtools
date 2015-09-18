package org.eclipse.linuxtools.tmf.analysis.graph.core.tests.staging;

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
import org.eclipse.linuxtools.tmf.analysis.graph.core.ctf.CtfTraceFinder;
import org.eclipse.linuxtools.tmf.analysis.graph.core.tests.staging.GraphBenchmark.Data;
import org.eclipse.linuxtools.tmf.analysis.graph.core.tests.staging.GraphBenchmark.Func;
import org.eclipse.linuxtools.tmf.analysis.graph.core.tests.staging.GraphBenchmark.Run;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.request.ITmfEventRequest;
import org.eclipse.linuxtools.tmf.core.request.ITmfEventRequest.ExecutionType;
import org.eclipse.linuxtools.tmf.core.request.TmfEventRequest;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfTrace;
import org.junit.Test;

public class ProfileTMF {

    private static final String TRACE_DIR = "traces/django-benchmark/";

    int tmfCount = 0;

    public Data readCtfReader(final File tracePath, final boolean parse) {
        Data data = Run.go(new Func() {
            @Override
            public void func() {
                tmfCount = 0;
                int noCtx = 0;
                try (CTFTrace trace = new CTFTrace(tracePath);
                        CTFTraceReader reader = new CTFTraceReader(trace)) {
                    while(reader.hasMoreEvents()) {
                        EventDefinition event = reader.getCurrentEventDef();
                        if (event == null) {
                            throw new RuntimeException("null event");
                        }
                        if (parse) {
                            event.getFields().getDefinition("");
                            if (event.getEventContext() == null || event.getFields() == null) {
                                noCtx++;
                            }
                        }
                        reader.advance();
                        tmfCount++;
                    }
                } catch (CTFReaderException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("read raw count: " + tmfCount + " (" + noCtx + ")");
            }
        });
        data.count = tmfCount;
        return data;
    }

    public Data readTmfFg(final File tracePath, final boolean parse) {
        return readTmf(tracePath, parse, ITmfEventRequest.ExecutionType.FOREGROUND);
    }

    public Data readTmfBg(final File tracePath, final boolean parse) {
        return readTmf(tracePath, parse, ITmfEventRequest.ExecutionType.BACKGROUND);
    }

    public Data readTmf(final File tracePath, final boolean parse, final ExecutionType execType) {
        try (final CtfTmfTrace trace = new CtfTmfTrace()) {
            trace.initTrace(null, tracePath.getAbsolutePath(), CtfTmfEvent.class);
            Data data = Run.go(new Func() {
                @Override
                public void func() {
                    tmfCount = 0;
                    TmfEventRequest rq =  new TmfEventRequest(ITmfEvent.class,
                            TmfTimeRange.ETERNITY, 0L, ITmfEventRequest.ALL_DATA,
                            execType) {
                        @Override
                        public void handleData(final ITmfEvent event) {
                            tmfCount++;
                            if (parse) {
                                if (event.getContent() == null) {
                                    event.getContent().getFields().size();
                                    throw new RuntimeException("null ctx");
                                }
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

    public Data readBabel(final File tracePath) {
        Data data = Run.go(new Func() {
            @Override
            public void func() {
                Process p;
                try {
                    p = new ProcessBuilder("babeltrace", "-o", "dummy", tracePath.getAbsolutePath()).start();
                    p.waitFor();
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException("process error: " + e.getMessage());
                }
            }
        });
        return data;
    }

    @Test
    public void testBabel() {
        Data d = readBabel(new File("traces/django-index/django-httpd"));
        System.out.println("time: " + d.time);
    }

    @Test
    public void testCompareCtfReaders() throws IOException {
        String fmt = "%s;%s;%d;%d\n";
        List<Path> path = CtfTraceFinder.getTracePathsByCreationTime(Paths.get(TRACE_DIR));
        try (FileWriter out = new FileWriter(new File("read-trace.data"))) {
            out.write(String.format("%s;%s;%s;%s\n", "type", "parse", "count", "time"));
            for (Path p: path) {
                String dir = p.toFile().getCanonicalPath() + "/django-httpd";
                File in = CtfTraceFinder.findCtfTrace(Paths.get(dir)).get(0).toFile();
                int repeat = 10;
                for (int i = 0; i < repeat; i++) {
                    for (Boolean parse: new boolean[] {Boolean.TRUE, Boolean.FALSE}) {
                        Data rawctf = readCtfReader(in, parse);
                        Data tmfBg = readTmf(in, parse, ITmfEventRequest.ExecutionType.BACKGROUND);
                        Data tmfFg = readTmf(in, parse, ITmfEventRequest.ExecutionType.FOREGROUND);
                        out.write(String.format(fmt, "ctf", parse, rawctf.count, rawctf.time));
                        out.write(String.format(fmt, "tmf-fg", parse, tmfFg.count, tmfFg.time));
                        out.write(String.format(fmt, "tmf-bg", parse, tmfBg.count, tmfBg.time));

                        // babeltrace always parse fields
                        // using previous count, because babeltrace does not report the number of events
                        if (parse) {
                            Data babel = readBabel(in);
                            out.write(String.format(fmt, "babel", parse, rawctf.count, babel.time));
                        }
                        out.flush();
                    }
                }
            }
        }
    }

}
