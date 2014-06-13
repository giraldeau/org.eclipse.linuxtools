package org.eclipse.linuxtools.tmf.analysis.graph.core.base;

import java.util.Comparator;

public class TmfVertexComparators {

    public static Comparator<TmfVertex> ascending = new Comparator<TmfVertex>() {
        @Override
        public int compare(TmfVertex v1, TmfVertex v2) {
            return v1.getTs() > v2.getTs() ? 1 : (v1.getTs() == v2.getTs() ? 0 : -1);
        }
    };

    public static Comparator<TmfVertex> descending = new Comparator<TmfVertex>() {
        @Override
        public int compare(TmfVertex v1, TmfVertex v2) {
            return v1.getTs() < v2.getTs() ? 1 : (v1.getTs() == v2.getTs() ? 0 : -1);
        }
    };

}
