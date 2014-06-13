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
import java.util.List;

import org.eclipse.linuxtools.tmf.analysis.graph.core.base.Messages;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelRegistry;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

/**
 * @since 3.0
 */
public abstract class AbstractTmfGraphProvider implements ITmfGraphProvider {

    private final ITmfTrace fTrace;
    private final Class<? extends ITmfEvent> fEventType;

    private boolean fGraphAssigned;

    private AnalysisPhase fCurrentPhase;

    /** Graph in which to insert the state changes */
    private TmfGraph fGraph = null;

    private final TmfModelRegistry fRegistry;

    /**
     * Instantiate a new graph builder plugin.
     *
     * @param registry
     *            The model registry
     * @param trace
     *            The trace
     * @param eventType
     *            The specific class for the event type that will be used within
     *            the subclass
     * @param id
     *            Name given to this state change input. Only used internally.
     */
    public AbstractTmfGraphProvider(TmfModelRegistry registry, ITmfTrace trace,
            Class<? extends ITmfEvent> eventType, String id) {
        this.fTrace = trace;
        this.fEventType = eventType;
        if (registry == null) {
            fRegistry = new TmfModelRegistry();
        } else {
            fRegistry = registry;
        }
        fGraphAssigned = false;
    }

    @Override
    public ITmfTrace getTrace() {
        return fTrace;
    }

    @Override
    public long getStartTime() {
        return fTrace.getStartTime().normalize(0, ITmfTimestamp.NANOSECOND_SCALE).getValue();
    }

    @Override
    public Class<? extends ITmfEvent> getExpectedEventType() {
        return fEventType;
    }

    /**
     * Get the model registry that goes with the graph
     *
     * @return The model registry
     */
    public TmfModelRegistry getModelRegistry() {
        return fRegistry;
    }

    @Override
    public void assignTargetGraph(TmfGraph graph) {
        fGraph = graph;
        fGraphAssigned = true;
    }

    @Override
    public TmfGraph getAssignedGraph() {
        return fGraph;
    }

    @Override
    public AnalysisPhase getCurrentPhase() {
        return fCurrentPhase;
    }

    @Override
    public void setCurrentPhase(AnalysisPhase phase) {
        fCurrentPhase = phase;
    }

    @Override
    public List<AnalysisPhase> makeAnalysis() {
        List<AnalysisPhase> phases = new ArrayList<>();
        phases.add(new AnalysisPhase(Messages.TmfGraphBuilder_BuildingGraph,
                new TmfGraphBuildRequest(this)));
        return phases;
    }

    @Override
    public void processEvent(ITmfEvent event) {
        /* Make sure the target graph has been assigned */
        if (!fGraphAssigned) {
            System.err.println("Cannot process event without a target graph"); //$NON-NLS-1$
            return;
        }
        eventHandle(event);
    }

    @Override
    public void dispose() {
        fGraphAssigned = false;
        fGraph = null;
    }

    /**
     * Event handler
     *
     * @param ev
     *            The event
     */
    protected abstract void eventHandle(ITmfEvent ev);

    @Override
    public void done() {
    }

    /**
     * Internal event handler, using the phase's handlers
     *
     * @param eventName
     *            Name of the event
     * @param event
     *            The event
     */
    protected void eventHandle(String eventName, ITmfEvent event) {
        for (ITraceEventHandler handler : getCurrentPhase().getHandlers(eventName)) {
            handler.handleEvent(event);
        }
    }

    @Override
    public void handleCancel() {
    }

}
