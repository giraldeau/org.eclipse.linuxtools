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

import java.util.HashMap;

import org.eclipse.linuxtools.lttng2.kernel.core.graph.sht.EventHandler.Ctx;
import org.eclipse.linuxtools.lttng2.kernel.core.graph.sht.Task.StateEnum;
import org.eclipse.linuxtools.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.linuxtools.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.linuxtools.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.linuxtools.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.linuxtools.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
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
     * Attributes keys in the state system
     *
     * @author Francis Giraldeau <francis.giraldeau@gmail.com>
     *
     */
    public static class Attributes {
        public static final String LABEL_STATE = "state"; //$NON-NLS-1$
        public static final String LABEL_BLOCKED = "blocked"; //$NON-NLS-1$
    }

    HashMap<Long, Integer> tidStateQuark; // (tid, quark)

    /**
     * Instantiate a new state provider plugin.
     *
     * @param trace
     *            The LTTng 2.0 kernel trace directory
     */
    public ExecGraphStateProvider(ITmfTrace trace) {
        super(trace, CtfTmfEvent.class, "LTTng Kernel"); //$NON-NLS-1$
        tidStateQuark = new HashMap<>();
        initHandler();
    }

    private void initHandler() {
        fHandler = new EventHandler();
        fHandler.addListener(new ITaskListener() {
            @Override
            public void stateChange(Ctx ctx, Task task, Long start, Long end, StateEnum state) {
                try {
                    doStateChange(ctx, task, start, end, state);
                } catch (StateValueTypeException | AttributeNotFoundException e) {
                    throw new RuntimeException("doStateChange() failed"); //$NON-NLS-1$
                }
            }

            /**
             * @param ctx
             * @param task
             * @param start
             * @param end
             * @param state
             */
            private void doStateChange(Ctx ctx, Task task, Long start, Long end, StateEnum state) throws StateValueTypeException, AttributeNotFoundException {
                // make path
                int quark;
                if (!tidStateQuark.containsKey(task.getTID())) {
                    String host = task.getHostID().replaceAll("^\"|\"$", "");  //$NON-NLS-1$ //$NON-NLS-2$
                    quark = ss.getQuarkAbsoluteAndAdd(host, task.getTID().toString(), Attributes.LABEL_STATE);
                    tidStateQuark.put(task.getTID(), quark);
                } else {
                    quark = tidStateQuark.get(task.getTID());
                }
                ITmfStateValue value = TmfStateValue.newValueInt(task.getState().value());
                ss.modifyAttribute(start, value, quark);

                // cleanup
                if (state == StateEnum.EXIT) {
                    tidStateQuark.remove(task.getTID());
                }
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
        return new ExecGraphStateProvider(this.getTrace());
    }

    @Override
    protected void eventHandle(ITmfEvent ev) {
        fHandler.eventHandle((CtfTmfEvent) ev);
    }

}