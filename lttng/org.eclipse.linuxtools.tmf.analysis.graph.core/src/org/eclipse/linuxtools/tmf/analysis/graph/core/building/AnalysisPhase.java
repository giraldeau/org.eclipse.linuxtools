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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.linuxtools.tmf.analysis.graph.core.Activator;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Class to describe an analysis phase and how it handles data
 *
 * @since 3.0
 */
public class AnalysisPhase {

    private final String fName;
    private final TmfGraphBuildRequest fRequest;
    private final Multimap<String, ITraceEventHandler> fHandlers;
    private final List<ITraceEventHandler> fHandlerList;

    /**
     * Constructor
     *
     * @param name
     *            The name of the phase
     * @param handler
     *            A request to be run
     */
    public AnalysisPhase(String name, TmfGraphBuildRequest handler) {
        fName = name;
        fRequest = handler;
        fHandlers = ArrayListMultimap.create();
        fHandlerList = new ArrayList<>();
    }

    /**
     * Gets the name of the phase
     *
     * @return The name of the phase
     */
    public String getName() {
        return this.fName;
    }

    /**
     * Gets the request associated with this analysis phase
     *
     * @return The graph build request
     */
    public TmfGraphBuildRequest getRequest() {
        return this.fRequest;
    }

    /**
     * Cancel the request
     */
    public void cancel() {
        fRequest.cancel();
    }

    /**
     * Cancels the request with exception
     *
     * @param e
     *            The exception causing the cancellation
     */
    public void cancel(Exception e) {
        fRequest.cancel();
        Activator.logError(String.format("Cancelling phase %s because an exception occurred", getName()), e); //$NON-NLS-1$
    }

    /**
     * Gets the listener associated with the request
     *
     * @return The request listener
     */
    public ITmfGraphBuildingListener getListener() {
        return fRequest.getListener();
    }

    /**
     * Sets a listener to the request
     *
     * @param listener
     *            A listener for the request
     */
    public void setListener(ITmfGraphBuildingListener listener) {
        fRequest.setListener(listener);
    }

    /**
     * Register a handler to a series of events
     *
     * @param events
     *            The array of events for which to add the handler
     * @param handler
     *            The trace event handler
     */
    public void registerHandler(String[] events, ITraceEventHandler handler) {
        for (String event : events) {
            fHandlers.put(event, handler);
        }
        fHandlerList.add(handler);
    }

    /**
     * Returns the handlers for the given event
     *
     * @param eventName
     *            The name of the event
     * @return The list of handlers
     */
    public Collection<ITraceEventHandler> getHandlers(String eventName) {
        return fHandlers.get(eventName);
    }

	public boolean isCancelled() {
		for (ITraceEventHandler handler: fHandlerList) {
		    if (!handler.isCancelled()) {
		        return false;
		    }
		}
		return true;
	}

}
