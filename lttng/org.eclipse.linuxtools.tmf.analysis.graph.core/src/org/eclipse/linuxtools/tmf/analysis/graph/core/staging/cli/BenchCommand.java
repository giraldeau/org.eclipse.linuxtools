package org.eclipse.linuxtools.tmf.analysis.graph.core.staging.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final String OPT_DRY_RUN = "dry-run";
    private static final String OPT_REPEAT = "repeat";
    private static final String OPT_TRACESET = "traceset";
    private static final String OPT_REGEX = "regex";
    private static final String OPT_REGEX_DEFAULT = "[a-z-A-Z_-]*(\\d+)";

    static Map<String, IBenchRunner> stages = new HashMap<>();
    static {
        stages.put("read", new DoRead());
        stages.put("sync", new DoSync());
        stages.put("presync", new DoSync());
        stages.put("build", new DoRead());
        stages.put("extract", new DoRead());
    }

    @Override
    public void handle(CommandLine opts) {
        BenchContext ctx = new BenchContext();
        BenchResult res = new BenchResult();

        Boolean dryRun = opts.hasOption(OPT_DRY_RUN);
        ctx.put(BenchResult.class, res);
        ctx.put(Integer.class, BenchContext.TAG_REPEAT, Integer.parseInt(opts.getOptionValue(OPT_REPEAT, "10")));
        String regex = opts.getOptionValue(OPT_REGEX, OPT_REGEX_DEFAULT);
        Pattern pattern = Pattern.compile(regex);

        List<Path> basePaths = new ArrayList<>();
        if (opts.hasOption(OPT_TRACESET)) {
            for (String dir: opts.getArgs()) {
                basePaths.addAll(CtfTraceFinder.getTracePathsByCreationTime(Paths.get(dir)));
            }
        } else {
            for (String dir: opts.getArgs()) {
                basePaths.add(Paths.get(dir));
            }
        }

        // run benchmark for each experiment
        for (Path path: basePaths) {
            Integer size = 1;
            if (opts.hasOption(OPT_TRACESET)) {
                String fileName = path.toFile().getName();
                Matcher matcher = pattern.matcher(fileName);
                if (matcher.matches()) {
                    size = Integer.parseInt(matcher.group(1));
                }
            }
            ctx.put(Integer.class, BenchContext.TAG_SIZE, size);
            System.out.println("experiment path (size=" + size + "): " + path);

            if (opts.hasOption(OPT_STAGE)) {
                String opt = opts.getOptionValue(OPT_STAGE);
                if (!opt.contains(opt)) {
                    System.err.println("unkown stage " + opt);
                }
                doOneStage(opt, stages.get(opt), ctx, path, dryRun);
            } else {
                for (String stage: stages.keySet()) {
                    doOneStage(stage, stages.get(stage), ctx, path, dryRun);
                }
            }
        }

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

    private static void doOneStage(String stage, IBenchRunner runner, BenchContext ctx, Path path, boolean dryRun) {
        TmfExperiment experiment = CtfTraceFinder.makeTmfExperiment(path);
        ctx.put(TmfExperiment.class, experiment);
        ctx.put(String.class, BenchContext.TAG_TASK_NAME, stage);
        Integer repeat = ctx.get(Integer.class, BenchContext.TAG_REPEAT);
        if (!dryRun) {
            runner.setup(ctx);
            for (int i = 0; i < repeat; i++) {
                runner.run(ctx);
            }
            runner.teardown(ctx);
        }
        experiment.dispose();
    }

    @Override
    public void createOptions(Options options) {
        options.addOption("o", OPT_OUTPUT, true, "output directory");
        options.addOption("s", OPT_STAGE, true, "stage to execute");
        options.addOption("n", OPT_DRY_RUN, false, "dry run, just display what would be done");
        options.addOption("r", OPT_REPEAT, true, "number of times to repeat (default 10)");
        options.addOption("t", OPT_TRACESET, false, "run for each subtraces of the given directory");
        options.addOption("g", OPT_REGEX, true, "capturing regex for trace size (when traceset is specified)");
    }

}
