package org.eclipse.linuxtools.tmf.analysis.graph.core.staging.bench;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map.Entry;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

public class BenchResult {

    public static final String METRIC_TIME = "time";
    public static final String METRIC_MEM = "mem";

    Table<String, String, DescriptiveStatistics> tbl = HashBasedTable.create();
    private long memStart;
    private long timeStart;

    public void begin() {
        Runtime runtime = Runtime.getRuntime();
        for (int i = 0; i < 10; i++) {
            System.gc();
            runtime.totalMemory();
            runtime.freeMemory();
        }
        memStart = runtime.totalMemory() - runtime.freeMemory();
        timeStart = System.currentTimeMillis();
    }

    public void done(String tag) {
        Runtime runtime = Runtime.getRuntime();
        long time = System.currentTimeMillis() - timeStart;
        System.gc();
        long mem = (runtime.totalMemory() - runtime.freeMemory()) - memStart;
        addDataRaw(tag, BenchResult.METRIC_TIME, time);
        addDataRaw(tag, BenchResult.METRIC_MEM, mem);
    }

    public void addDataRaw(String row, String col, double value) {
        if (!tbl.contains(row, col)) {
            tbl.put(row, col, new DescriptiveStatistics());
        }
        DescriptiveStatistics stats = tbl.get(row, col);
        stats.addValue(value);
    }

    public String dumpData() {
        StringBuilder str = new StringBuilder();
        for (Cell<String, String, DescriptiveStatistics> cell : tbl.cellSet()) {
            DescriptiveStatistics stats = cell.getValue();
            for (double value: stats.getValues()) {
                str.append(String.format("%s;%s;%f\n",
                        cell.getRowKey(), cell.getColumnKey(), value));
            }
        }
        return str.toString();
    }

    public String dumpSummary() {
        StringBuilder str = new StringBuilder();
        for (String col: tbl.columnKeySet()) {
            str.append(String.format("summary %s\n", col));
            for (Entry<String, DescriptiveStatistics> entry: tbl.column(col).entrySet()) {
                DescriptiveStatistics stats = entry.getValue();
                str.append(String.format("%-8s %6d %#12.2f %#12.2f\n",
                        entry.getKey(), stats.getN(), stats.getMean(), stats.getStandardDeviation()));
            }
        }
        return str.toString();
    }

    public static void save(Path path, String str) {
        System.out.println("saving " + path.toAbsolutePath());
        try (FileWriter w = new FileWriter(path.toFile())) {
            w.append(str);
        } catch (IOException e) {
            System.err.println("error saving data");
        }
    }

}
