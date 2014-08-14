package org.eclipse.linuxtools.tmf.core.synchronization;

import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;

public class TmfTimestampTransformLinearFast extends TmfTimestampTransformLinear {

    private static final long serialVersionUID = 2398540405078949738L;
    private double m = 0;
    private long b = 0;
//    private long t0 = 0;

    public TmfTimestampTransformLinearFast(TmfTimestampTransformLinear xform) {
        super();
//        this.t0 = t0;
        m = xform.getAlpha().doubleValue();
        b = xform.getBeta().longValue();
    }

    private long apply(long ts) {
        return (long) (m * ts) + b;
    }

    @Override
    public ITmfTimestamp transform(ITmfTimestamp timestamp) {
        return new TmfTimestamp(timestamp, apply(timestamp.getValue()));
    }

    @Override
    public long transform(long timestamp) {
        return apply(timestamp);
    }

}