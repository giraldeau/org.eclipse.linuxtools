/*******************************************************************************
 * Copyright (c) 2014 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building;

import org.eclipse.osgi.util.NLS;

/**
 * Externalized string for the o.e.l.lttng2.kernel.core.graph.building package
 * @since 4.0
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.linuxtools.lttng2.kernel.core.graph.building.messages"; //$NON-NLS-1$
    /** Phase 1 title */
    public static String LttngKernelExecGraphProvider_AnalysePhase1;
    /** Phase 2 title */
    public static String LttngKernelExecGraphProvider_AnalysePhase2;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
