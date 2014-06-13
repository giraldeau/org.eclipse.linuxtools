package org.eclipse.linuxtools.tmf.analysis.graph.core.base;

import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;

/**
 * Breadth first traversal for TMF Graph
 *
 * @author francis
 */
public class TmfGraphBFS {

    public static int[] outEdges = new int[] { TmfVertex.OUTV, TmfVertex.OUTH };
    public static int[] incEdges = new int[] { TmfVertex.INV, TmfVertex.INH };

    public TmfGraphBFS() {
    }

    public void traverse(TmfVertex vertex, TmfVertex end, boolean forward, TmfGraphVisitor visitor) {
        int[] directions = forward ? outEdges : incEdges;
        Comparator<TmfVertex> comparator = forward ? TmfVertexComparators.ascending : TmfVertexComparators.descending;
        PriorityQueue<TmfVertex> queue = new PriorityQueue<>(1, comparator);
        HashSet<TmfVertex> visited = new HashSet<>();
        queue.offer(vertex);
        while(!queue.isEmpty()) {
            TmfVertex v = queue.poll();
            if (null != end && comparator.compare(v, end) > 0) {
                break;
            }
            if (!visited.contains(v)) {
                visited.add(v);
                visitor.visit(v);
            }

            for (int dir: directions) {
                if (v.hasNeighbor(dir)) {
                    TmfVertex next = v.neighbor(dir);
                    boolean horizontal = ((dir == TmfVertex.OUTH) || (dir == TmfVertex.INH));
                    visitor.visit(v.getEdges()[dir], horizontal);
                    if (!visited.contains(next)) {
                        queue.offer(next);
                    }
                }
            }
        }
    }

    public void traverseForward(TmfVertex head, TmfVertex end, TmfGraphVisitor visitor) {
        traverse(head, end, true, visitor);
    }

    public void traverseBackward(TmfVertex tail, TmfVertex end, TmfGraphVisitor visitor) {
        traverse(tail, end, false, visitor);
    }

}
