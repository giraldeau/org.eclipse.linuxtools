/*******************************************************************************
 * Copyright (c) 2014 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francis Giraldeau - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.lttng2.kernel.core.graph.sht;

import org.eclipse.linuxtools.lttng2.kernel.core.graph.sht.EventHandler.Ctx;
import org.eclipse.linuxtools.lttng2.kernel.core.graph.sht.Task.StateEnum;
import org.eclipse.linuxtools.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfEvent;

/**
 * Generate blocking interval tree
 *
 * @author Francis Giraldeau
 *
 */
public class ExecGraphStateProvider extends AbstractTmfStateProvider {

    /**
     * Version number of this state provider. Please bump this if you modify the
     * contents of the generated state history in some way.
     */
    private static final int VERSION = 1;

    private EventHandler fHandler;

    /**
     * Instantiate a new state provider plugin.
     *
     * @param trace
     *            The LTTng 2.0 kernel trace directory
     */
    public ExecGraphStateProvider(TmfExperiment trace) {
        super(trace, CtfTmfEvent.class, "LTTng Kernel"); //$NON-NLS-1$
        initHandler();
    }

    private void initHandler() {
        fHandler = new EventHandler();
        fHandler.addListener(new ITaskListener() {
            @Override
            public void stateChange(Ctx ctx, Task task, Long start, Long end, StateEnum state) {
                //ITmfStateSystem ss = getAssignedStateSystem();
                System.out.println(task + " " + (end - start) + " " + state);
            }
        });
    }

    // ------------------------------------------------------------------------
    // IStateChangeInput
    // ------------------------------------------------------------------------

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public void assignTargetStateSystem(ITmfStateSystemBuilder ssb) {
        super.assignTargetStateSystem(ssb);
    }

    @Override
    public ExecGraphStateProvider getNewInstance() {
        return new ExecGraphStateProvider((TmfExperiment) this.getTrace());
    }

    @Override
    protected void eventHandle(ITmfEvent ev) {
        fHandler.eventHandle((CtfTmfEvent) ev);
    }

}