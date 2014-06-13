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

package org.eclipse.linuxtools.tmf.analysis.graph.core.model;

/**
 * Interface for elements of the trace model.
 *
 * @since 3.0
 */
public interface ITmfModelElement {

    /**
     * Checks whether two model elements are the same
     *
     * FIXME: It is not the same as equals, there was a reason why I did this,
     * but I don't recall and this function is not used...
     *
     * @param other
     *            The other model element to compare to
     * @return whether two model elements are the same
     */
    public boolean isSame(ITmfModelElement other);

    /**
     * Applies a filter to a model element and returns whether the filter
     * applies. This function can be used to search for an element using a
     * filter
     *
     * @param filter
     *            The filter to apply
     * @return true if the filter applies
     */
    public boolean filter(TmfModelElementFilter filter);
}
