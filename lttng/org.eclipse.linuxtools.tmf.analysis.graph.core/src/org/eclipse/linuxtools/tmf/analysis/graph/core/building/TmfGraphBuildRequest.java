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

package org.eclipse.linuxtools.tmf.analysis.graph.core.building;

import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.request.ITmfEventRequest;
import org.eclipse.linuxtools.tmf.core.request.TmfEventRequest;
import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;

/**
 * @since 3.0
 */
public class TmfGraphBuildRequest extends TmfEventRequest {

    private final ITmfGraphProvider fProvider;

    private ITmfGraphBuildingListener fListener;

    /**
     * Constructor
     *
     * @param provider
     *            The graph provider
     */
    public TmfGraphBuildRequest(ITmfGraphProvider provider) {
        super(provider.getExpectedEventType(),
                TmfTimeRange.ETERNITY,
                0,
                ITmfEventRequest.ALL_DATA,
                ITmfEventRequest.ExecutionType.BACKGROUND);

        this.fProvider = provider;
        setDefaultListener();
    }

    @Override
    public void handleData(final ITmfEvent event) {
        super.handleData(event);
        if (event != null) {
            fListener.progress(event.getTimestamp().getValue());
            fProvider.processEvent(event);
        }
    }

    @Override
    public void done() {
        super.done();
        fProvider.done();
    }

    @Override
    public void handleCancel() {
        fProvider.handleCancel();
        super.handleCancel();
    }



    @Override
	public synchronized boolean isCancelled() {
        if (super.isCancelled()) {
            return true;
        }
		return fProvider.getCurrentPhase().isCancelled();
	}

	/**
     * Get the listener of this request
     *
     * @return The graph building listener
     */
    public ITmfGraphBuildingListener getListener() {
        return fListener;
    }

    /**
     * Sets the listener on this request
     *
     * @param listener
     *            graph building listener
     */
    public void setListener(ITmfGraphBuildingListener listener) {
        this.fListener = listener;
        if (this.fListener == null) {
            setDefaultListener();
        }
    }

    private void setDefaultListener() {
        this.fListener = new ITmfGraphBuildingListener() {

            @Override
            public void begin() {

            }

            @Override
            public void progress(long time) {
            }

            @Override
            public void phase(int phase) {

            }

            @Override
            public void finished() {

            }

            @Override
            public boolean isCanceled() {
                return false;
            }

            @Override
            public void setTimeRange(ITmfTimestamp startTime, ITmfTimestamp endTime) {

            }

        };
    }
}
