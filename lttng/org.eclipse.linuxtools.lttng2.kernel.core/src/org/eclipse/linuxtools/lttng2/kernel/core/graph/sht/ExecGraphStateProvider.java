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
import java.util.Stack;

import org.eclipse.linuxtools.internal.lttng2.kernel.core.LttngStrings;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.model.EventField;
import org.eclipse.linuxtools.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventField;
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

    /**
     * Processing state using POJO objects for efficiency
     */
    public static class Ctx {
        ITmfEventField content;
        String eventName;
        String hostId;
        Long ts;
        Integer cpu;
        Machine machine;

        /**
         * Load current context from an event
         * @param self the state provider
         * @param event the event
         */
        public void load(ExecGraphStateProvider self, CtfTmfEvent event) {
            content = event.getContent();
            eventName = event.getType().getName();
            hostId = event.getTrace().getHostId();
            ts = event.getTimestamp().getValue();
            cpu = event.getCPU();
            machine = self.getMachine(hostId);
        }
    }

    private Ctx ctx;

    private HashMap<String, Machine> machines;

    /**
     * Instantiate a new state provider plugin.
     *
     * @param trace
     *            The LTTng 2.0 kernel trace directory
     */
    public ExecGraphStateProvider(TmfExperiment trace) {
        super(trace, CtfTmfEvent.class, "LTTng Kernel"); //$NON-NLS-1$
        machines = new HashMap<>();
        ctx = new Ctx();
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
        /*
         * AbstractStateChangeInput should have already checked for the correct
         * class type
         */
        CtfTmfEvent event = (CtfTmfEvent) ev;

//        int quark = 0;
//        ITmfStateValue value = null;

        switch (event.getType().getName()) {
        case LttngStrings.SCHED_SWITCH:
            ctx.load(this, event);
            handleSchedSwitch(event);
            break;
        case LttngStrings.SCHED_TTWU:
            ctx.load(this, event);
            handleSchedWakeup(event);
            break;
        case LttngStrings.SOFTIRQ_ENTRY:
        case LttngStrings.IRQ_HANDLER_ENTRY:
        case LttngStrings.HRTIMER_EXPIRE_ENTRY:
            pushInterrupt(event);
            break;
        case LttngStrings.SOFTIRQ_EXIT:
        case LttngStrings.IRQ_HANDLER_EXIT:
        case LttngStrings.HRTIMER_EXPIRE_EXIT:
            popInterrupt(event);
            break;
        default:
            break;
        }
    }

    private void pushInterrupt(CtfTmfEvent event) {
        ctx.load(this, event);
        Long attr = 0L;
        Integer type = 0;
        switch(ctx.eventName) {
        case LttngStrings.SOFTIRQ_ENTRY:
            type = Interrupt.SOFTIRQ;
            attr = EventField.getLong(event, LttngStrings.VEC);
            break;
        case LttngStrings.IRQ_HANDLER_ENTRY:
            type = Interrupt.IRQ;
            attr = EventField.getLong(event, LttngStrings.IRQ);
            break;
        case LttngStrings.HRTIMER_EXPIRE_ENTRY:
            type = Interrupt.HRTIMER;
            attr = EventField.getLong(event, LttngStrings.HRTIMER);
            break;
        default:
            break;
        }
        ctx.machine.getInterruptStack(ctx.cpu).push(new Interrupt(ctx.cpu, ctx.ts, type, attr));
    }

    private Interrupt popInterrupt(CtfTmfEvent event) {
        ctx.load(this, event);
        Long attr = 0L;
        Integer type = 0;
        switch(ctx.eventName) {
        case LttngStrings.SOFTIRQ_EXIT:
            type = Interrupt.SOFTIRQ;
            attr = EventField.getLong(event, LttngStrings.VEC);
            break;
        case LttngStrings.IRQ_HANDLER_EXIT:
            type = Interrupt.IRQ;
            attr = EventField.getLong(event, LttngStrings.IRQ);
            break;
        case LttngStrings.HRTIMER_EXPIRE_EXIT:
            type = Interrupt.HRTIMER;
            attr = EventField.getLong(event, LttngStrings.HRTIMER);
            break;
        default:
            break;
        }
        Stack<Interrupt> stack = ctx.machine.getInterruptStack(ctx.cpu);
        Interrupt ret = null;
        if (!stack.isEmpty()) {
            Interrupt top = stack.peek();
            if (top.type.equals(type) && top.vec.equals(attr)) {
                ret = stack.pop();
            } else {
                System.out.println("warning: wrong interrupt stack");
            }
        }
        return ret;
    }

    private void handleSchedSwitch(CtfTmfEvent event) {
        Long next = EventField.getLong(event, LttngStrings.NEXT_TID);
        Long prev = EventField.getLong(event, LttngStrings.PREV_TID);
        Long prevState = EventField.getLong(event, LttngStrings.PREV_STATE);
        prevState = new Long((prevState.intValue()) & (0x3));
        ctx.machine.setCurrentTid(ctx.cpu, next);
        Task nextTask = ctx.machine.getOrCreateTask(ctx.cpu, next, ctx.ts);
        Task prevTask = ctx.machine.getOrCreateTask(ctx.cpu, prev, ctx.ts);
        nextTask.setState(ctx.ts, Task.RUN);
        prevTask.setState(ctx.ts, prevState.equals(0L) ? Task.PREEMPTED : Task.BLOCKED);
    }

    private void handleSchedWakeup(CtfTmfEvent event) {
        Long target = EventField.getLong(event, LttngStrings.TID);
        Task targetTask = ctx.machine.getOrCreateTask(ctx.cpu, target, ctx.ts);
        if (targetTask.getState() == Task.BLOCKED) {
            System.out.println("blocked " + target);
        }
        targetTask.setState(ctx.ts, Task.PREEMPTED);
    }

    /**
     * Get the machine for the corresponding event
     * @param hostId the host string
     * @return machine
     */
    public Machine getMachine(String hostId) {
        Machine machine = machines.get(hostId);
        if (machine == null) {
            machine = new Machine(hostId);
            machines.put(hostId, machine);
        }
        return machine;
    }

}