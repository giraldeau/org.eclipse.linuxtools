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

/**
 * An empty implementation of the graph visitor
 *
 * @since 3.0
 */
public class TmfGraphVisitor implements ITmfGraphVisitor {

    @Override
    public void visitHead(TmfVertex node) {

    }

    @Override
    public void visit(TmfVertex node) {

    }

    @Override
    public void visit(TmfEdge edge, boolean horizontal) {

    }

}
