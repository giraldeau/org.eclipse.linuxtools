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

import org.eclipse.osgi.util.NLS;

/**
 * Packages external string files
 *
 * @since 3.0
 */
@SuppressWarnings("javadoc")
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.linuxtools.tmf.analysis.graph.core.base.messages"; //$NON-NLS-1$

    public static String TmfGraph_FromNotInGraph;

    public static String TmfGraph_KeyNull;

    public static String TmfGraphBuilder_BuildingGraph;

    public static String TmfGraphBuilder_CancellingJob;

    public static String TmfVertex_ArgumentTimestampLower;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
