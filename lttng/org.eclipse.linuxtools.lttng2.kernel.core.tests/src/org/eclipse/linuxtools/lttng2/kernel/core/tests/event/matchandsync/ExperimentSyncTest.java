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
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import org.eclipse.linuxtools.lttng2.kernel.core.event.matching.TcpEventMatching;
import org.eclipse.linuxtools.lttng2.kernel.core.event.matching.TcpLttngEventMatching;
import org.eclipse.linuxtools.tmf.core.event.matching.TmfEventMatching;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.synchronization.ITmfTimestampTransform;
import org.eclipse.linuxtools.tmf.core.synchronization.SyncAlgorithmFullyIncremental;
import org.eclipse.linuxtools.tmf.core.synchronization.SynchronizationAlgorithm;
import org.eclipse.linuxtools.tmf.core.synchronization.SynchronizationAlgorithm.SyncQuality;
import org.eclipse.linuxtools.tmf.core.synchronization.TmfTimestampTransform;
import org.eclipse.linuxtools.tmf.core.synchronization.TmfTimestampTransformLinear;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfTrace;
import org.eclipse.linuxtools.tmf.ctf.core.tests.shared.CtfTmfTestTrace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for experiment syncing
 *
 * @author Geneviève Bastien
 */
@SuppressWarnings("nls")
public class ExperimentSyncTest {

    private static final String EXPERIMENT   = "MyExperiment";
    private static int          BLOCK_SIZE   = 1000;

    private static ITmfTrace[] fTraces;
    private static TmfExperiment fExperiment;

    /**
     * Setup the traces and experiment
     */
    @Before
    public void setUp() {
        assumeTrue(CtfTmfTestTrace.SYNC_SRC.exists());
        assumeTrue(CtfTmfTestTrace.SYNC_DEST.exists());

        fTraces = new CtfTmfTrace[2];
        fTraces[0] = CtfTmfTestTrace.SYNC_SRC.getTrace();
        fTraces[1] = CtfTmfTestTrace.SYNC_DEST.getTrace();

        fExperiment = new TmfExperiment(fTraces[0].getEventType(), EXPERIMENT, fTraces, BLOCK_SIZE);

        TmfEventMatching.registerMatchObject(new TcpEventMatching());
        TmfEventMatching.registerMatchObject(new TcpLttngEventMatching());
    }

    /**
     * Reset the timestamp transforms on the traces
     */
    @After
    public void cleanUp() {
        fTraces[0].setTimestampTransform(TmfTimestampTransform.IDENTITY);
        fTraces[1].setTimestampTransform(TmfTimestampTransform.IDENTITY);
        fTraces[0].dispose();
        fTraces[1].dispose();
    }

    /**
     * Testing experiment synchronization
     */
    @Test
    public void testExperimentSync() {
        try {
            SynchronizationAlgorithm syncAlgo = fExperiment.synchronizeTraces(true);

            ITmfTimestampTransform tt1, tt2;

            tt1 = syncAlgo.getTimestampTransform(fTraces[0]);
            tt2 = syncAlgo.getTimestampTransform(fTraces[1]);

            fTraces[0].setTimestampTransform(tt1);
            fTraces[1].setTimestampTransform(tt2);

            assertEquals(tt2, TmfTimestampTransform.IDENTITY);
            TmfTimestampTransformLinear tt1x = (TmfTimestampTransformLinear) tt1;
            assertEquals(1.0, tt1x.getAlpha().doubleValue(), 0.001);
            assertEquals(79796507913179.0, tt1x.getBeta().doubleValue(), 1);

            assertEquals(syncAlgo.getTimestampTransform(fTraces[0].getHostId()),fTraces[0].getTimestampTransform());
            assertEquals(syncAlgo.getTimestampTransform(fTraces[1].getHostId()),fTraces[1].getTimestampTransform());

        } catch (TmfTraceException e) {
            fail("Exception thrown in experiment synchronization " + e.getMessage());
        }
    }

    /**
     * Test sync traces on Django experiment
     * @throws TmfTraceException forward exception thrown by the synchronize method
     */
    @Test
    public void testDjangoSync() throws TmfTraceException {
        assumeTrue(CtfTmfTestTrace.DJANGO_CLIENT.exists());
        assumeTrue(CtfTmfTestTrace.DJANGO_DB.exists());
        assumeTrue(CtfTmfTestTrace.DJANGO_HTTPD.exists());
        try (CtfTmfTrace client = CtfTmfTestTrace.DJANGO_CLIENT.getTrace();
                CtfTmfTrace db = CtfTmfTestTrace.DJANGO_DB.getTrace();
                CtfTmfTrace httpd = CtfTmfTestTrace.DJANGO_HTTPD.getTrace();) {
            TmfExperiment experiment = new TmfExperiment(CtfTmfEvent.class,
                    "Test experiment", new CtfTmfTrace[] { client, db, httpd }, 1000);
            SyncAlgorithmFullyIncremental syncAlgo = (SyncAlgorithmFullyIncremental) experiment.synchronizeTraces(true);
            assertEquals(SyncQuality.ABSENT, syncAlgo.getSynchronizationQuality(client, db));
            assertEquals(SyncQuality.ACCURATE, syncAlgo.getSynchronizationQuality(client, httpd));
            assertEquals(SyncQuality.ACCURATE, syncAlgo.getSynchronizationQuality(httpd, db));
        }
    }

}
