/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Master test suite for TMF Core.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    GraphAnalysisCorePluginTest.class,
    org.eclipse.linuxtools.tmf.analysis.graph.core.tests.analysis.criticalpath.AllTests.class,
    org.eclipse.linuxtools.tmf.analysis.graph.core.tests.graph.AllTests.class,
    org.eclipse.linuxtools.tmf.analysis.graph.core.tests.model.AllTests.class
})
public class AllAnalysisGraphCoreTests {

}

