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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to define a filter (search) on element models
 *
 * @since 3.0
 */
public class TmfModelElementFilter {

    private final Map<String, Object> fFilters = new HashMap<>();
    private final TmfModelElementDeclaration fDeclaration;

    /**
     * Constructor
     *
     * @param declaration The declaration of the model element to search
     */
    public TmfModelElementFilter(TmfModelElementDeclaration declaration) {
        fDeclaration = declaration;
    }

    /**
     * Sets the value of a field to filter
     *
     * @param field The field on which to filter
     * @param value The value
     */
    public void setValue(String field, Object value) {
        if (!fDeclaration.getFieldsList().contains(field)) {
            throw new RuntimeException(String.format("Tried to assign field %s to filter of type %s but this field is not allowed", field, fDeclaration.getName())); //$NON-NLS-1$
        }
        fFilters.put(field, value);
    }

    /**
     * Gets the list of filtered fields
     *
     * @return The list if filters
     */
    public Map<String, Object> getFilters() {
        return Collections.unmodifiableMap(fFilters);
    }

    /**
     * Get the model element declaration this filter applies to
     *
     * @return The model element declaration
     */
    public TmfModelElementDeclaration getDeclaration() {
        return fDeclaration;
    }
}
