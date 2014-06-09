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

    public static final String LABEL_TASK = "task";
    public static final String LABEL_CPU = "cpu";

    /**
     * Attributes keys in the state system
     *
     * @author Francis Giraldeau <francis.giraldeau@gmail.com>
     *
     */
    public enum Attributes {
        STATE("state"); //$NON-NLS-1$
        private final String label;
        private Attributes(String label) {
            this.label = label;
        }
        public String label() {
            return label;
        }
    }

    Table<String, Long, EnumMap<Attributes, Integer>> quarkTidCache; // (host, tid, (attr, quark))
    Table<String, Long, EnumMap<Attributes, Integer>> quarkCpuCache; // (host, cpu, (attr, quark))

    /**
     * Instantiate a new state provider plugin.
     *
     * @param trace
     *            The LTTng 2.0 kernel trace directory
     */
    public ExecGraphStateProvider(ITmfTrace trace) {
        super(trace, CtfTmfEvent.class, "LTTng Kernel"); //$NON-NLS-1$
        quarkTidCache = HashBasedTable.create();
        quarkCpuCache = HashBasedTable.create();
        initHandler();
    }

    private void initHandler() {
        fHandler = new EventHandler();
        fHandler.addListener(new ITaskListener() {
            @Override
            public void stateChange(Ctx ctx, Task task, StateEnum state) {
                try {
                    updateCpuState(ctx, task, state);
                    updateTaskState(ctx, task, state);
                } catch (StateValueTypeException | AttributeNotFoundException e) {
                    e.printStackTrace();
                    throw new RuntimeException("doStateChange() failed"); //$NON-NLS-1$
                }
            }

            private void updateCpuState(Ctx ctx, Task task, StateEnum state) throws StateValueTypeException, AttributeNotFoundException {
                if (state == StateEnum.RUN) {
                    Integer qCpuState = getOrCreateQuark(quarkCpuCache, ctx.hostId, LABEL_CPU, ctx.cpu.longValue(), Attributes.STATE);
                    ITmfStateValue value = TmfStateValue.newValueInt(task.getTID().intValue());
                    ss.modifyAttribute(ctx.ts, value, qCpuState);
                }
            }

            /**
             * @param state the next task state
             */
            private void updateTaskState(Ctx ctx, Task task, StateEnum state) throws StateValueTypeException, AttributeNotFoundException {
                Integer qTaskState = getOrCreateQuark(quarkTidCache, ctx.hostId, LABEL_TASK, task.getTID(), Attributes.STATE);
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
                case UNKNOWN:
                default:
                    packed = task.getState().value();
                    break;
                }
                ITmfStateValue value = TmfStateValue.newValueLong(packed);
                ss.modifyAttribute(task.getLastUpdate(), value, qTaskState);

                // cleanup
                if (state == StateEnum.EXIT) {
                    value = TmfStateValue.newValueLong(StateEnum.EXIT.value());
                    ss.modifyAttribute(ctx.ts, value, qTaskState);
                    quarkTidCache.remove(task.getHostID(), task.getTID());
                }
            }

            private Integer getOrCreateQuark(Table<String, Long, EnumMap<Attributes, Integer>> cache, String host, String type, Long id, Attributes attribute) {
                if (!cache.contains(host, id)) {
                    String clean = host.replaceAll("^\"|\"$", "");  //$NON-NLS-1$ //$NON-NLS-2$ // unquote
                    // create all attributes
                    EnumMap<Attributes, Integer> quarks = new EnumMap<>(Attributes.class);
                    for (Attributes attr: Attributes.values()) {
                        int quark = ss.getQuarkAbsoluteAndAdd(clean, type, id.toString(), attr.label());
                        quarks.put(attr, quark);
                    }
                    cache.put(host, id, quarks);
                }
                return cache.get(host, id).get(attribute);
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