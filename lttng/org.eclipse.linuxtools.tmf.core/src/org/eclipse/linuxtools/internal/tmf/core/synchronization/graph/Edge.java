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

/**
 * An edge in the {@link SyncGraph}
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 * @param <V>
 *            The vertices type
 * @param <E>
 *            The edge annotation type
 */
public class Edge<V, E> {

    private final V from;
    private final V to;
    private final E label;

    /**
     * An edge constructor
     *
     * @param from
     *            The origin vertex
     * @param to
     *            The destination vertex
     * @param label
     *            The edge annotation label
     */
    public Edge(V from, V to, E label) {
        this.from = from;
        this.to = to;
        this.label = label;
    }

    /**
     * Get the vertex from
     *
     * @return The origin vertex
     */
    public V getFrom() {
        return from;
    }

    /**
     * Get the vertex to
     *
     * @return The destination vertex
     */
    public V getTo() {
        return to;
    }

    /**
     * Get the edge label
     *
     * @return The edge label
     */
    public E getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return String.format("(%s, %s, %s)", from, to, label); //$NON-NLS-1$
    }
}
