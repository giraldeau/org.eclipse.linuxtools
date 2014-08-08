package org.eclipse.linuxtools.tmf.analysis.graph.core.staging.bench;

import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import com.google.common.collect.ArrayListMultimap;

public class BenchResult {

    public static final String METRIC_TIME = "time";
    public static final String METRIC_MEM = "mem";

    HashMap<BenchResultKey, BenchResultKey> results = new HashMap<>();

    private long memStart;
    private long timeStart;
    private boolean recording;
    /**
     * @param ctx
     */
    public void begin(BenchContext ctx) {
        Runtime runtime = Runtime.getRuntime();
        System.gc();
        memStart = runtime.totalMemory() - runtime.freeMemory();
        timeStart = System.currentTimeMillis();
    }

    public void done(BenchContext ctx) {
        Runtime runtime = Runtime.getRuntime();
        long time = System.currentTimeMillis() - timeStart;
        System.gc();
        long mem = (runtime.totalMemory() - runtime.freeMemory()) - memStart;
        String tag = ctx.get(String.class, BenchContext.TAG_TASK_NAME);
        Integer size = ctx.get(Integer.class, BenchContext.TAG_SIZE);
        addDataRaw(tag, BenchResult.METRIC_TIME, size, time);
        addDataRaw(tag, BenchResult.METRIC_MEM, size, mem);
    }

    public void addDataRaw(String name, String metric, Integer size, double value) {
        if (!recording) {
            return;
        }
        BenchResultKey key = new BenchResultKey(name, metric, size);
        if (!results.containsKey(key)) {
            results.put(key, key);
        }
        BenchResultKey item = results.get(key);
        item.getStats().addValue(value);
    }

    public String dumpData() {
        StringBuilder str = new StringBuilder();
        SortedSet<BenchResultKey> sorted = new TreeSet<>(BenchResultKey.CMP_LEXICAL);
        sorted.addAll(results.values());
        for (BenchResultKey item: sorted) {
            DescriptiveStatistics stats = item.getStats();
            for (double value: stats.getValues()) {
                str.append(String.format("%s;%s;%d,%f\n",
                        item.getName(), item.getMetric(), item.getSize(), value));
            }
        }
        return str.toString();
    }

    public String dumpSummary() {
        StringBuilder str = new StringBuilder();
        SortedSet<BenchResultKey> sorted = new TreeSet<>(BenchResultKey.CMP_LEXICAL);
        sorted.addAll(results.values());
        ArrayListMultimap<String, BenchResultKey> byMetric = ArrayListMultimap.create();
        for (BenchResultKey item: sorted) {
            byMetric.put(item.getMetric(), item);
        }
        for (String metric: byMetric.keySet()) {
            str.append(String.format("summary %s\n", metric));
            for (BenchResultKey entry: byMetric.get(metric)) {
                DescriptiveStatistics stats = entry.getStats();
                str.append(String.format("%-8s %6d %6d %#12.2f %#12.2f\n",
                        entry.getName(), entry.getSize(), stats.getN(), stats.getMean(), stats.getStandardDeviation()));
            }
        }
        return str.toString();
    }

    public void setRecording(boolean recording) {
        this.recording = recording;
    }

}
