package org.eclipse.linuxtools.internal.lttng2.kernel.core.cli;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 * Command to run execution path command and benchmark
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
public class TransformCommand extends BaseCommand {

    private static final MathContext fMc = MathContext.DECIMAL128;

    @Override
    public void handle(CommandLine opts) {
        int n = 10;
        double[] alpha = new double[n];
        for (int i = 0; i < alpha.length; i++) {
            alpha[i] = 0.9 + 0.02 * i;
        }
        long t0 = 1789456123789456123L;
        long t1 = t0 + 1000000000L;
        long defScale = 1000000000L;
        int defShift = 30;

        printHeader("varying alpha");
        for (int i = 0; i < alpha.length; i++) {
            printResult(alpha[i], t0, t1, defScale, defShift);
        }

        printHeader("varying ts");
        for (int i = 0; i < 10; i++) {
            printResult(Math.PI, t0, t1 + i, defScale, defShift);
        }

        printHeader("varying scale");
        for (int i = 0; i < 12; i++) {
            printResult(Math.PI, t0, t1, (long) Math.pow(10, i), defShift);
        }

        printHeader("varying shift");
        for (int i = 0; i < 32; i++) {
            printResult(Math.PI, t0, t1, defScale, i);
        }

        StringBuilder str = new StringBuilder();
        for (int i = 0; i < 600; i++) {
            buildSamples(str, Math.PI - 2.141592, t0, t1 + i, defScale, defShift);
        }

        printHeader("varying ts with low shift");
        for (int i = 0; i < 100; i++) {
            printResult(Math.PI, t0, t1 + i, defScale, 25);
        }

        String outDir = "/tmp/xform/";
        File outDirFile = new File(outDir);
        if (!outDirFile.exists()) {
            outDirFile.mkdirs();
        }
        String out = outDir + "approx-error.data";
        try(FileWriter w = new FileWriter(new File(out))) {
            w.write(str.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static final String fmts = "%7s %19s %12s %5s %5s %5s %19s %10s %12s";
    private static final String fmtd = "%7.2f %19d %12s %5s %5d %5d %19d %10d %12s";

    public void printHeader(String msg) {
        System.out.println(msg);
        System.out.println(String.format(fmts, "alpha", "ts", "scale", "shift", "f1", "f2", "f3", "f4", "f5"));
    }

    public void printResult(double alpha, long t0, long t1, long scale, int shift) {
        long expected = precise(alpha, t1);
        System.out.println(String.format(fmtd,
                alpha,
                t1,
                scale,
                shift,
                expected - approxFloat(alpha, t1),
                expected - approxFloatAndScale(alpha, t0, t1),
                expected - approxInt(alpha, scale, t1),
                expected - approxIntAndScale(alpha, scale, t0, t1),
                expected - approxIntAndScaleWithShift(alpha, shift, t0, t1)
                ));
    }

    public void buildSamples(StringBuilder str, double alpha, long t0, long t1, long scale, int shift) {
        long expected = precise(alpha, t1);
        str.append(String.format("%d,%d,%d,%d,%d\n",
                expected - approxFloat(alpha, t1),
                expected - approxFloatAndScale(alpha, t0, t1),
                expected - approxInt(alpha, scale, t1),
                expected - approxIntAndScale(alpha, scale, t0, t1),
                expected - approxIntAndScaleWithShift(alpha, shift, t0, t1)
                ));
    }

    public long precise(double alpha, long ts) {
        BigDecimal m = BigDecimal.valueOf(alpha);
        return BigDecimal.valueOf(ts).multiply(m, fMc).longValue();
    }

    public long approxFloat(double alpha, long ts) {
        return (long) (alpha * ts);
    }

    public long approxFloatAndScale(double alpha, long start, long ts) {
        BigDecimal m = BigDecimal.valueOf(alpha);
        long off = BigDecimal.valueOf(start).multiply(m, fMc).longValue();
        return (long) (alpha * (ts - start) + off);
    }

    public long approxInt(double alpha, long scale, long ts) {
        long alphaInt = (long) (alpha * scale);
        return ((alphaInt * ts) / scale);
    }

    public long approxIntAndScale(double alpha, long scale, long start, long ts) {
        BigDecimal m = BigDecimal.valueOf(alpha);
        long off = BigDecimal.valueOf(start).multiply(m, fMc).longValue();
        long alphaInt = (long) (alpha * scale);
        return ((alphaInt * (ts - start)) / scale) + off;
    }

    public long approxIntAndScaleWithShift(double alpha, int shift, long start, long ts) {
        BigDecimal m = BigDecimal.valueOf(alpha);
        long off = BigDecimal.valueOf(start).multiply(m, fMc).longValue();
        long alphaInt = (long) (alpha * (1 << shift));
        return ((alphaInt * (ts - start)) >>> shift) + off;
    }

    @Override
    public void createOptions(Options options) {
    }

}
