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

import java.util.LinkedList;

import org.eclipse.linuxtools.tmf.core.event.matching.TmfNetworkEventMatching.PacketKey;

/**
 * @since 4.0
 */
public class ExpireCleanupMonitor extends AbstractMatchMonitor {

    private static final int threshold = 100;
    private int count;
    private long delay = 1000000000; // FIXME: should be 6 sigma from the average delay
    private final LinkedList<PacketKey> queue = new LinkedList<>();

    @Override
    public void init() {
        count = 0;
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
                if (getParent() != null) {
                    getParent().removeKey(pk);
                }
            }

        }
    }

}
