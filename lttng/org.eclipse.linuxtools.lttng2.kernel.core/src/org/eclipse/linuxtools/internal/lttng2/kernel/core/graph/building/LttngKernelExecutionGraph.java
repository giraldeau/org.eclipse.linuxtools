/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building;

import org.eclipse.linuxtools.tmf.analysis.graph.core.building.ITmfGraphProvider;
import org.eclipse.linuxtools.tmf.analysis.graph.core.building.TmfGraphBuilderModule;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

/**
 * Graph building module for the lttng kernel execution graph
 * @since 3.1
 */
public class LttngKernelExecutionGraph extends TmfGraphBuilderModule {

    /**
     * Analysis id of this module
     */
    public static final String ANALYSIS_ID = "org.eclipse.linuxtools.lttng2.kernel.core.execgraph"; //$NON-NLS-1$

    @Override
    public boolean canExecute(ITmfTrace trace) {
        /*
         * FIXME: ITmfTrace does not have hasEvent() method
         * Always return true until the method is available.
         */
        /*
        ITmfTrace[] traceSet = TmfTraceManager.getFullTraceSet(trace);
        for (ITmfTrace traceItem: traceSet) {
            if (traceItem instanceof LttngKernelTrace) {
                LttngKernelTrace kernel = (LttngKernelTrace) traceItem;
                String[] names = { LttngStrings.SCHED_SWITCH, LttngStrings.SCHED_WAKEUP };
                String[] addons = { LttngStrings.SCHED_SWITCH, LttngStrings.SCHED_TTWU };
                if(kernel.hasAllEvents(names) || kernel.hasAllEvents(addons)) {
                    return true;
                }
            }
        }
        */
        return true;
    }

    @Override
    protected ITmfGraphProvider getGraphProvider() {
        return new LttngKernelExecGraphProvider(getModelRegistry(), getTrace());
    }

    @Override
    protected String getFullHelpText() {
        return super.getFullHelpText();
    }

    @Override
    protected String getShortHelpText(ITmfTrace trace) {
        return super.getShortHelpText(trace);
    }

    @Override
    protected String getTraceCannotExecuteHelpText(ITmfTrace trace) {
        return "The trace must have events 'sched_switch' and 'sched_wakeup' enabled"; //$NON-NLS-1$
    }

}
