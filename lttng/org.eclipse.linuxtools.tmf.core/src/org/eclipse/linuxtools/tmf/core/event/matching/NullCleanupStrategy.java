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

/**
 * @since 4.0
 */
public class NullCleanupStrategy extends AbstractMatchMonitor {

    @Override
    public void init() {
    }

    @Override
    public void cacheMiss(PacketKey key) {
    }

    @Override
    public void cacheHit(TmfEventDependency dep) {
    }

}
