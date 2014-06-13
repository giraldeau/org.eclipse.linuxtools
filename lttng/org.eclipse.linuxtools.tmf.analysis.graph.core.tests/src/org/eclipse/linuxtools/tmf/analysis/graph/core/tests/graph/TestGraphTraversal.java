package org.eclipse.linuxtools.tmf.analysis.graph.core.tests.graph;

import static org.junit.Assert.assertEquals;

import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfEdge;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraphBFS;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraphVisitor;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfVertex;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.Ops;
import org.junit.Test;

/**
 * Test graph traversal algorithm
 *
 * @author francis
 *
 */
public class TestGraphTraversal {

    private Object A = "A";
    private Object B = "B";
    private Object C = "C";
    private Object[] actors = { A, B, C };

    /**
     * Test Breadth-First Search traversal
     */
    int countForward = 0;
    int countBackward = 0;

    @Test
    public void testBFS() {
        // build graph (3 levels, 18 nodes)
        TmfGraph graph = new TmfGraph();
        for (Object actor: actors) {
            TmfVertex v = Ops.sequence(6, 10);
            do {
                graph.add(actor, v);
            } while((v = v.outh()) != null);
        }
        int x = actors.length;
        for (int i = 0; i < x - 1; i++) {
            TmfVertex h1 = graph.getHead(actors[i]);
            TmfVertex h2 = graph.getHead(actors[i+1]);
            Ops.seek(h1, i + 1).linkVertical(Ops.seek(h2, i + 1));
            Ops.seek(h2, x - i + 1).linkVertical(Ops.seek(h1, x - i + 1));
        }

        TmfGraphBFS bfs = new TmfGraphBFS();

        bfs.traverseForward(graph.getHead(A), null, new TmfGraphVisitor() {
            @Override
            public void visit(TmfEdge e, boolean h) {
                countForward++;
            }
        });

        bfs.traverseBackward(graph.getTail(A), null, new TmfGraphVisitor() {
            @Override
            public void visit(TmfEdge e, boolean h) {
                countBackward++;
            }
        });
        // graph is symmetric
        assertEquals(16, countForward);
        assertEquals(16, countBackward);
    }

}
