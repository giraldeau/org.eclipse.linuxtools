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

package org.eclipse.linuxtools.tmf.core.event.matching;

import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.linuxtools.tmf.core.event.matching.TmfNetworkEventMatching.PacketKey;
import org.eclipse.linuxtools.tmf.core.request.TmfEventRequest;
import org.eclipse.linuxtools.tmf.core.synchronization.SyncAlgorithmFullyIncremental;
import org.eclipse.linuxtools.tmf.core.synchronization.SynchronizationAlgorithm.SyncQuality;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

/**
 * @since 3.1
 */
public class ExpireCleanupMonitor implements IMatchMonitor {

    private static final int threshold = 100;
    private int count;
    private long delay = 1000000000;
    private final LinkedList<PacketKey> queue = new LinkedList<>();
    private TmfNetworkEventMatching fParent; // FIXME: move removeKey() to base
                                             // class
    private TmfEventRequest fRequest;
    private Object fUF;

    public ExpireCleanupMonitor() {
        count = 0;
    }

    @Override
    public void init() {
        queue.clear();
    }

    @Override
    public void cacheMiss(PacketKey key) {
        count = count++ % threshold;
        queue.add(key);
        if (count == 0) {
            PacketKey last = queue.getLast();
            while ((last.getTs() - queue.peek().getTs()) > delay) {
                PacketKey pk = queue.poll();
                if (fParent != null) {
                    fParent.removeKey(pk);
                }
            }

        }
    }

    @Override
    public void cacheHit(TmfEventDependency dep) {
        if (fParent == null || fRequest == null) {
            return;
        }
        IMatchProcessingUnit unit = fParent.getProcessingUnit();
        if (unit instanceof SyncAlgorithmFullyIncremental) {
            SyncAlgorithmFullyIncremental sync = (SyncAlgorithmFullyIncremental) unit;
            if (fUF == null) {
                Collection<? extends ITmfTrace> traces = fParent.getTraces();
                fUF = new WeightedQuickUnion(traces.size());
            }

            SyncQuality quality = sync.getSynchronizationQuality(dep.getSourceEvent().getTrace(), dep.getDestinationEvent().getTrace());
            if (quality == SyncQuality.ACCURATE) {
                System.out.println("accurate!");
                fRequest.cancel();
            }
        }
    }

    @Override
    public void setParent(TmfNetworkEventMatching parent) {
        this.fParent = parent;
    }

    @Override
    public void setRequest(TmfEventRequest request) {
        this.fRequest = request;
    }
}
