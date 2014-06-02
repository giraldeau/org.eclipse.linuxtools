package org.eclipse.linuxtools.lttng2.kernel.core.tests.graph;

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.linuxtools.lttng2.kernel.core.graph.sht.ExecGraphModule;
import org.eclipse.linuxtools.lttng2.kernel.core.graph.sht.Machine;
import org.eclipse.linuxtools.statesystem.core.ITmfStateSystem;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.linuxtools.tmf.core.tests.shared.TmfTestHelper;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.junit.After;
import org.junit.Test;

public class TestExecGraphStateSystem {

    @Test
    public void testMachineState() {
        Machine m = new Machine();
        int cpu1 = 1;
        m.update(cpu1);
        assertEquals(cpu1 + 1, m.current.size());
        for (int i = 0; i < m.current.size(); i++) {
            m.current.set(i, 0L);
        }
        int cpu2 = 3;
        m.update(cpu2);
        assertEquals(cpu2 + 1, m.current.size());
        for (int i = 0; i <= cpu1; i++) {
            assertEquals(0L, (long) m.current.get(i));
        }
        for (int i = cpu1 + 1; i <= cpu2; i++) {
            assertEquals(-1L, (long) m.current.get(i));
        }
        System.out.println(m.current);
    }

    @After
    public void cleanup() {
        Path ht = Paths.get(String.format("/tmp/null/%s.ht", ExecGraphModule.ID));
        ht.toFile().delete();
    }

    @Test
    public void testMakeExecGraphSS() throws Throwable {
        Path path = Paths.get(TraceStrings.TRACE_DIR, TraceStrings.EXP_DJANGO_INDEX);
        TmfExperiment experiment = CtfTraceFinder.makeSynchronizedTmfExperiment(path);
        TmfTraceOpenedSignal signal = new TmfTraceOpenedSignal(this, experiment, null);
        experiment.traceOpened(signal);

        ExecGraphModule module = new ExecGraphModule();
        module.setId(ExecGraphModule.ID);
        module.setTrace(experiment);
        TmfTestHelper.executeAnalysis(module);
        ITmfStateSystem stateSystem = module.getStateSystem();
        System.out.println("size: " + stateSystem.getNbAttributes());
    }

}
