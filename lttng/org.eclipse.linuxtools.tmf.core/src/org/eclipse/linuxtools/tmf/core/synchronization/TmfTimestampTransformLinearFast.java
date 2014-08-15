package org.eclipse.linuxtools.tmf.core.synchronization;

import java.math.BigDecimal;

import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public class TmfTimestampTransformLinearFast extends TmfTimestampTransformLinear {

    private static final long serialVersionUID = 2398540405078949739L;
    private long scaleOffset;
    private long start;
    private long fAlphaLong;
    private long fBetaLong;
    private long scaleMiss;
    private int hc;
    private static final int tsBitWidth = 30;
    private static final HashFunction hf = Hashing.goodFastHash(32);
    /*
            HashFunction hf =
    HashCode hc =
           */

    public TmfTimestampTransformLinearFast(TmfTimestampTransformLinear xform) {
        super(xform.getAlpha(), xform.getBeta());
        fAlphaLong = xform.getAlpha().multiply(BigDecimal.valueOf(1 << tsBitWidth)).longValue() ;
        fBetaLong = xform.getBeta().longValue();
        start = 0L;
        scaleOffset = 0L;
        scaleMiss = 0;
        hc = hf.newHasher()
                .putLong(getAlpha().longValue())
                .putLong(getBeta().longValue())
                .hash()
                .asInt();
    }

    private long apply(long ts) {
        // rescale if we exceed the
        if (Math.abs(ts - start) > (1 << tsBitWidth)) {
            scaleOffset = BigDecimal.valueOf(ts).multiply(getAlpha(), fMc).longValue();
            start = ts;
            scaleMiss++;
        }
        return ((fAlphaLong * (ts - start)) >>> tsBitWidth) + scaleOffset + fBetaLong;
    }

    @Override
    public ITmfTimestamp transform(ITmfTimestamp timestamp) {
        return new TmfTimestamp(timestamp, apply(timestamp.getValue()));
    }

    @Override
    public long transform(long timestamp) {
        return apply(timestamp);
    }

    public long getScaleMiss() {
        return scaleMiss;
    }

    public void setScaleMiss(long scaleMiss) {
        this.scaleMiss = scaleMiss;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof TmfTimestampTransformLinearFast) {
            TmfTimestampTransformLinearFast that = (TmfTimestampTransformLinearFast) other;
            return this.getAlpha().equals(that.getAlpha()) &&
                    this.getBeta().equals(that.getBeta());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return hc;
    }


}