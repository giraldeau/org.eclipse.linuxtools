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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Base class for model elements with fields. A model element is defined through
 * a modelElementDeclaration which specifies the fields of this element, which
 * fields are used to identify this element and which can be used to pair
 * another element.
 *
 * @since 3.0
 */
public class TmfAbstractModelElement implements ITmfModelElement, Comparable<TmfAbstractModelElement> {

    /* This object's fields */
    private final Map<String, Object> fFields;

    /* The list of old values for values that need to be backed up */
    private final Map<String, Object> fOldValues = new HashMap<>();

    /* The model element declaration */
    private final TmfModelElementDeclaration fDeclaration;

    /**
     * Constructor
     *
     * @param declaration
     *            The element declaration describing this element
     */
    public TmfAbstractModelElement(TmfModelElementDeclaration declaration) {
        fFields = new HashMap<>();
        fDeclaration = declaration;

        for (String field : declaration.getFieldsList()) {
            fFields.put(field, null);
        }
    }

    /**
     * Copy constructor
     *
     * @param element
     *            The element to copy
     */
    public TmfAbstractModelElement(TmfAbstractModelElement element) {
        fDeclaration = element.fDeclaration;
        fFields = new HashMap<>();

        for (String field : fDeclaration.getFieldsList()) {
            if (element.getField(field) != null) {
                fFields.put(field, element.getField(field));
            } else {
                fFields.put(field, null);
            }
        }
    }

    /**
     * Sets the value of a field.
     *
     * @param name
     *            The name of the field to set
     * @param value
     *            The value
     */
    public void setField(String name, Object value) {
        if (!fDeclaration.getFieldsList().contains(name)) {
            throw new RuntimeException(String.format("Tried to assign field %s to model resource of type %s but this field is not allowed", name, fDeclaration.getName())); //$NON-NLS-1$
        }
        /* Keep the last value of this field, if necessary */
        if (fDeclaration.getKeepFieldsList().contains(name) && fFields.containsKey(name)) {
            fOldValues.put(name, fFields.get(name));
        }
        fFields.put(name, value);

    }

    /**
     * Gets the value of a field
     *
     * @param name
     *            The field name
     * @return The value of the field
     */
    public Object getField(String name) {
        if (fFields.containsKey(name)) {
            return fFields.get(name);
        }
        return null;
    }

    /**
     * Gets the backed up value of a field. Throws exception is field is not
     * declared to keep previous value
     *
     * @param name
     *            Name of the field for which to get the value
     * @return The backed up value of a field
     */
    public Object getOldValue(String name) {
        if (!fDeclaration.getKeepFieldsList().contains(name)) {
            throw new RuntimeException(String.format("Tried to get the previous value of field %s to model resource of type %s but this field is not set to had its old value kept", name, fDeclaration.getName())); //$NON-NLS-1$
        }
        return fOldValues.get(name);
    }

    /**
     * Gets the declaration of this model element
     *
     * @return The model element declaration
     */
    public TmfModelElementDeclaration getDeclaration() {
        return fDeclaration;
    }

    @Override
    public boolean isSame(ITmfModelElement other) {
        boolean result = false;
        if (other instanceof TmfAbstractModelElement) {
            TmfAbstractModelElement otherres = (TmfAbstractModelElement) other;
            /* Only fields set to be equal in declaration need be equal */
            List<String> idfields = fDeclaration.getSameFieldsList();

            /* If no field set as id, then all fields must be equal */
            if (idfields.isEmpty()) {
                idfields = fDeclaration.getFieldsList();
            }
            result = true;
            for (String field : idfields) {
                if (!fFields.get(field).equals(otherres.getField(field))) {
                    result = false;
                }
            }
        }
        return result;
    }

    @Override
    public boolean filter(TmfModelElementFilter filter) {
        /* Is it the type we are looking for? */
        if (fDeclaration.getName().equals(filter.getDeclaration().getName())) {
            for (Entry<String, Object> entry : filter.getFilters().entrySet()) {
                if (fFields.containsKey(entry.getKey())) {
                    if (fFields.get(entry.getKey()) != null) {
                        if (!fFields.get(entry.getKey()).equals(entry.getValue())) {
                            return false;
                        }
                    } else if (entry.getValue() != null) {
                        /* value is null but not filter value */
                        return false;
                    }
                } else {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int compareTo(TmfAbstractModelElement other) {
        int result = 0;

        if (fDeclaration.equals(other.getDeclaration())) {
            for (String field : fDeclaration.getSameFieldsList()) {
                Object field1 = fFields.get(field);
                if (field1 == null) {
                    if (other.getField(field) != null) {
                        result = -1;
                    }
                }
                if (result != 0) {
                    break;
                }
            }
        }

        return result;
    }

}
