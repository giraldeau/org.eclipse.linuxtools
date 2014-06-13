/*******************************************************************************
 * Copyright (c) 2013, 2014 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.analysis.graph.core.tests;

import static org.junit.Assert.assertEquals;

import org.eclipse.linuxtools.tmf.analysis.graph.core.Activator;
import org.junit.Test;

/**
 * Test the XML Analysis Core plug-in activator
 */
public class GraphAnalysisCorePluginTest {
    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    // Plug-in instantiation
    static final Activator fPlugin = new Activator();

    // ------------------------------------------------------------------------
    // Test cases
    // ------------------------------------------------------------------------

    /**
     * Test the plugin ID.
     */
    @Test
    public void testTmfCorePluginId() {
        assertEquals("Plugin ID", "org.eclipse.linuxtools.tmf.analysis.graph.core", Activator.PLUGIN_ID);
    }

    /**
     * Test the getDefault() static method.
     */
    @Test
    public void testGetDefault() {
        Activator plugin = Activator.getDefault();
        assertEquals("getDefault()", plugin, fPlugin);
    }
}
