package org.eclipse.linuxtools.tmf.analysis.graph.core.staging;

public class PackedIntValue {

//    public static final int HIGH_MAX_VALUE = (1 << ((3 * 8) - 1)) - 1;  //  2^23-1
//    public static final int HIGH_MIN_VALUE = -1 * (1 << (3 * 8));       // -2^23

    /**
     * Pack low integer to first byte, and high integer to the upper three bytes
     *
     * @param low the lsb
     * @param high the msb
     * @return the packed value
     */
    public static int pack(int low, int high) {
        if (((low & (~0 << 8)) != 0) || ((high & (~0 << 24)) != 0)) {
            throw new IllegalArgumentException("overflow"); //$NON-NLS-1$
        }
        int val = low & 0xFF;
        val |= high << 8;
        return val;
    }

    public static int unpack(int index, int value) {
        int res = 0;
        switch(index) {
        case 0:
            res = value & 0xFF;
            break;
        case 1:
            res = (value >>> 8);
            break;
        default:
            throw new IndexOutOfBoundsException();
        }
        return res;
    }

}
