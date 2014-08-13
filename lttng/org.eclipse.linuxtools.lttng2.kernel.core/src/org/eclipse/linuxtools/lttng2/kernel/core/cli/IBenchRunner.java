package org.eclipse.linuxtools.lttng2.kernel.core.cli;

public interface IBenchRunner {

    public void setup(BenchContext ctx);
    public void run(BenchContext ctx);
    public void teardown(BenchContext ctx);

}
