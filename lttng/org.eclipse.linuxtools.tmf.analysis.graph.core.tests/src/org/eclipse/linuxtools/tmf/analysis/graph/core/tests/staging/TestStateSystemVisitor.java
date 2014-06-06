package org.eclipse.linuxtools.tmf.analysis.graph.core.tests.staging;

import static org.junit.Assert.assertNotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.linuxtools.statesystem.core.ITmfStateSystem;
import org.eclipse.linuxtools.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.linuxtools.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.linuxtools.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.linuxtools.tmf.analysis.graph.core.ctf.CtfTraceFinder;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.ExecGraphModule;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.IntervalTraverse;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.IntervalVisitor;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.linuxtools.tmf.core.tests.shared.TmfTestHelper;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestStateSystemVisitor {

    private ITmfStateSystem ss;

    @Before
    public void setup() throws Throwable {
        Path path = Paths.get(TraceStrings.TRACE_DIR, TraceStrings.EXP_DJANGO_INDEX);
        TmfExperiment experiment = CtfTraceFinder.makeSynchronizedTmfExperiment(path);
        TmfTraceOpenedSignal signal = new TmfTraceOpenedSignal(this, experiment, null);
        experiment.traceOpened(signal);

        ExecGraphModule module = new ExecGraphModule();
        module.setId(ExecGraphModule.ID);
        module.setTrace(experiment);
        TmfTestHelper.executeAnalysis(module);
        ss = module.getStateSystem();
        assertNotNull(ss);
    }

    @After
    public void cleanup() {
        Path ht = Paths.get(String.format("/tmp/null/%s.ht", ExecGraphModule.ID));
        ht.toFile().delete();
    }

    public class SimpleIntervalTraverse implements IntervalTraverse {

        @Override
        public void traverse(ITmfStateSystem s, int quark, long start, long end, IntervalVisitor visitor) {
            try {
                doTraverse(s, quark, start, end, visitor);
            } catch (AttributeNotFoundException | StateSystemDisposedException e) {
                e.printStackTrace();
            }
        }

        private void doTraverse(ITmfStateSystem s, int quark, long start, long end, IntervalVisitor visitor) throws AttributeNotFoundException, StateSystemDisposedException {
            List<ITmfStateInterval> hist = s.queryHistoryRange(quark, start, end);
            for (ITmfStateInterval i: hist) {
                visitor.visit(i);
            }
        }

    }

    public class DumpIntervalVisitor implements IntervalVisitor {

        @Override
        public void visit(ITmfStateInterval interval) {
            System.out.println(interval);
        }

    }

    @Test
    public void testMakeExecGraphSS() throws AttributeNotFoundException, StateSystemDisposedException {
        int quark = ss.getQuarkAbsolute("1ac2304e-a4ce-4519-a289-067db4f955ec", "task", "22014", "state"); // OK
        ss.queryHistoryRange(quark, 0, Long.MAX_VALUE); // BOOM
    }

}
