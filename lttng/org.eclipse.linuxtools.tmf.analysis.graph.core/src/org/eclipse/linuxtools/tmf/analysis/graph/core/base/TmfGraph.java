/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien and Francis Giraldeau - Initial implementation and API
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.analysis.graph.core.base;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;

import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfEdge.EdgeType;
import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;

import com.google.common.collect.ArrayListMultimap;

/**
 * Undirected, unweighed, timed graph data type for dependencies between
 * elements of a system.
 *
 * Vertices are timed: each vertex has a timestamp associated, so the vertex
 * belongs to an object (the key of the multimap) at a given time. This is why
 * we use an ArrayListMultimap to represent the graph, instead of a simple list.
 *
 * @since 3.0
 */
public class TmfGraph {

    private final ArrayListMultimap<Object, TmfVertex> nodeMap;
    private final Map<TmfVertex, Object> reverse;

    /* Latch tracking if the graph is done building or not */
    private final CountDownLatch finishedLatch = new CountDownLatch(1);

    /**
     * Constructor
     */
    public TmfGraph() {
        nodeMap = ArrayListMultimap.create();
        reverse = new HashMap<>();
    }

    /**
     * Replace tail node of the provided object without linking.
     *
     * @param obj
     *            The key of the object the vertex belongs to
     * @param node
     *            The vertex replacing the old one
     */
    public void replace(Object obj, TmfVertex node) {
        List<TmfVertex> list = nodeMap.get(obj);
        if (!list.isEmpty()) {
            TmfVertex n = list.remove(list.size() - 1);
            reverse.remove(n);
        }
        list.add(node);
        reverse.put(node, obj);
    }

    /**
     * Add node to the provided object without linking
     *
     * @param obj
     *            The key of the object the vertex belongs to
     * @param node
     *            The new vertex
     */
    public void add(Object obj, TmfVertex node) {
        if (obj == null) {
            throw new IllegalArgumentException(Messages.TmfGraph_KeyNull);
        }
        List<TmfVertex> list = nodeMap.get(obj);
        list.add(node);
        reverse.put(node, obj);
    }

    /**
     * Add node to object's list and make horizontal link with tail.
     *
     * @param obj
     *            The key of the object the vertex belongs to
     * @param node
     *            The new vertex
     * @return The edge constructed
     */
    public TmfEdge append(Object obj, TmfVertex node) {
        return append(obj, node, EdgeType.DEFAULT);
    }

    /**
     * Add node to object's list and make horizontal link with tail.
     *
     * @param obj
     *            The key of the object the vertex belongs to
     * @param node
     *            The new vertex
     * @param type
     *            The type of edge to create
     * @return The edge constructed
     */
    public TmfEdge append(Object obj, TmfVertex node, EdgeType type) {
        if (obj == null) {
            throw new IllegalArgumentException(Messages.TmfGraph_KeyNull);
        }
        List<TmfVertex> list = nodeMap.get(obj);
        TmfVertex tail = getTail(obj);
        TmfEdge link = null;
        if (tail != null) {
            link = tail.linkHorizontal(node);
            link.setType(type);
        }
        list.add(node);
        reverse.put(node, obj);
        return link;
    }

    /**
     * Add a link between two vertices of the graph. The from vertex must be in
     * the graph. If the 'to' vertex is not in the graph, it will be appended to
     * the object the 'from' vertex is for. Otherwise a vertical or horizontal
     * link will be created between the vertices.
     *
     * Caution: this will remove without warning any previous link from the
     * 'from' vertex
     *
     * @param from
     *            The source vertex
     * @param to
     *            The destination vertex
     * @return The newly created edge
     */
    public TmfEdge link(TmfVertex from, TmfVertex to) {
        return link(from, to, EdgeType.DEFAULT);
    }

    /**
     * Add a link between two vertices of the graph. The from vertex must be in
     * the graph. If the 'to' vertex is not in the graph, it will be appended to
     * the object the 'from' vertex is for. Otherwise a vertical or horizontal
     * link will be created between the vertices.
     *
     * Caution: this will remove without warning any previous link from the
     * 'from' vertex
     *
     * @param from
     *            The source vertex
     * @param to
     *            The destination vertex
     * @param type
     *            The type of edge to create
     * @return The newly created edge
     */
    public TmfEdge link(TmfVertex from, TmfVertex to, EdgeType type) {
        Object ofrom = reverse.get(from);
        Object oto = reverse.get(to);
        if (ofrom == null) {
            throw new IllegalArgumentException(Messages.TmfGraph_FromNotInGraph);
        }
        TmfEdge link;
        if (oto == null) {
            link = append(ofrom, to);
        } else if (oto.equals(ofrom)) {
            link = from.linkHorizontal(to);
        } else {
            link = from.linkVertical(to);
        }
        if (link != null) {
            link.setType(type);
        }
        return link;
    }

    /**
     * Returns tail node of the provided object
     *
     * @param obj
     *            The key of the object the vertex belongs to
     * @return The last vertex of obj
     */
    public TmfVertex getTail(Object obj) {
        List<TmfVertex> list = nodeMap.get(obj);
        if (!list.isEmpty()) {
            return list.get(list.size() - 1);
        }
        return null;
    }

    /**
     * Removes the last vertex of the provided object
     *
     * @param obj
     *            The key of the object the vertex belongs to
     * @return The removed vertex
     */
    public TmfVertex removeTail(Object obj) {
        List<TmfVertex> list = nodeMap.get(obj);
        if (!list.isEmpty()) {
            TmfVertex last = list.remove(list.size() - 1);
            reverse.remove(last);
            return last;
        }
        return null;
    }

    /**
     * Returns head node of the provided object. This is the very first node of
     * an object
     *
     * @param obj
     *            The key of the object the vertex belongs to
     * @return The head vertex
     */
    public TmfVertex getHead(Object obj) {
        Object ref = obj;
        if (obj == null) {
            ref = nodeMap.keySet().iterator().next();
        }
        List<TmfVertex> list = nodeMap.get(ref);
        if (!list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    /**
     * Returns the head node of the first object of the nodeMap
     *
     * @return The head vertex
     */
    public TmfVertex getHead() {
        return getHead(nodeMap.keySet().iterator().next());
    }

    /**
     * Returns head vertex from a given node. That is the first of the current
     * sequence of edges, the one with no left edge when going back through the
     * original vertex's left edge
     *
     * @param vertex
     *            The vertex for which to get the head
     * @return The head vertex from the requested vertex
     */
    public TmfVertex getHead(TmfVertex vertex) {
        TmfVertex headNode = vertex;
        while (headNode.inh() != null) {
            headNode = headNode.inh();
        }
        return headNode;
    }

    /**
     * Returns all nodes of the provided object.
     *
     * @param obj
     *            The key of the object the vertex belongs to
     * @return The list of vertices for the object
     */
    public List<TmfVertex> getNodesOf(Object obj) {
        return nodeMap.get(obj);
    }

    /**
     * Returns the object the vertex belongs to
     *
     * @param node
     *            The vertex to get the parent for
     * @return The object the vertex belongs to
     */
    public Object getParentOf(TmfVertex node) {
        return reverse.get(node);
    }

    /**
     * Returns the graph element
     *
     * @return The vertex map
     */
    public ArrayListMultimap<Object, TmfVertex> getNodesMap() {
        return nodeMap;
    }

    /**
     * Returns the number of vertices in the graph
     *
     * @return number of vertices
     */
    public int size() {
        return reverse.size();
    }

    @Override
    public String toString() {
        return String.format("Graph { actors=%d, nodes=%d }", //$NON-NLS-1$
                nodeMap.keySet().size(), nodeMap.values().size());
    }

    /**
     * Dumps the full graph
     *
     * @return A string with the graph dump
     */
    public String dump() {
        StringBuilder str = new StringBuilder();
        for (Object obj : nodeMap.keySet()) {
            str.append(String.format("%10s ", obj)); //$NON-NLS-1$
            str.append(nodeMap.get(obj));
            str.append("\n"); //$NON-NLS-1$
        }
        return str.toString();
    }

    // ----------------------------------------------
    // Graph operations and visits
    // ----------------------------------------------

    /**
     * Visits a graph from the start vertex and every vertex of the graph having
     * a path to/from them that intersects the start vertex
     *
     * Each time the worker changes, it goes back to the beginning of the
     * current horizontal sequence and visits all nodes from there.
     *
     * Parts of the graph that are totally disjoints from paths to/from start
     * will not be visited by this method
     *
     * @param start
     *            The vertex to start the scan for
     * @param visitor
     *            The visitor
     */
    public void scanLineTraverse(final TmfVertex start, final ITmfGraphVisitor visitor) {
        if (start == null) {
            return;
        }
        Stack<TmfVertex> stack = new Stack<>();
        HashSet<TmfVertex> visited = new HashSet<>();
        stack.add(start);
        while (!stack.isEmpty()) {
            TmfVertex curr = stack.pop();
            if (visited.contains(curr)) {
                continue;
            }
            // process one line
            TmfVertex n = getHead(curr);
            visitor.visitHead(n);
            while (n != null) {
                visitor.visit(n);
                // Only visit links up-right, guarantee to visit once only
                if (n.outv() != null) {
                    stack.push(n.outv());
                    visitor.visit(n.getEdges()[TmfVertex.OUTV], false);
                }
                if (n.inv() != null) {
                    stack.push(n.inv());
                }
                if (n.outh() != null) {
                    visitor.visit(n.getEdges()[TmfVertex.OUTH], true);
                }
                visited.add(n);
                n = n.outh();
            }
        }
    }

    /**
     * @see TmfGraph#scanLineTraverse(TmfVertex, ITmfGraphVisitor)
     *
     * @param start
     *            The worker from which to start the scan
     * @param visitor
     *            The visitor
     */
    public void scanLineTraverse(final Object start, final ITmfGraphVisitor visitor) {
        if (start == null) {
            return;
        }
        scanLineTraverse(getHead(start), visitor);
    }

    /**
     * Return the vertex for an object at a given timestamp, or the first vertex
     * after the timestamp
     *
     * @param startTime
     *            The desired time
     * @param obj
     *            The object for which to get the vertex
     * @return Vertex at timestamp or null if no vertex at or after timestamp
     */
    public TmfVertex getVertexAt(ITmfTimestamp startTime, Object obj) {
        List<TmfVertex> list = nodeMap.get(obj);

        long ts = startTime.getValue();
        // Scan the list until vertex is later than time
        for (TmfVertex vertex : list) {
            if (vertex.getTs() >= ts) {
                return vertex;
            }
        }
        return null;
    }

    /**
     * Returns whether the graph is completed or not
     *
     * @return whether the graph is done building
     */
    public boolean isDoneBuilding() {
        return finishedLatch.getCount() == 0;
    }

    /**
     * Countdown the latch to show that the graph is done building
     */
    public void closeGraph() {
        finishedLatch.countDown();
    }

}
