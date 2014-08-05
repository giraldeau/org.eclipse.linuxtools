package org.eclipse.linuxtools.tmf.analysis.graph.core.staging.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.eclipse.linuxtools.tmf.analysis.graph.core.ctf.CtfTraceFinder;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.bench.BenchContext;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.bench.BenchResult;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.bench.DoRead;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.bench.DoSync;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.bench.IBenchRunner;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;

/**
 * Command to run execution path command and benchmark
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
@SuppressWarnings("nls")
public class BenchCommand extends BaseCommand {

    private static final String OPT_OUTPUT = "output";
    private static final String OPT_STAGE = "stage";
    private static final String OPT_REPEAT = "repeat";

    static Map<String, IBenchRunner> stages = new HashMap<>();
    static {
        stages.put("read", new DoRead());
        stages.put("sync", new DoSync());
        stages.put("build", new DoRead());
        stages.put("extract", new DoRead());
    }

    @Override
    public void handle(CommandLine opts) {
        BenchContext ctx = new BenchContext();
        BenchResult res = new BenchResult();

        ctx.put(BenchResult.class, res);
        ctx.put(Integer.class, BenchContext.TAG_REPEAT, Integer.parseInt(opts.getOptionValue(OPT_REPEAT, "10")));

        ArrayList<Path> tracePaths = new ArrayList<>();
        for (String dir: opts.getArgs()) {
            tracePaths.add(Paths.get(dir));
        }
        TmfExperiment experiment = CtfTraceFinder.makeTmfExperiment(tracePaths);
        ctx.put(TmfExperiment.class, experiment);

        if (opts.hasOption(OPT_STAGE)) {
            String opt = opts.getOptionValue(OPT_STAGE);
            if (!opt.contains(opt)) {
                System.err.println("unkown stage " + opt);
            }
            doOneStage(opt, stages.get(opt), ctx);
        } else {
            for (String stage: stages.keySet()) {
                doOneStage(stage, stages.get(stage), ctx);
            }
        }
        experiment.dispose();
        System.out.println(res.dumpData());
        System.out.println(res.dumpSummary());
        Path outDir = Paths.get("results");
        if (opts.hasOption(OPT_OUTPUT)) {
            outDir = Paths.get(opts.getOptionValue(OPT_OUTPUT));
        }
        if (!outDir.toFile().exists()) {
            outDir.toFile().mkdirs();
        }
        BenchResult.save(outDir.resolve("data"), res.dumpData());
        BenchResult.save(outDir.resolve("summary"), res.dumpSummary());
    }

    private static void doOneStage(String stage, IBenchRunner runner, BenchContext ctx) {
        ctx.put(String.class, BenchContext.TAG_TASK_NAME, stage);
        Integer repeat = ctx.get(Integer.class, BenchContext.TAG_REPEAT);
        for (int i = 0; i < repeat; i++) {
            runner.run(ctx);
        }
    }

    @Override
    public void createOptions(Options options) {
        options.addOption("o", OPT_OUTPUT, true, "output directory");
        options.addOption("s", OPT_STAGE, true, "stage to execute");
        options.addOption("n", OPT_REPEAT, true, "number of times to repeat (default 10)");
    }

}
