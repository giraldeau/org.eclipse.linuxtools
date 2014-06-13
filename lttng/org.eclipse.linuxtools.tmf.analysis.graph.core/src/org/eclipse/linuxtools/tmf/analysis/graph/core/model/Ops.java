/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien & Francis Giraldeau- Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.analysis.graph.core.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfEdge;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraphVisitor;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfVertex;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfEdge.EdgeType;

/**
 * Class that implements static operations on vertices and edges. The sets of
 * nodes and vertices can then be transformed to a graph.
 *
 * @author Francis Giraldeau
 */
public class Ops {

    /**
     * Create an horizontal edge of default type between 2 vertices
     *
     * @param len
     *            The length of the edge
     * @return The first vertex
     */
    public static TmfVertex basic(long len) {
        return basic(len, EdgeType.DEFAULT, 0);
    }

    /**
     * Create an horizontal edge between 2 vertices starting at time 0
     *
     * @param len
     *            The length of the edge
     * @param type
     *            The type of the dge
     * @return The first vertex
     */
    public static TmfVertex basic(long len, EdgeType type) {
        return basic(len, type, 0);
    }

    /**
     * Create an horizontal edge between 2 vertices
     *
     * @param len
     *            The length of the edge
     * @param type
     *            The type of the dge
     * @param start
     *            The start time
     * @return The first vertex
     */
    public static TmfVertex basic(long len, EdgeType type, long start) {
        TmfVertex head = new TmfVertex(start);
        TmfEdge link = head.linkHorizontal(new TmfVertex(start + len));
        link.setType(type);
        return head;
    }

    private static class CloneState {
        HashMap<TmfVertex, TmfVertex> map = new HashMap<>(); // orig, clone
        TmfVertex head;
    }

    /**
     * Clone the provided connected node set. Returns the head of the new node
     * sequence.
     *
     * @param orig
     *            The vertex to clone
     * @return The head of the new vertex set
     */
    public static TmfVertex clone(TmfVertex orig) {
        return clone(orig, 0);
    }

    /**
     * Clone the provided connected vertex/edge set. Returns the head of the new
     * vertex sequence.
     *
     * @param orig
     *            The vertex to clone
     * @param offset
     *            The time offset to apply
     * @return The head of the new vertex set
     */
    public static TmfVertex clone(TmfVertex orig, final long offset) {
        // two steps clone:
        // 1- clone all nodes
        // 2- create links
        final CloneState state = new CloneState();
        ScanLineTraverse.traverse(orig, new TmfGraphVisitor() {
            @Override
            public void visitHead(TmfVertex node) {
            }
            @Override
            public void visit(TmfVertex node) {
                TmfVertex clone = new TmfVertex(node, node.getTs() + offset);
                state.map.put(node, clone);
            }
            @Override
            public void visit(TmfEdge link, boolean hori) {
            }
        });
        // FIXME: can iterate over map keys
        ScanLineTraverse.traverse(orig, new TmfGraphVisitor() {
            @Override
            public void visitHead(TmfVertex node) {
                if (state.head == null) {
                    state.head = state.map.get(node);
                }
            }
            @Override
            public void visit(TmfVertex node) {
            }
            @Override
            public void visit(TmfEdge link, boolean hori) {
                TmfVertex from = state.map.get(link.getVertexFrom());
                TmfVertex to = state.map.get(link.getVertexTo());
                TmfEdge lnk = null;
                if (hori) {
                    lnk = from.linkHorizontal(to);
                } else {
                    lnk = from.linkVertical(to);
                }
                lnk.setType(link.getType());
            }
        });
        return state.head;
    }

    /**
     * The head of set n2 is connected at the tail of set n1. If necessary, n2
     * is offset such that is starts at the same time n1 ends. Also vertical
     * edges from the head of n2 are connected at the tail of n1 instead.
     *
     * @param n1
     *            Base set
     * @param n2
     *            Set to concatenate to n1
     */
    public static void concatInPlace(TmfVertex n1, TmfVertex n2) {
        TmfVertex x = Ops.tail(n1);
        TmfVertex y = Ops.head(n2);
        if (!y.hasNeighbor(TmfVertex.OUTH)) {
            return;
        }
        long offset = x.getTs() - y.getTs();
        TmfVertex nnew = Ops.offset(n2, offset);
        y = Ops.head(nnew);
        if ((x.hasNeighbor(TmfVertex.OUTV) && y.hasNeighbor(TmfVertex.OUTV)) ||
                x.hasNeighbor(TmfVertex.INV) && y.hasNeighbor(TmfVertex.INV)) {
            throw new RuntimeException("concat would overwrite links");
        }
        TmfVertex rightNode = y.neighbor(TmfVertex.OUTH);
        EdgeType oldType = y.getEdges()[TmfVertex.OUTH].getType();
        x.linkHorizontal(rightNode).setType(oldType);
        if (y.hasNeighbor(TmfVertex.OUTV)) {
            TmfVertex up = y.neighbor(TmfVertex.OUTV);
            oldType = y.getEdges()[TmfVertex.OUTV].getType();
            x.linkVertical(up).setType(oldType);
        }
        if (y.hasNeighbor(TmfVertex.INV)) {
            TmfVertex down = y.neighbor(TmfVertex.INV);
            oldType = y.getEdges()[TmfVertex.INV].getType();
            down.linkVertical(x).setType(oldType);
        }
    }

    /**
     * Create edges between the tails of respectively n2 and n1 and from the
     * head of respectively n1 and n2. The resulting is a set with all nodes
     * from n1 and n2
     *
     * @param n1
     *            Vertex in first set
     * @param n2
     *            Vertex in second set
     * @param split
     *            Type of the edge ffom the heads
     * @param merge
     *            Type of the head from the tails
     * @return The head vertex in n1
     */
    public static TmfVertex unionInPlace(TmfVertex n1, TmfVertex n2, EdgeType split, EdgeType merge) {
        unionInPlaceRight(n1, n2, merge);
        return unionInPlaceLeft(n1, n2, split);
    }

    /**
     * Create a link between the head of the sequence starting at n1 to the head
     * of sequence starting at n2
     *
     * @param n1
     *            Vertex in first sequence
     * @param n2
     *            Vertex in second sequence
     * @param split
     *            The type of the link
     * @return The tail node of sequence 1
     */
    public static TmfVertex unionInPlaceLeft(TmfVertex n1, TmfVertex n2, EdgeType split) {
        TmfVertex h1 = Ops.head(n1);
        TmfVertex h2 = Ops.head(n2);
        if (h1.hasNeighbor(TmfVertex.OUTV) || h2.hasNeighbor(TmfVertex.INV)) {
            throw new RuntimeException("union would overwrite links");
        }
        h1.linkVertical(h2).setType(split);
        return h1;
    }

    /**
     * Create a link between the end of the sequence starting at n2 to the end
     * of sequence starting at n1
     *
     * @param n1
     *            Vertex in first sequence
     * @param n2
     *            Vertex in second sequence
     * @param merge
     *            The type of the link
     * @return The tail node of sequence 1
     */
    public static TmfVertex unionInPlaceRight(TmfVertex n1, TmfVertex n2, EdgeType merge) {
        TmfVertex t1 = Ops.tail(n1);
        TmfVertex t2 = Ops.tail(n2);
        if (t1.hasNeighbor(TmfVertex.INV) || t2.hasNeighbor(TmfVertex.OUTV)) {
            throw new RuntimeException("union would overwrite links");
        }
        t2.linkVertical(t1).setType(merge);
        return t1;
    }

    /**
     * Generate an horizontal sequence of NUM nodes with STEP timestamps
     *
     * @param num
     *            The number of vertices to create
     * @param step
     *            The time step between vertices
     * @return The head of the sequence
     */
    public static TmfVertex sequence(int num, int step) {
        return sequence(num, step, EdgeType.DEFAULT);
    }

    /**
     * Generate an horizontal sequence of NUM nodes with STEP timestamps
     *
     * @param num
     *            The number of vertices to create
     * @param step
     *            The time step between vertices
     * @param type
     *            The type of the edge
     * @return The head of the sequence
     */
    public static TmfVertex sequence(int num, int step, EdgeType type) {
        return sequence(num, step, type, 0);
    }

    /**
     * Generate an horizontal sequence of NUM nodes with STEP timestamps
     *
     * @param num
     *            The number of vertices
     * @param step
     *            The time step between vertices
     * @param type
     *            The type of the edges
     * @param start
     *            The start time
     * @return The head of the sequence
     */
    public static TmfVertex sequence(int num, int step, EdgeType type, long start) {
        TmfVertex curr = null;
        TmfVertex next = null;
        for (int i = 0; i < num; i++) {
            next = new TmfVertex(start + (i * step));
            if (curr != null) {
                TmfEdge link = curr.linkHorizontal(next);
                link.setType(type);
            }
            curr = next;
        }
        return head(curr);
    }

    /**
     * Align tail timestamps of N2 according to tail of N1
     *
     * @param n1
     *            Vertex in first set
     * @param n2
     *            Vertex in second set
     * @return The head vertex of new aligned n2 set
     */
    public static TmfVertex alignRight(TmfVertex n1, TmfVertex n2) {
        TmfVertex t1 = tail(n1);
        TmfVertex t2 = tail(n2);
        long diff = t1.getTs() - t2.getTs();
        return offset(n2, diff);
    }

    /**
     * Get the last horizontal vertex out of a vertex
     *
     * @param node
     *            Starting vertex
     * @return The last horizontal vertex from the start
     */
    public static TmfVertex tail(TmfVertex node) {
        TmfVertex currentNode = node;
        while(currentNode.outh() != null) {
            currentNode = currentNode.outh();
        }
        return currentNode;
    }

    /**
     * Get the rightmost vertex in a given set, also searches vertically
     *
     * @param node
     *            A vertex in the set
     * @return The rightmost vertex
     */
    public static TmfVertex end(TmfVertex node) {
        TmfVertex rightnode = node, topnode = node;
        if (node.outh() != null) {
            rightnode = end(node.outh());
        }
        if (node.outv() != null) {
            topnode = end(node.outv());
        }
        if (topnode.getTs() > rightnode.getTs()) {
            return topnode;
        }
        return rightnode;
    }

    /**
     * Get the first vertex getting in a vertex
     *
     * @param node
     *            The start vertex
     * @return The first vertex getting in
     */
    public static TmfVertex head(TmfVertex node) {
        TmfVertex currentNode = node;
        while(currentNode.inh() != null) {
            currentNode = currentNode.inh();
        }
        return currentNode;
    }

    /**
     * Seek horizontally a vertex at a given distance
     *
     * @param node
     *            The starting vertex
     * @param distance
     *            The distance to seek for the vertex
     * @return The arrival vertex
     */
    public static TmfVertex seek(TmfVertex node, int distance) {
        TmfVertex currentNode = node;
        int nbSeek = distance;
        int dir = TmfVertex.OUTH;
        if (nbSeek < 0) {
            nbSeek = Math.abs(nbSeek);
            dir = TmfVertex.INH;
        }
        for (int i = 0; i < nbSeek; i++) {
            if (currentNode.hasNeighbor(dir)) {
                currentNode = currentNode.neighbor(dir);
            }
        }
        return currentNode;
    }

    /**
     * Recreate a vertex/edge sequence at an offset
     *
     * @param node
     *            The vertex to offset
     * @param offset
     *            The time offset to apply
     * @return The head of the new sequence
     */
    public static TmfVertex offset(TmfVertex node, final long offset) {
        if (offset == 0) {
            return node;
        }
        return clone(node, offset);
    }

    /**
     * Creates a graph from a set of vertices and edges. Every head node visited
     * is considered from a new actor
     *
     * @param head
     *            A vertex in the set
     * @return The graph containing the set of vertices and edges
     */
    public static TmfGraph toGraphInPlace(TmfVertex head) {
        final TmfGraph g = new TmfGraph();
        ScanLineTraverse.traverse(head, new TmfGraphVisitor() {
            Long actor = -1L;

            @Override
            public void visitHead(TmfVertex node) {
                actor++;
            }

            @Override
            public void visit(TmfVertex node) {
                g.add(actor, node);
            }

            @Override
            public void visit(TmfEdge link, boolean hori) {
            }
        });
        return g;
    }

    private static class ValidateState {
        boolean ok = true;
    }

    /**
     * Check that timestamps increase monotonically
     *
     * @param vertex
     *            A vertex in the set to validate
     * @return if timestamps increase monotonically
     */
    public static boolean validate(TmfVertex vertex) {
        final ValidateState state = new ValidateState();
        ScanLineTraverse.traverse(vertex, new TmfGraphVisitor() {
            @Override
            public void visitHead(TmfVertex node) {
            }

            @Override
            public void visit(TmfVertex node) {
            }

            @Override
            public void visit(TmfEdge link, boolean hori) {
                long elapsed = link.getVertexTo().getTs() - link.getVertexFrom().getTs();
                if (elapsed < 0) {
                    state.ok = false;
                    System.err.println("timestamps error on link " + link +
                            " : " + link.getVertexFrom().getTs() + " -> " + link.getVertexTo().getTs() +
                            " (" + elapsed + ")");
                }
            }
        });
        return state.ok;
    }

    /**
     * Check if traversing N1 and N2 yields the same node sequence, that all
     * nodes have the same timestamps and that links type is the same.
     *
     * @param n1
     *            The starting node from first set
     * @param n2
     *            The starting node from second set
     * @return Whether the two vertex sets have the same configuration
     */
    public static boolean match(TmfVertex n1, TmfVertex n2) {
        return match(n1, n2, MATCH_TIMESTAMPS | MATCH_LINKS_TYPE);
    }

    /**
     * Check nodes and links structure
     */
    public static final int MATCH_ISOMORPH = 0;

    /**
     * Check all nodes timestamps for equality
     */
    public static final int MATCH_TIMESTAMPS = 1;

    /**
     * Check all links types for equality
     */
    public static final int MATCH_LINKS_TYPE = 2;

    /**
     * Check if traversing N1 and N2 respect properties PROP. Properties can be ORed.
     *
     * @param n1
     *            The starting node from first set
     * @param n2
     *            The starting node from second set
     * @param prop flags for the match
     * @return Whether the two vertex sets have the same configuration
     */
    public static boolean match(TmfVertex n1, TmfVertex n2, int prop) {
        if (n1 == null || n2 == null) {
            return false;
        }
        Stack<TmfVertex> stack = new Stack<>();
        HashSet<TmfVertex> visited = new HashSet<>();
        stack.push(n2);
        stack.push(n1);
        boolean ok = true;
        while(!stack.isEmpty() && ok) {
            TmfVertex c1 = stack.pop();
            TmfVertex c2 = stack.pop();
            if (visited.contains(c1) && visited.contains(c2)) {
                continue;
            }
            visited.add(c1);
            visited.add(c2);
            // check timestamps
            if ((prop & MATCH_TIMESTAMPS) != 0) {
                if (c1.compareTo(c2) != 0) {
                    ok = false;
                    break;
                }
            }
            // follow links, push next nodes
            for (int i = 0; i < c1.getEdges().length; i++) {
                if (c1.hasNeighbor(i) && !c2.hasNeighbor(i) ||
                        !c1.hasNeighbor(i) && c2.hasNeighbor(i)) {
                    ok = false;
                    break;
                }
                if (c1.hasNeighbor(i) && c2.hasNeighbor(i)) {
                    // check links type
                    if ((prop & MATCH_LINKS_TYPE) != 0) {
                        if (c1.getEdges()[i].getType() != c2.getEdges()[i].getType()) {
                            ok = false;
                            break;
                        }
                    }
                    stack.push(c2.neighbor(i));
                    stack.push(c1.neighbor(i));
                }
            }
        }
        return ok;
    }

}
