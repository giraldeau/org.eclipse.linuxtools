package org.eclipse.linuxtools.tmf.core.synchronization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
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
 * V is the vertex type and E is the label (weight) type.
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 */
public class SyncGraph<V, E> {

    private Multimap<V, Edge<V, E>> adj;
    private Set<V> vertices;

    public SyncGraph() {
        adj = ArrayListMultimap.create();
        vertices = new HashSet<>();
    }

    public void addEdge(V v, V w, E label) {
        adj.put(v, new Edge<>(v, w, label));
        vertices.add(v);
        vertices.add(w);
    }

    public int E() {
        return adj.entries().size();
    }

    public int V() {
        return vertices.size();
    }

    public Collection<Edge<V, E>> adj(V v) {
        return adj.get(v);
    }

    public Collection<Edge<V, E>> path(V start, V end) {
        // TODO
        ArrayList<Edge<V, E>> path = new ArrayList<>();
        System.out.println(start + " " + end);
        return path;
    }

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
