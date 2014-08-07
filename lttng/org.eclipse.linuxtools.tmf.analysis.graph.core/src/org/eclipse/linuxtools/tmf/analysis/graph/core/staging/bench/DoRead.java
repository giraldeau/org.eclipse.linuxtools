package org.eclipse.linuxtools.tmf.analysis.graph.core.staging.bench;


public class DoRead implements IBenchRunner {

    @Override
    public void setup(BenchContext ctx) {
    }

    @Override
    public void run(BenchContext ctx) {
        BenchResult res = ctx.get(BenchResult.class);
        res.begin();
        res.done(ctx.get(String.class, BenchContext.TAG_TASK_NAME), 42);
    }

    @Override
    public void teardown(BenchContext ctx) {
    }

}
