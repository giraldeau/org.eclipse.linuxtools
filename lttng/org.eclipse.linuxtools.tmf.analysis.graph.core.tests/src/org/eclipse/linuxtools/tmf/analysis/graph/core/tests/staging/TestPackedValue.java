package org.eclipse.linuxtools.tmf.analysis.graph.core.tests.staging;

import static org.junit.Assert.assertEquals;

import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.PackedIntValue;
import org.junit.Test;

public class TestPackedValue {

    @Test
    public void testPackedValueRoundTrip() {
        int[] exp = new int[] { 0xFF, 0xA0A0A0 };
        int[] act = new int[exp.length];
        int packed = PackedIntValue.pack(exp[0], exp[1]);
        act[0] = PackedIntValue.unpack(0, packed);
        act[1] = PackedIntValue.unpack(1, packed);
        assertEquals(exp[0], act[0]);
        assertEquals(exp[1], act[1]);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testOverflowLow() {
        PackedIntValue.pack(0x1FF, 0x0);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testOverflowHigh() {
        PackedIntValue.pack(0x0, 0x1FFFFFF);
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testOverflowIllegalIndex() {
        PackedIntValue.unpack(2, 0);
    }

}
