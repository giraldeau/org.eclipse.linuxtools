package org.eclipse.linuxtools.tmf.analysis.graph.core.staging.bench;

public interface IBenchRunner {

    public void setup(BenchContext ctx);
    public void run(BenchContext ctx);
    public void teardown(BenchContext ctx);

}
