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

package org.eclipse.linuxtools.tmf.analysis.graph.core.staging;

import java.util.EnumMap;

import org.eclipse.linuxtools.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.linuxtools.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.linuxtools.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.linuxtools.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.linuxtools.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.EventHandler.Ctx;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Task.StateEnum;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfEvent;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

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
    public enum Attributes {
        STATE(0, "state"); //$NON-NLS-1$
        private final Integer value;
        private final String label;
        private Attributes(Integer v, String label) {
            this.value = v;
            this.label = label;
        }
        public Integer value() {
            return value;
        }
        public String label() {
            return label;
        }
    }

    Table<String, Long, EnumMap<Attributes, Integer>> quarkCache; // (host, tid, (attr, quark))

    /**
     * Instantiate a new state provider plugin.
     *
     * @param trace
     *            The LTTng 2.0 kernel trace directory
     */
    public ExecGraphStateProvider(ITmfTrace trace) {
        super(trace, CtfTmfEvent.class, "LTTng Kernel"); //$NON-NLS-1$
        quarkCache = HashBasedTable.create();
        initHandler();
    }

    private void initHandler() {
        fHandler = new EventHandler();
        fHandler.addListener(new ITaskListener() {
            @Override
            public void stateChange(Ctx ctx, Task task, StateEnum state) {
                try {
                    doStateChange(ctx, task, state);
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
            private void doStateChange(Ctx ctx, Task task, StateEnum state) throws StateValueTypeException, AttributeNotFoundException {
                Integer q = getOrCreateTaskQuark(task, Attributes.STATE);

                long packed = 0;
                switch (task.getState()) {
                case BLOCKED:
                    packed = PackedLongValue.pack(task.getState().value(), 0);
                    break;
                case PREEMPTED:
                    packed = PackedLongValue.pack(task.getState().value(), ctx.cpu);
                    break;
                case EXIT:
                case RUN:
                default:
                    packed = task.getState().value();
                    break;
                }

                ITmfStateValue value = TmfStateValue.newValueLong(packed);
                ss.modifyAttribute(task.getLastUpdate(), value, q);

                // cleanup
                if (state == StateEnum.EXIT) {
                    value = TmfStateValue.newValueLong(StateEnum.EXIT.value());
                    ss.modifyAttribute(ctx.ts, value, q);
                    quarkCache.remove(task.getHostID(), task.getTID());
                }
            }

            private Integer getOrCreateTaskQuark(Task task, Attributes attribute) {
                if (!quarkCache.contains(task.getHostID(), task.getTID())) {
                    String host = task.getHostID().replaceAll("^\"|\"$", "");  //$NON-NLS-1$ //$NON-NLS-2$ // unquote
                    // create all task attributes
                    EnumMap<Attributes, Integer> quarks = new EnumMap<>(Attributes.class);
                    for (Attributes attr: Attributes.values()) {
                        int quark = ss.getQuarkAbsoluteAndAdd(host, task.getTID().toString(), attr.label());
                        quarks.put(attr, quark);
                    }
                    quarkCache.put(task.getHostID(), task.getTID(), quarks);
                }
                return quarkCache.get(task.getHostID(), task.getTID()).get(attribute);
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