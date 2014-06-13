/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.analysis.graph.core.tests.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.eclipse.linuxtools.tmf.analysis.graph.core.base.ITmfGraphVisitor;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfEdge;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraphStatistics;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfVertex;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfEdge.EdgeType;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the basic functionalities of the TmfGraph, TmfVertex and TmfEdge classes.
 */
@SuppressWarnings("javadoc")
public class TmfGraphTest {

    private Object A = "A";
    private Object B = "B";
    private TmfGraph fGraph;
    private TmfVertex fV0;
    private TmfVertex fV1;

    @Before
    public void buildBasicGraph() {
        fGraph = new TmfGraph();
        fV0 = new TmfVertex(0);
        fV1 = new TmfVertex(1);
    }

    @Test
    public void testDefaultConstructor() {
        TmfGraph g = new TmfGraph();
        assertEquals(0, g.size());
    }

    @Test
    public void testPutNode() {
        fGraph.replace(A, fV0);
        fGraph.replace(A, fV1);
        assertEquals(1, fGraph.getNodesOf(A).size());
    }

    @Test
    public void testAppendNode() {
        int max = 10;
        for(int i = 0; i < max; i++) {
            fGraph.append(A, new TmfVertex(i));
        }
        List<TmfVertex> list = fGraph.getNodesOf(A);
        assertEquals(max, list.size());
        checkLinkHorizontal(list);

        // With a type
        TmfEdge link = fGraph.append(A, new TmfVertex(max+1), EdgeType.BLOCKED);
        assertEquals(EdgeType.BLOCKED, link.getType());
    }

    @Test
    public void testLink() {
        // Start with a first node
        fGraph.add(A, fV0);

        // Link with second node not in graph
        TmfEdge link = fGraph.link(fV0, fV1, EdgeType.NETWORK);
        List<TmfVertex> list = fGraph.getNodesOf(A);
        assertEquals(2, list.size());
        assertEquals(fV0, fV1.inh());
        assertEquals(fV1, fV0.outh());
        assertEquals(EdgeType.NETWORK, link.getType());

        // Link with second node for the same object
        TmfVertex v2 = new TmfVertex(2);
        fGraph.add(A, v2);
        link = fGraph.link(fV1, v2, EdgeType.NETWORK);
        list = fGraph.getNodesOf(A);
        assertEquals(3, list.size());
        assertEquals(v2, fV1.outh());
        assertEquals(fV1, v2.inh());
        assertEquals(EdgeType.NETWORK, link.getType());

        // Link with second node for another object
        TmfVertex v3 = new TmfVertex(3);
        fGraph.add(B, v3);
        link = fGraph.link(v2, v3, EdgeType.NETWORK);
        list = fGraph.getNodesOf(B);
        assertEquals(1, list.size());
        list = fGraph.getNodesOf(A);
        assertEquals(3, list.size());
        assertEquals(v2, v3.inv());
        assertEquals(v3, v2.outv());
        assertEquals(EdgeType.NETWORK, link.getType());

    }

    private static void checkLinkHorizontal(List<TmfVertex> list) {
        if (list.isEmpty()) {
            return;
        }
        for (int i = 0; i < list.size() - 1; i++) {
            TmfVertex n0 = list.get(i);
            TmfVertex n1 = list.get(i+1);
            assertEquals(n0.outh(), n1);
            assertEquals(n1.inh(), n0);
            assertEquals(n0.getEdges()[TmfVertex.OUTH].getVertexFrom(), n0);
            assertEquals(n1.getEdges()[TmfVertex.INH].getVertexTo(), n1);
            assertNull(n1.outv());
            assertNull(n1.inv());
            assertNull(n0.outv());
            assertNull(n0.inv());
        }
    }

    @Test
    public void testAddNode() {
        int max = 10;
        for(int i = 0; i < max; i++) {
            fGraph.add(A, new TmfVertex(i));
        }
        List<TmfVertex> list = fGraph.getNodesOf(A);
        assertEquals(max, list.size());
        for (int i = 0; i < list.size() - 1; i++) {
            TmfVertex n0 = list.get(i);
            assertNull(n0.outh());
            assertNull(n0.inh());
            assertNull(n0.outv());
            assertNull(n0.inv());
        }
    }

    @Test
    public void testIllegalNode() {
        fGraph.append(A, new TmfVertex(1));
        Exception exception = null;
        try {
            fGraph.append(A, new TmfVertex(0));
        } catch (IllegalArgumentException e) {
            exception = e;
        }
        assertNotNull(exception);
    }

    @Test
    public void testTail() {
        fGraph.append(A, fV0);
        fGraph.append(A, fV1);
        assertEquals(fV1, fGraph.getTail(A));
        assertEquals(fV1, fGraph.removeTail(A));
        assertEquals(fV0, fGraph.getTail(A));
    }

    @Test
    public void testHead() {
        fGraph.append(A, fV0);
        fGraph.append(A, fV1);
        assertEquals(fV0, fGraph.getHead(A));
        assertEquals(fV0, fGraph.getHead(fV1));
        assertEquals(fV0, fGraph.getHead(fV0));
    }

    @Test
    public void testParent() {
        fGraph.append(A, fV0);
        fGraph.append(B, fV1);
        assertEquals(A, fGraph.getParentOf(fV0));
        assertNotSame(A, fGraph.getParentOf(fV1));
        assertEquals(B, fGraph.getParentOf(fV1));
    }

    @Test
    public void testVertexAt() {
        TmfVertex[] vertices = new TmfVertex[5];
        for (int i = 0; i < 5; i++) {
            vertices[i] = new TmfVertex( (i+1) * 5);
            fGraph.append(A, vertices[i]);
        }
        assertEquals(vertices[0], fGraph.getVertexAt(new TmfTimestamp(5), A));
        assertEquals(vertices[0], fGraph.getVertexAt(new TmfTimestamp(0), A));
        assertEquals(vertices[1], fGraph.getVertexAt(new TmfTimestamp(6), A));
        assertEquals(vertices[3], fGraph.getVertexAt(new TmfTimestamp(19), A));
        assertEquals(vertices[3], fGraph.getVertexAt(new TmfTimestamp(20), A));
        assertEquals(vertices[4], fGraph.getVertexAt(new TmfTimestamp(21), A));
        assertNull(fGraph.getVertexAt(new TmfTimestamp(26), A));
    }

    /**
     * The following graph will be used
     *
     * ____0___1___2___3___4___5___6___7___8___9___10___11___12___13___14___15
     *
     * A   *-------*       *---*-------*---*---*    *---*----*----*---------*
     *             |           |           |            |    |
     * B       *---*---*-------*   *-------*------------*    *----------*
     *
     */
    private TmfGraph buildFullGraph() {
        TmfGraph graph = new TmfGraph();
        TmfVertex[] vertexA;
        TmfVertex[] vertexB;
        long[] timesA = {0, 2, 4, 5, 7, 8, 9, 10, 11, 12, 13, 15};
        long[] timesB = {1, 2, 3, 5, 6, 8, 11, 12, 14};
        vertexA = new TmfVertex[timesA.length];
        vertexB = new TmfVertex[timesB.length];
        for (int i = 0; i < timesA.length; i++) {
            vertexA[i] = new TmfVertex(timesA[i]);
        }
        for (int i = 0; i < timesB.length; i++) {
            vertexB[i] = new TmfVertex(timesB[i]);
        }
        graph.append(A, vertexA[0]);
        graph.append(A, vertexA[1]);
        graph.add(A, vertexA[2]);
        graph.append(A, vertexA[3]);
        graph.append(A, vertexA[4]);
        graph.append(A, vertexA[5]);
        graph.append(A, vertexA[6]);
        graph.add(A, vertexA[7]);
        graph.append(A, vertexA[8]);
        graph.append(A, vertexA[9]);
        graph.append(A, vertexA[10]);
        graph.append(A, vertexA[11]);
        graph.append(B, vertexB[0]);
        graph.append(B, vertexB[1]);
        graph.append(B, vertexB[2]);
        graph.append(B, vertexB[3]);
        graph.add(B, vertexB[4]);
        graph.append(B, vertexB[5]);
        graph.append(B, vertexB[6]);
        graph.add(B, vertexB[7]);
        graph.append(B, vertexB[8]);
        vertexA[1].linkVertical(vertexB[1]);
        vertexB[3].linkVertical(vertexA[3]);
        vertexA[5].linkVertical(vertexB[5]);
        vertexB[6].linkVertical(vertexA[8]);
        vertexA[9].linkVertical(vertexB[7]);
        return graph;
    }

    @Test
    public void testCheckHorizontal() {
        TmfVertex n0 = new TmfVertex(10);
        TmfVertex n1 = new TmfVertex(0);
        Exception exception = null;
        try {
            n0.linkHorizontal(n1);
        } catch (IllegalArgumentException e) {
            exception = e;
        }
        assertNotNull(exception);
    }

    @Test
    public void testCheckVertical() {
        TmfVertex n0 = new TmfVertex(10);
        TmfVertex n1 = new TmfVertex(0);
        Exception exception = null;
        try {
            n0.linkVertical(n1);
        } catch (IllegalArgumentException e) {
            exception = e;
        }
        assertNotNull(exception);
    }

    private class ScanCountVertex implements ITmfGraphVisitor {
        public int nbVertex = 0;
        public int nbVLink = 0;
        public int nbHLink = 0;
        public int nbStartVertex = 0;

        @Override
        public void visitHead(TmfVertex node) {
            nbStartVertex++;
        }

        @Override
        public void visit(TmfVertex node) {
            nbVertex++;

        }

        @Override
        public void visit(TmfEdge edge, boolean horizontal) {
            if (horizontal) {
                nbHLink++;
            } else {
                nbVLink++;
            }
        }
    }

    @Test
    public void testScanCount() {
        TmfGraph graph = buildFullGraph();
        ScanCountVertex visitor = new ScanCountVertex();
        graph.scanLineTraverse(graph.getHead(A), visitor);
        assertEquals(21, visitor.nbVertex);
        assertEquals(6, visitor.nbStartVertex);
        assertEquals(5, visitor.nbVLink);
        assertEquals(15, visitor.nbHLink);
    }

    @Test
    public void testGraphStatistics() {
        TmfGraph graph = buildFullGraph();
        TmfGraphStatistics stats = new TmfGraphStatistics();
        stats.getGraphStatistics(graph, A);
        assertEquals(12, stats.getSum(A).longValue());
        assertEquals(11, stats.getSum(B).longValue());
        assertEquals(23, stats.getSum().longValue());
    }

}
