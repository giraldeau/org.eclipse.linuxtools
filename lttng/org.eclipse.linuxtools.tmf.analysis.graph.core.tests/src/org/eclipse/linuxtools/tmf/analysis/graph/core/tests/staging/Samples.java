package org.eclipse.linuxtools.tmf.analysis.graph.core.tests.staging;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class Samples<K, V> {

    private Table<K, String, V> samples; // (size, type, value)

    public Samples() {
        samples = HashBasedTable.create();
    }

    public void addSample(String col, K key, V value) {
        samples.put(key, col, value);
    }

    public void save(Path out) {
        Charset cs = Charset.forName("UTF-8");
        try  (BufferedWriter w = Files.newBufferedWriter(out, cs,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            w.write("size;");
            ArrayList<String> columns = new ArrayList<>(samples.columnKeySet());
            for (String s: columns) {
                w.write(s + ";");
            }
            w.write("\n");
            for (K key: samples.rowMap().keySet()) {
                w.write(key + ";");
                for (String col: columns) {
                    w.write(samples.get(key, col) + ";");
                }
                w.write("\n");
            }
        } catch (IOException e) {
            System.err.println("Failed to save samples to " + out);
        }
    }

}
