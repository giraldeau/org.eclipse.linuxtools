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
 * An element declaration specific for model resources
 * @since 3.0
 */
public class TmfModelResourceDeclaration extends TmfModelElementDeclaration {

    /**
     * Constructor
     *
     * @param name
     *            The name of this element declaration
     */
    public TmfModelResourceDeclaration(String name) {
        super(name);
    }

    @Override
    public TmfModelResource create() {
        return new TmfModelResource(this);
    }

}
