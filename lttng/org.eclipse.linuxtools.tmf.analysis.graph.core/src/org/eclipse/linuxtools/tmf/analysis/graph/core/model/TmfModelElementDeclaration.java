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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Describe a model element: its fields, fields that identify this element,
 * fields for which to keep a backup of the value (because the old value may be
 * of use) and fields used to match another element.
 *
 * @since 3.0
 */
public abstract class TmfModelElementDeclaration {

    /* List of all fields */
    private final List<String> fFieldsList = new ArrayList<>();

    /* Identification fields */
    private final List<String> fSameFields = new ArrayList<>();

    /* Fields to backup */
    private final List<String> fKeepFields = new ArrayList<>();

    /* Fields used to match another element */
    private final Map<String, String> fComplementaryFields = new HashMap<>();

    /* Name of the declaration */
    private final String fName;

    /**
     * Constructor
     *
     * @param name
     *            The name of this element
     */
    public TmfModelElementDeclaration(String name) {
        fName = name;
    }

    /**
     * Creates a model resource for this declaration
     *
     * @return A new model resource
     */
    public abstract TmfAbstractModelElement create();

    /**
     * Get the list of fields for this declaration
     *
     * @return The list of all fields
     */
    public List<String> getFieldsList() {
        return Collections.unmodifiableList(fFieldsList);
    }

    /**
     * Gets the list of field that identify this element
     *
     * @return list of fields
     */
    public List<String> getSameFieldsList() {
        return Collections.unmodifiableList(fSameFields);
    }

    /**
     * Gets the list of fields for which to back up value
     *
     * @return list of fields
     */
    public List<String> getKeepFieldsList() {
        return Collections.unmodifiableList(fKeepFields);
    }

    /**
     * Gets the name of this element declaration
     *
     * @return The name of the declaration
     */
    public String getName() {
        return fName;
    }

    /**
     * Add a field to the list of fields
     *
     * @param field
     *            The field name
     */
    public void addField(String field) {
        fFieldsList.add(field);
    }

    /**
     * Add a field that identifies this element. Also adds it to the list of all
     * fields
     *
     * @param field
     *            The field name
     */
    public void addSameField(String field) {
        addField(field);
        fSameFields.add(field);
    }

    /**
     * Add a field for which a backup of the value will be done. Also adds it to
     * the list of all fields
     *
     * @param field
     *            The field name
     */
    public void addKeepField(String field) {
        addField(field);
        fKeepFields.add(field);
    }

    /**
     * Add a field mapping between two elements of the same type to find a pair
     * of elements. Fields must already be declared.
     *
     * @param field1
     *            The first field to match
     * @param field2
     *            The second field to match
     */
    public void addComplementFields(String field1, String field2) {
        if ((!fFieldsList.contains(field1)) || (!fFieldsList.contains(field2))) {
            throw new RuntimeException(String.format("Tried to add complementary fields to declaration %s, but either %s or %s is not an available field", fName, field1, field2)); //$NON-NLS-1$
        }
        fComplementaryFields.put(field1, field2);
    }
}
