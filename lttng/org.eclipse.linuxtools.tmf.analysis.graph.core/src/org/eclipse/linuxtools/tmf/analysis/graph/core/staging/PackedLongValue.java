package org.eclipse.linuxtools.tmf.analysis.graph.core.staging;

public class PackedLongValue {

    /**
     * Pack two integers into one long
     *
     * @param low integer
     * @param high integer
     * @return the packed long value
     */
    public static long pack(int low, int high) {
        long val = low;
        val |= ((long) high) << 32;
        return val;
    }

    public static int unpack(int index, long value) {
        int res = 0;
        switch(index) {
        case 0:
            res = (int) value;
            break;
        case 1:
            res = (int) (value >>> 32);
            break;
        default:
            throw new IndexOutOfBoundsException();
        }
        return res;
    }

}
