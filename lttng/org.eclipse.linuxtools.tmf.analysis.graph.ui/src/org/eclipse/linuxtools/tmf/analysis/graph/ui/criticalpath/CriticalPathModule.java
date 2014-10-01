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

package org.eclipse.linuxtools.tmf.analysis.graph.ui.criticalpath;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfVertex;
import org.eclipse.linuxtools.tmf.analysis.graph.core.building.TmfGraphBuilderModule;
import org.eclipse.linuxtools.tmf.analysis.graph.core.criticalpath.CriticalPathAlgorithmBounded;
import org.eclipse.linuxtools.tmf.analysis.graph.core.criticalpath.CriticalPathAlgorithmUnbounded;
import org.eclipse.linuxtools.tmf.analysis.graph.core.criticalpath.ICriticalPathAlgorithm;
import org.eclipse.linuxtools.tmf.analysis.graph.core.criticalpath.Messages;
import org.eclipse.linuxtools.tmf.analysis.graph.ui.Activator;
import org.eclipse.linuxtools.tmf.analysis.graph.ui.criticalpath.view.CriticalPathView;
import org.eclipse.linuxtools.tmf.core.analysis.IAnalysisModule;
import org.eclipse.linuxtools.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfTraceManager;
import org.eclipse.linuxtools.tmf.ui.analysis.TmfAnalysisViewOutput;
import org.eclipse.osgi.util.NLS;

/**
 * Class to implement the critical path analysis
 */
public class CriticalPathModule extends TmfAbstractAnalysisModule {

    /**
     * Analysis ID for this module
     */
    public static final String ANALYSIS_ID = "org.eclipse.linuxtools.lttng2.kernel.ui.criticalpath"; //$NON-NLS-1$

    /** Graph parameter name */
    public static final String PARAM_GRAPH = "graph"; //$NON-NLS-1$
    private TmfGraphBuilderModule fGraphModule;
    /** Worker_id parameter name */
    public static final String PARAM_WORKER = "workerid"; //$NON-NLS-1$
    /** Algorithm parameter name */
    public static final String PARAM_ALGORITHM = "algorithm"; //$NON-NLS-1$
    private TmfGraph fCriticalPath;

    /**
     * Default constructor
     */
    public CriticalPathModule() {
        super();
        registerOutput(new TmfAnalysisViewOutput(CriticalPathView.ID));
    }

    @Override
    protected boolean executeAnalysis(final IProgressMonitor monitor) throws TmfAnalysisException {
        /* Get the graph */
        TmfGraphBuilderModule graphModule = getGraph();
        if (graphModule == null) {
            Activator.getDefault().logWarning("No graph was found to execute the critical path on"); //$NON-NLS-1$
            return true;
        }
        graphModule.schedule();

        monitor.setTaskName(NLS.bind(Messages.CriticalPathModule_waitingForGraph, graphModule.getName()));
        if (!graphModule.waitForCompletion(monitor)) {
            Activator.getDefault().logInfo("Critical path execution: graph building was cancelled.  Results may not be accurate."); //$NON-NLS-1$
            return false;
        }
        TmfGraph graph = graphModule.getGraph();
        if (graph == null) {
            throw new TmfAnalysisException("Critical Path analysis: graph " + graphModule.getName() + " is null"); //$NON-NLS-1$//$NON-NLS-2$
        }

        /* Get the worker id */
        Object worker = getParameter(PARAM_WORKER);

        TmfVertex head = graph.getHead(worker);
        if (head == null) {
            System.err.println("WARNING: head vertex is null for task " + worker); //$NON-NLS-1$
            return false;
        }
        TmfTimeRange tr = TmfTraceManager.getInstance().getCurrentRange();
        TmfVertex start = graph.getVertexAt(tr.getStartTime(), worker);
        if (start == null) {
            System.err.println("WARNING: no vertex at time " + tr.getStartTime() + " for task " + worker); //$NON-NLS-1$//$NON-NLS-2$
            return false;
        }
        ICriticalPathAlgorithm cp = getAlgorithm(graph);
        System.out.println("Will compute critical path with algorithm " + cp.getClass().getName());
        fCriticalPath = cp.compute(start, null);

        return true;
    }

    @Override
    protected void canceling() {

    }

    @Override
    public Object getParameter(String name) {
        if (name.equals(PARAM_GRAPH)) {
            try {
                return getGraph();
            } catch (TmfAnalysisException e) {
                Activator.getDefault().logError("Error getting the graph for critical path", e); //$NON-NLS-1$
                return null;
            }
        }
        return super.getParameter(name);
    }

    @Override
    public synchronized void setParameter(String name, Object value) throws RuntimeException {
        if (name.equals(PARAM_GRAPH) && (value instanceof String)) {
            setGraph((String) value);
        }
        super.setParameter(name, value);
    }

    @Override
    protected void parameterChanged(String name) {
        cancel();
        resetAnalysis();
        schedule();
    }

    /**
     * The value of graph should be the id of the analysis module that builds
     * the required graph
     *
     * @param graphName
     *            Id of the graph
     */
    private void setGraph(String graphName) {
        IAnalysisModule module = getTrace().getAnalysisModule(graphName);
        if (module instanceof TmfGraphBuilderModule) {
            fGraphModule = (TmfGraphBuilderModule) module;
        }
    }

    @SuppressWarnings("nls")
    private TmfGraphBuilderModule getGraph() throws TmfAnalysisException {
        /* The graph module is null, take the first available graph if any */
        if (fGraphModule == null) {
            Object paramGraph = super.getParameter(PARAM_GRAPH);
            if (paramGraph instanceof String) {
                IAnalysisModule module = getTrace().getAnalysisModule((String) paramGraph);
                if (module instanceof TmfGraphBuilderModule) {
                    fGraphModule = (TmfGraphBuilderModule) module;
                } else {
                    throw new TmfAnalysisException(String.format(
                            "CriticalPathModule: getParameter(%s) is of the wrong type " +
                            "(expected TmfGraphBuilderModule but was %s)",
                            PARAM_GRAPH, (module == null) ? "null" : module.getClass()));
                }
            } else {
                throw new TmfAnalysisException(String.format(
                        "CriticalPathModule: getParameter(%s) is of the wrong type " +
                        "(expected the ID of a graph analysis, but got %s)",
                        PARAM_GRAPH, paramGraph));
            }
        }
        return fGraphModule;
    }

    private ICriticalPathAlgorithm getAlgorithm(TmfGraph graph) {
        Object algorithmClass = super.getParameter(PARAM_ALGORITHM);
        if (algorithmClass instanceof Class) {
            if (((Class<?>) algorithmClass).equals(CriticalPathAlgorithmUnbounded.class)) {
                return new CriticalPathAlgorithmUnbounded(graph);
            }
        }
        return new CriticalPathAlgorithmBounded(graph);
    }

    @Override
    public boolean canExecute(@NonNull ITmfTrace trace) {
        /*
         * TODO: The critical path executes on a graph, so at least a graph must
         * be available for this trace
         */
        return true;
    }

    /**
     * Gets the graph for the critical path
     *
     * @return The critical path graph
     */
    public TmfGraph getCriticalPath() {
        return fCriticalPath;
    }

    @Override
    protected String getFullHelpText() {
        return "Computes the critical path of an execution graph.  To compute the critical path, you need to select a process in the control flow view and the path will be computed for that process."; //$NON-NLS-1$
    }

    @Override
    protected String getShortHelpText(ITmfTrace trace) {
        return getFullHelpText();
    }

    @Override
    protected String getTraceCannotExecuteHelpText(@NonNull ITmfTrace trace) {
        return "There is no graph available on which to compute the critical path"; //$NON-NLS-1$
    }

}
