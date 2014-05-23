package org.eclipse.linuxtools.tmf.core.tests.synchronization;

import static org.junit.Assert.*;

import org.eclipse.linuxtools.tmf.core.synchronization.SyncGraph;
import org.junit.Test;

/**
 * Test synchronization graph data structure and algorithms
 *
 * @author Francis Giraldeau <francis.giraldeau@gma.com>
 *
 */
public class SyncGraphTest {

    // Source: http://algs4.cs.princeton.edu/42directed/tinyDG.txt
    private static int[][] tinyDG = new int[][] {
        { 4, 2 },
        { 2, 3 },
        { 3, 2 },
        { 6, 0 },
        { 0, 1 },
        { 2, 0 },
        { 11, 12 },
        { 12, 9 },
        { 9, 10 },
        { 9, 11 },
        { 7, 9 },
        { 10, 12 },
        { 11, 4 },
        { 4, 3 },
        { 3, 5 },
        { 6, 8 },
        { 8, 6 },
        { 5, 4 },
        { 0, 5 },
        { 6, 4 },
        { 6, 9 },
        { 7, 6 },
    };

    private static SyncGraph<Integer, Integer> makeGraph(int[][] edges) {
        SyncGraph<Integer, Integer> g = new SyncGraph<>();
        for (int i = 0; i < edges.length; i++) {
            g.addEdge(edges[i][0], edges[i][1], 1);
            //g.addEdge(edges[i][1], edges[i][0], -1);
        }
        return g;
    }

    /**
     * Test graph construction
     */
    @Test
    public void testCreateGraph() {
        SyncGraph<Integer, Integer> g = makeGraph(tinyDG);
        assertEquals(tinyDG.length, g.E());
        assertEquals(13, g.V());
        assertEquals(4, g.adj(6).size());
    }

    /**
     * Check if graph is connected or not
     */
    @Test
    public void testConnected() {
        SyncGraph<Integer, Integer> g = makeGraph(tinyDG);
        assertFalse(g.isConnected());
        g = makeGraph(new int[][] { {0, 1}, {1, 2}, {3, 4} });
        assertFalse(g.isConnected());
        g = makeGraph(new int[][] { {0, 1}, {1, 2}, {2, 3} });
        assertTrue(g.isConnected());
    }

    /**
     * Test shortest path between two vertices
     */
    @Test
    public void testPath() {
        SyncGraph<Integer, Integer> g = makeGraph(tinyDG);
        System.out.println(g.toString());
        g.path(8, 1);
    }

}
