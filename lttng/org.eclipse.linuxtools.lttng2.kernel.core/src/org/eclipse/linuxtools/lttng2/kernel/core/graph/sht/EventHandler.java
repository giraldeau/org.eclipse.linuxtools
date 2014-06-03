package org.eclipse.linuxtools.lttng2.kernel.core.graph.sht;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import org.eclipse.linuxtools.internal.lttng2.kernel.core.LttngStrings;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.model.EventField;
import org.eclipse.linuxtools.lttng2.kernel.core.graph.sht.Task.StateEnum;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventField;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfEvent;

/**
 * Task state machine
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
public class EventHandler {

    /**
     * Processing state using POJO objects for efficiency
     */
    public static class Ctx {
        CtfTmfEvent event;
        ITmfEventField content;
        String eventName;
        String hostId;
        Long ts;
        Integer cpu;
        Machine machine;

        /**
         * Load current context from an event
         * @param self the state provider
         * @param ev the event
         */
        public void load(EventHandler self, CtfTmfEvent ev) {
            event = ev;
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

    private ArrayList<ITaskListener> stateListeners;

    /**
     * Constructor
     */
    public EventHandler() {
        machines = new HashMap<>();
        ctx = new Ctx();
        stateListeners = new ArrayList<>();
    }

    /**
     * Update state machine with the input event
     * @param event the event
     */
    public void eventHandle(CtfTmfEvent event) {
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

    /**
     * Add item to the interrupt stack
     * @param event the event
     */
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

    /**
     * Return from interrupt
     * @param event the event
     * @return the top of the interrupt stack
     */
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
            }
        }
        return ret;
    }

    /**
     * Handle the scheduling event
     * @param event
     */
    private void handleSchedSwitch(CtfTmfEvent event) {
        Long next = EventField.getLong(event, LttngStrings.NEXT_TID);
        Long prev = EventField.getLong(event, LttngStrings.PREV_TID);
        Long prevState = EventField.getLong(event, LttngStrings.PREV_STATE);
        prevState = new Long((prevState.intValue()) & (0x3));
        ctx.machine.setCurrentTid(ctx.cpu, next);
        Task nextTask = ctx.machine.getOrCreateTask(ctx.cpu, next, ctx.ts);
        Task prevTask = ctx.machine.getOrCreateTask(ctx.cpu, prev, ctx.ts);
        notifyStateChange(nextTask, StateEnum.RUN);
        notifyStateChange(prevTask, prevState.equals(0L) ? StateEnum.PREEMPTED : StateEnum.BLOCKED);
    }

    /**
     * Handle the wakeup event
     * @param event
     */
    private void handleSchedWakeup(CtfTmfEvent event) {
        Long target = EventField.getLong(event, LttngStrings.TID);
        Task targetTask = ctx.machine.getOrCreateTask(ctx.cpu, target, ctx.ts);
        notifyStateChange(targetTask, StateEnum.PREEMPTED);
    }

    /**
     * Call this wrapper function to change task state
     * Will notify listeners (avoid keeping listener reference into the task itself)
     *
     * @param task
     * @param nextState
     */
    private void notifyStateChange(Task task, StateEnum nextState) {
        for (ITaskListener listener: stateListeners) {
            listener.stateChange(ctx, task, task.getLastUpdate(), ctx.ts, nextState);
        }
        task.setState(ctx.ts, nextState);
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

    /**
     * Add task state change listener
     * @param listener the task state listener
     */
    public void addListener(ITaskListener listener) {
        stateListeners.add(listener);
    }

}
