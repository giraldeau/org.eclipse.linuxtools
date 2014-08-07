package org.eclipse.linuxtools.tmf.analysis.graph.core.staging.bench;


public class DoSleep implements IBenchRunner {

    @Override
    public void setup(BenchContext ctx) {
    }

    @Override
    public void run(BenchContext ctx) {
        BenchResult res = ctx.get(BenchResult.class);
        res.begin(ctx);
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        res.done(ctx);
    }

    @Override
    public void teardown(BenchContext ctx) {
    }

}
