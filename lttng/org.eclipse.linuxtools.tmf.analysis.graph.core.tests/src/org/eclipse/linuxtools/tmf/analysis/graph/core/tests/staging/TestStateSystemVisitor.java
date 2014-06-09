package org.eclipse.linuxtools.tmf.analysis.graph.core.tests.staging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.linuxtools.statesystem.core.ITmfStateSystem;
import org.eclipse.linuxtools.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.linuxtools.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.linuxtools.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.linuxtools.tmf.analysis.graph.core.ctf.CtfTraceFinder;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.ExecGraphModule;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Task;
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

    @Test
    public void testMakeExecGraphSS() throws AttributeNotFoundException, StateSystemDisposedException {
        Task task = new Task("1ac2304e-a4ce-4519-a289-067db4f955ec", 22014, 0);
        int quark = ss.getQuarkAbsolute(task.getHostID(), "task", task.getTID().toString(), "state");
        List<ITmfStateInterval> range = ss.queryHistoryRange(quark, ss.getStartTime(), ss.getCurrentEndTime());
        assertTrue(range.size() > 0);

        CountIntervalVisitor visitor = new CountIntervalVisitor();
        SeqIntervalTraverse traverse = new SeqIntervalTraverse();
        traverse.traverse(ss, task, ss.getStartTime(), ss.getCurrentEndTime(), visitor);
        assertEquals(range.size(), visitor.getCount());
    }

}
