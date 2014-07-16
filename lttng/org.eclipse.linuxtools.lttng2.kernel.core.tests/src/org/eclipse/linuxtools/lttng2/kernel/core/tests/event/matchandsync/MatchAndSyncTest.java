/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial implementation
 *******************************************************************************/

package org.eclipse.linuxtools.lttng2.kernel.core.tests.event.matchandsync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.linuxtools.lttng2.kernel.core.event.matching.TcpEventMatching;
import org.eclipse.linuxtools.lttng2.kernel.core.event.matching.TcpLttngEventMatching;
import org.eclipse.linuxtools.tmf.core.event.matching.IMatchProcessingUnit;
import org.eclipse.linuxtools.tmf.core.event.matching.TmfEventMatching;
import org.eclipse.linuxtools.tmf.core.event.matching.TmfNetworkEventMatching;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfTrace;
import org.eclipse.linuxtools.tmf.ctf.core.tests.shared.CtfTmfTestTrace;
import org.junit.Test;

/**
 * Tests for {@link TcpEventMatching}
 *
 * @author Geneviève Bastien
 */
@SuppressWarnings("nls")
public class MatchAndSyncTest {

    /**
     * Testing the packet matching
     * @throws Exception exception
     */
    @Test
    public void testMatching() throws Exception {
        assumeTrue(CtfTmfTestTrace.SYNC_SRC.exists());
        assumeTrue(CtfTmfTestTrace.SYNC_DEST.exists());
        try (CtfTmfTrace trace1 = CtfTmfTestTrace.SYNC_SRC.getTrace();
                CtfTmfTrace trace2 = CtfTmfTestTrace.SYNC_DEST.getTrace();) {

            List<ITmfTrace> tracearr = new LinkedList<>();
            tracearr.add(trace1);
            tracearr.add(trace2);

            TmfEventMatching.registerMatchObject(new TcpEventMatching());
            TmfEventMatching.registerMatchObject(new TcpLttngEventMatching());

            TmfNetworkEventMatching twoTraceMatch = new TmfNetworkEventMatching(tracearr);
            assertTrue(twoTraceMatch.matchEvents());

            // Access protected method of parent class to get the number of matches
            Method m = twoTraceMatch
                    .getClass()
                    .getSuperclass()
                    .getDeclaredMethod("getProcessingUnit");
            m.setAccessible(true);
            IMatchProcessingUnit unit = (IMatchProcessingUnit) m.invoke(twoTraceMatch);
            assertEquals(46, unit.countMatches());
        }

    }

}
