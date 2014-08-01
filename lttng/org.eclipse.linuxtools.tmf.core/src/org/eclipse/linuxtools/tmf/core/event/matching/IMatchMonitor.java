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

import org.eclipse.linuxtools.tmf.core.event.matching.TmfNetworkEventMatching.PacketKey;
import org.eclipse.linuxtools.tmf.core.request.TmfEventRequest;

/**
 * The match monitor is responsible for cleanup of packet matching and to stop
 * the request if some condition occurs.
 *
 * @since 3.1
 */
public interface IMatchMonitor {
    public void init();

    /**
     * Item is queued for match
     * @param key the packet key enqueued
     */
    public void cacheMiss(PacketKey key);

    /**
     * A match was found
     */
    public void cacheHit(TmfEventDependency dep);

    public void setParent(TmfNetworkEventMatching tmfNetworkEventMatching);

    public void setRequest(TmfEventRequest request);
}
