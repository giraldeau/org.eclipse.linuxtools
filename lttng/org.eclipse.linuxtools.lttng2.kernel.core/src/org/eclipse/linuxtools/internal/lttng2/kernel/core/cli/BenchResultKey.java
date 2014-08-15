package org.eclipse.linuxtools.internal.lttng2.kernel.core.cli;

import java.util.Comparator;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * Result key is a POJO class to store benchmark results in a one-level hash
 * map. It aims to replace nested hash maps for each subkeys. In the case of the
 * measurements, there are 4 dimensions:
 *   - the operation (read, processing, etc.)
 *   - the metric (time, mem, etc)
 *   - the input size
 *   - the actual values
 *
 * The values is a collection that is stored into the DescriptiveStatistics object.
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
public class BenchResultKey {

    private static class LexicalOrderComparator implements Comparator<BenchResultKey> {
        @Override
        public int compare(BenchResultKey k1, BenchResultKey k2) {
            int comp = k1.name.compareTo(k2.name);
            if (comp != 0) {
                return comp;
            }
            comp = k1.metric.compareTo(k2.metric);
            if (comp != 0) {
                return comp;
            }
            return k1.size.compareTo(k2.size);
        }
    }

    public static final Comparator<BenchResultKey> CMP_LEXICAL = new LexicalOrderComparator();

    private final static HashFunction hf = Hashing.goodFastHash(32);
    private final String name;
    private final String metric;
    private final Integer size;
    private final DescriptiveStatistics stats;
    private final int hashCode;

    public BenchResultKey(String name, String metric, Integer size) {
        this(name, metric, size, new DescriptiveStatistics());
    }

    public BenchResultKey(String name, String metric, Integer size, DescriptiveStatistics stats) {
        this.name = name; this.metric = metric; this.size = size; this.stats = stats;
        this.hashCode = hf.newHasher()
                .putInt(name.hashCode())
                .putInt(metric.hashCode())
                .putInt(size).hash().asInt();
    }

    public String getName() {
        return this.name;
    }

    public String getMetric() {
        return this.metric;
    }

    public Integer getSize() {
        return this.size;
    }

    public DescriptiveStatistics getStats() {
        return this.stats;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof BenchResultKey) {
            BenchResultKey k = (BenchResultKey) other;
            if (name.equals(k.name) &&
                    metric.equals(k.metric) &&
                    size.equals(k.size)) {
                return true;
            }
        }
        return false;
    }

}