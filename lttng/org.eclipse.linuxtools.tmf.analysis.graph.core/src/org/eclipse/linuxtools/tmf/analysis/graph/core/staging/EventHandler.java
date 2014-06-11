package org.eclipse.linuxtools.tmf.analysis.graph.core.staging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Interrupt.Hardirq;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Interrupt.Softirq;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Interrupt.InterruptType;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Task.StateEnum;
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
        Task wakeupSource;

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
            wakeupSource = null;
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
        InterruptType type = InterruptType.UNKNOWN;
        switch(ctx.eventName) {
        case LttngStrings.SOFTIRQ_ENTRY:
            type = InterruptType.SOFTIRQ;
            attr = Field.getLong(event, LttngStrings.VEC);
            break;
        case LttngStrings.IRQ_HANDLER_ENTRY:
            type = InterruptType.HARDIRQ;
            attr = Field.getLong(event, LttngStrings.IRQ);
            break;
        case LttngStrings.HRTIMER_EXPIRE_ENTRY:
            type = InterruptType.HRTIMER;
            attr = Field.getLong(event, LttngStrings.HRTIMER);
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
        InterruptType type = InterruptType.UNKNOWN;
        switch(ctx.eventName) {
        case LttngStrings.SOFTIRQ_EXIT:
            type = InterruptType.SOFTIRQ;
            attr = Field.getLong(event, LttngStrings.VEC);
            break;
        case LttngStrings.IRQ_HANDLER_EXIT:
            type = InterruptType.HARDIRQ;
            attr = Field.getLong(event, LttngStrings.IRQ);
            break;
        case LttngStrings.HRTIMER_EXPIRE_EXIT:
            type = InterruptType.HRTIMER;
            attr = Field.getLong(event, LttngStrings.HRTIMER);
            break;
        default:
            break;
        }
        Stack<Interrupt> stack = ctx.machine.getInterruptStack(ctx.cpu);
        Interrupt ret = null;
        if (!stack.isEmpty()) {
            Interrupt top = stack.peek();
            if (top.getType().equals(type) && top.getVec().equals(attr)) {
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
        Long next = Field.getLong(event, LttngStrings.NEXT_TID);
        Long prev = Field.getLong(event, LttngStrings.PREV_TID);
        int val = Field.getLong(event, LttngStrings.PREV_STATE).intValue();

        StateEnum prevState = StateEnum.WAIT_CPU;
        if ((val & 0x3) != 0) {
            /* the real wait type is known at wake-up only, assign generic
             * WAIT_BLOCKED in mean time.
             */
            prevState = StateEnum.WAIT_UNKNOWN;
        } else if ((val & 0x40) != 0) {
            prevState = StateEnum.EXIT;
        }
        ctx.machine.setCurrentTid(ctx.cpu, next);
        Task nextTask = ctx.machine.getOrCreateTask(ctx.cpu, next, ctx.ts);
        Task prevTask = ctx.machine.getOrCreateTask(ctx.cpu, prev, ctx.ts);
        nextTask.setStateRaw(StateEnum.WAIT_CPU); // a task that is scheduled was necessarily waiting the CPU
        notifyStateChange(nextTask, StateEnum.RUNNING);
        notifyStateChange(prevTask, prevState);
    }

    /**
     * Handle the wakeup event
     * @param event
     */
    private void handleSchedWakeup(CtfTmfEvent event) {
        Long targetTid = Field.getLong(event, LttngStrings.TID);
        Task targetTask = ctx.machine.getOrCreateTask(ctx.cpu, targetTid, ctx.ts);
        /*
         * Resolve the wake-up type. We change the task state only if the task
         * was blocked or unknown.
         */
        if (targetTask.getState() == StateEnum.WAIT_UNKNOWN) {

            // 1. Wake-up from interrupt
            Stack<Interrupt> interruptStack = ctx.machine.getInterruptStack(ctx.cpu);
            if (!interruptStack.isEmpty()) {
                Interrupt top = interruptStack.peek();
                switch(top.getType()) {
                case HRTIMER:
                    targetTask.setStateRaw(StateEnum.WAIT_TIMER);
                    break;
                case HARDIRQ:
                    targetTask.setStateRaw(resolveIRQ(top.getVec()));
                    break;
                case SOFTIRQ:
                    targetTask.setStateRaw(resolveSoftirq(top.getVec()));
                    break;
                case UNKNOWN:
                default:
                    break;
                }
            } else {
                // 1. Wake-up from the current task
                ctx.wakeupSource = ctx.machine.getCurrentTask(ctx.cpu, ctx.ts);
                targetTask.setStateRaw(StateEnum.WAIT_TASK);
            }
            //targetTask.setState(targetTask.getLastUpdate(), state);
            notifyStateChange(targetTask, StateEnum.WAIT_CPU);
        }
    }

    private static StateEnum resolveIRQ(Long vec) {
        StateEnum ret = StateEnum.WAIT_UNKNOWN;
        Hardirq irq = Hardirq.fromValue(vec.intValue());
        switch (irq) {
        case RESCHED:
            ret = StateEnum.INTERRUPTED;
            break;
        case EHCI_HCD_1:
        case EHCI_HCD_2:
            ret = StateEnum.WAIT_USER_INPUT;
            break;
        case UNKNOWN:
        default:
            ret = StateEnum.WAIT_UNKNOWN;
            break;
        }
        return ret;
    }

    private static StateEnum resolveSoftirq(Long vec) {
        StateEnum ret = StateEnum.WAIT_UNKNOWN;
        Softirq soft = Softirq.fromValue(vec.intValue());
        switch (soft) {
        case HRTIMER:
        case TIMER:
            ret = StateEnum.WAIT_TIMER;
            break;
        case BLOCK:
        case BLOCK_IOPOLL:
            ret = StateEnum.WAIT_BLOCK_DEV;
            break;
        case NET_RX:
        case NET_TX:
            ret = StateEnum.WAIT_NETWORK;
            break;
        case SCHED:
            ret = StateEnum.INTERRUPTED;
            break;
        case HI:
        case RCU:
        case TASKLET:
        case UNKNOWN:
        default:
            ret = StateEnum.WAIT_UNKNOWN;
            break;
        }
        return ret;
    }

    /**
     * Call this wrapper function to change task state
     * Will notify listeners (avoid keeping listener reference into the task itself)
     *
     * @param task
     * @param nextState
     */
    private void notifyStateChange(Task task, StateEnum nextState) {
        // make sure there is a state change
        if (task.getState() != nextState) {
            for (ITaskListener listener: stateListeners) {
                listener.stateChange(ctx, task, nextState);
            }
            task.setState(ctx.ts, nextState);
            if (nextState == StateEnum.EXIT) {
                ctx.machine.removeTask(task);
            }
        }
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

    public void handleDone() {
        /*
         * flush pending state for all tasks
         */
        for (Machine machine: machines.values()) {
            for (Task task: machine.tasks.values()) {
                for (ITaskListener listener: stateListeners) {
                    listener.stateFlush(task);
                }
            }
        }
    }

}
