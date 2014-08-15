/*******************************************************************************
 * Copyright (c) 2014 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francis Giraldeau - Initial implementation and API
 *******************************************************************************/

package org.eclipse.linuxtools.internal.tmf.core.synchronization.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Minimal graph implementation to compute timestamps transforms of a trace from
 * a given synchronized set of traces. The graph is implemented as an adjacency
 * list and is directed. To create undirected graph, add the edge in both
 * directions.
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 * @param <V>
 *            The vertices type
 * @param <E>
 *            The edge annotation type
 */
public class SyncGraph<V, E> {

    private Multimap<V, Edge<V, E>> adj;
    private Set<V> vertices;

    /**
     * Construct empty graph
     */
    public SyncGraph() {
        adj = ArrayListMultimap.create();
        vertices = new HashSet<>();
    }

    /**
     * Add edge from v to w and annotation label
     *
     * @param v
     *            from vertex
     * @param w
     *            to vertex
     * @param label
     *            the edge label
     */
    public void addEdge(V v, V w, E label) {
        adj.put(v, new Edge<>(v, w, label));
        vertices.add(v);
        vertices.add(w);
    }

    /**
     * Number of edges
     *
     * @return number of edges
     */
    public int E() {
        return adj.entries().size();
    }

    /**
     * Number of vertices
     *
     * @return number of vertices
     */
    public int V() {
        return vertices.size();
    }

    /**
     * Returns the adjacent edges of the given vertex
     *
     * @param v
     *            the vertex
     * @return the adjacent vertices
     */
    public Collection<Edge<V, E>> adj(V v) {
        return adj.get(v);
    }

    /**
     * Returns a path between start and end vertices.
     *
     * @param start
     *            vertex
     * @param end
     *            vertex
     * @return the list of edges between start and end vertices
     */
    public List<Edge<V, E>> path(V start, V end) {
        ArrayList<Edge<V, E>> path = new ArrayList<>();
        HashMap<V, Edge<V, E>> hist = new HashMap<>();
        HashSet<V> visited = new HashSet<>();
        Queue<V> queue = new LinkedList<>();
        queue.offer(start);
        while (!queue.isEmpty()) {
            V node = queue.poll();
            visited.add(node);
            for (Edge<V, E> e : adj(node)) {
                V to = e.getTo();
                if (!visited.contains(to)) {
                    queue.offer(e.getTo());
                    if (!hist.containsKey(e.getTo())) {
                        hist.put(e.getTo(), e);
                    }
                }
            }
        }
        V node = end;
        while (true) {
            Edge<V, E> edge = hist.get(node);
            if (edge == null) {
                break;
            }
            path.add(edge);
            node = edge.getFrom();
            if (node == start) {
                break;
            }
        }
        Collections.reverse(path);
        return path;
    }

    /**
     * Check if this graph is connected (no partitions)
     *
     * @return true if the graph is connected, false otherwise
     */
    public boolean isConnected() {
        HashSet<V> visited = new HashSet<>();
        Stack<V> stack = new Stack<>();
        stack.push(vertices.iterator().next());
        while (!stack.isEmpty()) {
            V node = stack.pop();
            visited.add(node);
            for (Edge<V, E> edge : adj(node)) {
                if (!visited.contains(edge.getTo())) {
                    stack.push(edge.getTo());
                }
            }
        }
        return visited.size() == vertices.size();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (V key : adj.keySet()) {
            str.append(key + ": " + adj.get(key) + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return str.toString();
    }

}
