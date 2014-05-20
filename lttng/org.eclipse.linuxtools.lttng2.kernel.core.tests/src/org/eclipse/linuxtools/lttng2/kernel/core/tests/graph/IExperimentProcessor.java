package org.eclipse.linuxtools.lttng2.kernel.core.tests.graph;

import java.nio.file.Path;

import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;

public interface IExperimentProcessor {

    public void before(Path experiment);
    public void core(TmfExperiment experiment);
    public void after(TmfExperiment experiment);
}
