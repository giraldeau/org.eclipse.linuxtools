package org.eclipse.linuxtools.internal.lttng2.kernel.core.cli;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.eclipse.linuxtools.tmf.analysis.graph.core.ctf.CtfTraceFinder;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;

/**
 * Command to run execution path command and benchmark
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
@SuppressWarnings("nls")
public class BenchCommand extends BaseCommand {

    public static final String OPT_OUTPUT = "output";
    public static final String OPT_OUTPUT_DEFAULT = "results";
    public static final String OPT_STAGE = "stage";
    public static final String OPT_DRY_RUN = "dry-run";
    public static final String OPT_REPEAT = "repeat";
    public static final String OPT_TRACESET = "traceset";
    public static final String OPT_REGEX = "regex";
    public static final String OPT_REGEX_DEFAULT = "[a-z-A-Z_-]*(\\d+)";

    static Map<String, IBenchRunner> stages = new HashMap<>();
    static {
        stages.put("sleep", new DoSleep());
        stages.put("info", new DoInfo());
        stages.put("read", new DoRead());
        stages.put("readfast", new DoReadFast());
        stages.put("sync", new DoSync());
        stages.put("sync-worst", new DoSyncWorst());
        stages.put("sync-optimized", new DoSyncOptimized());
        stages.put("sync-optimized-rawreader", new DoSyncOptimizedRawReader());
        stages.put("build", new DoBuild());
        stages.put("extract", new DoExtract());
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
                String[] optList = opt.split(",");

                for (String stageName: optList) {
                    if (!stages.containsKey(stageName)) {
                        System.err.println("unkown stage " + stageName);
                    }
                    doOneStage(stageName, stages.get(stageName), ctx, path, dryRun);
                }
            } else {
                for (String stage: stages.keySet()) {
                    doOneStage(stage, stages.get(stage), ctx, path, dryRun);
                }
            }
        }

        System.out.println(res.dumpData());
        System.out.println(res.dumpSummary());
        Path outDir = Paths.get(opts.getOptionValue(OPT_OUTPUT, OPT_OUTPUT_DEFAULT));
        if (!outDir.toFile().exists()) {
            outDir.toFile().mkdirs();
        }
        Date now = new Date();
        saveResults(outDir, "data", now, res.dumpData());
        saveResults(outDir, "summary", now, res.dumpSummary());
    }

    private static void saveResults(Path outDir, String prefix, Date date, String str) {
        Path link = outDir.resolve(prefix + "-latest");
        Path data = outDir.resolve(prefix + "-" + date.getTime());

        // write content
        try (FileWriter w = new FileWriter(data.toFile())) {
            w.append(str);
        } catch (IOException e) {
            System.err.println("error saving data to file " + data);
        }

        // symlink
        try {
            if (link.toFile().exists()) {
                link.toFile().delete();
            }
            System.out.println(link + " -> " + data.toFile().getName());
            Files.createSymbolicLink(link, Paths.get(data.toFile().getName()));
        } catch (IOException e) {
            System.err.println("error creating symlink " + link);
        }
    }

    private static void doOneStage(String stage, IBenchRunner runner, BenchContext ctx, Path path, boolean dryRun) {
        TmfExperiment experiment = CtfTraceFinder.makeTmfExperiment(path);
        TmfTraceOpenedSignal signal = new TmfTraceOpenedSignal(new Object(), experiment, null);
        experiment.traceOpened(signal);
        System.err.println("begin stage: " + stage + " experiment: " +  path.toFile().getName());
        ctx.put(TmfExperiment.class, experiment);
        ctx.put(String.class, BenchContext.TAG_TASK_NAME, stage);
        Integer repeat = ctx.get(Integer.class, BenchContext.TAG_REPEAT);
        ctx.get(BenchResult.class).setRecording(true);
        if (!dryRun) {
            System.err.print("setup...\r");
            runner.setup(ctx);
            System.err.print("warm up...\r");
            ctx.get(BenchResult.class).setRecording(false);
            runner.run(ctx);
            runner.run(ctx);
            ctx.get(BenchResult.class).setRecording(true);
            for (int i = 0; i < repeat; i++) {
                System.err.print(String.format("run %d/%d...\r", i + 1, repeat));
                runner.run(ctx);
            }
            System.err.print("teardown...\r");
            runner.teardown(ctx);
        }
        experiment.dispose();
        System.out.println("\nstage done");
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
