package org.eclipse.linuxtools.tmf.core.synchronization;

public class Edge<V, E> {

    private final V from;
    private final V to;
    private final E label;

    public Edge(V from, V to, E label) {
        this.from = from;
        this.to = to;
        this.label = label;
    }

    public V getFrom() {
        return from;
    }

    public V getTo() {
        return to;
    }

    public E getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return "(" + from + "," + to + "," + label + ")";
    }
}
