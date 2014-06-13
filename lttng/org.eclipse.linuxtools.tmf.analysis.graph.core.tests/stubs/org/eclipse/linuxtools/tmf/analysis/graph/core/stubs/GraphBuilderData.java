/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien & Francis Giraldeau- Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.analysis.graph.core.stubs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfVertex;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfEdge.EdgeType;

/**
 *
 *
 * @author Francis Giraldeau
 */
@SuppressWarnings("javadoc")
public class GraphBuilderData {

    public TmfVertex head;
    public TmfVertex bounded;
    public TmfVertex unbounded;
    public int id = 0;
    public int len = 0;
    public int num = 0;
    public int depth = 0;
    public long delay = 0;
    public List<EdgeType> types = new ArrayList<>();

}