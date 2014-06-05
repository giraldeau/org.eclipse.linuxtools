package org.eclipse.linuxtools.tmf.analysis.graph.core.tests.staging;

import static org.junit.Assert.assertEquals;

import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.PackedLongValue;
import org.junit.Test;

public class TestPackedValue {

    @Test
    public void testPackedValueRoundTrip() {
        int[] exp = new int[] { 0xFF, 0xA0A0A0 };
        int[] act = new int[exp.length];
        long packed = PackedLongValue.pack(exp[0], exp[1]);
        act[0] = PackedLongValue.unpack(0, packed);
        act[1] = PackedLongValue.unpack(1, packed);
        assertEquals(exp[0], act[0]);
        assertEquals(exp[1], act[1]);
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testOverflowIllegalIndex() {
        PackedLongValue.unpack(2, 0);
    }

}
