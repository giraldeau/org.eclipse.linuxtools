package org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.handlers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.linuxtools.internal.lttng2.kernel.core.LttngStrings;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building.LttngKernelSystemModelStrings;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building.LttngKernelExecGraphProvider.execution_mode_enum;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building.LttngKernelExecGraphProvider.process_status_enum;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.model.ALog;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.model.AnalysisFilter;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.model.CloneFlags;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.model.EventField;
import org.eclipse.linuxtools.tmf.analysis.graph.core.building.AbstractTmfGraphProvider;
import org.eclipse.linuxtools.tmf.analysis.graph.core.building.AbstractTraceEventHandler;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.ITmfWorkerFactory;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelElementDeclaration;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelResource;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfSystemModelWithCpu;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfWorker;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfWorkerDeclaration;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTraceWithPreDefinedEvents;
import org.eclipse.linuxtools.tmf.core.trace.TmfEventTypeCollectionHelper;
import org.eclipse.linuxtools.tmf.core.trace.TmfTraceManager;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfEvent;

/**
 * Provides the current task running on a CPU according to scheduling events
 */

@SuppressWarnings("nls")
public class TraceEventHandlerSched extends AbstractTraceEventHandler {

    /* Keep tmp info until corresponding sys_exit */
    public enum EventType {
        SYS_EXECVE, SYS_CLONE
    }

    public class EventData {
        public EventType type;
        public String cmd;
        public long flags;
    }

    HashMap<Long, EventData> evHistory;

    TmfSystemModelWithCpu system;
    ITmfTrace fTrace;

    private AnalysisFilter filter;

    private ALog log;

    private Map<ITmfTrace, Boolean> fHasEventSchedTTWU;

    /*
     * sched_migrate_task: sched_process_exit: sched_process_fork:
     * sched_process_free: sched_process_wait: sched_stat_runtime:
     * sched_stat_sleep: sched_stat_wait: sched_switch: sched_wakeup:
     * sched_wakeup_new:
     */

    public static String[] getHandledEvents() {
        return new String[] { LttngStrings.SCHED_SWITCH, LttngStrings.SCHED_WAKEUP,
                LttngStrings.SCHED_PROCESS_FORK, LttngStrings.SCHED_PROCESS_EXIT,
                LttngStrings.SCHED_PROCESS_EXEC, LttngStrings.SCHED_TTWU };
    }

    @Override
    public void handleEvent(ITmfEvent ev) {
        CtfTmfEvent event = (CtfTmfEvent) ev;
        if (event.getType().getName().startsWith(LttngStrings.SYSCALL_PREFIX)) {
            int cpu = event.getCPU();
            TmfWorker curr = system.getWorkerCpu(event.getTrace().getHostId(), cpu);
            if (curr == null) {
                return;
            }
            curr.setField(LttngStrings.MODE, execution_mode_enum.SYSCALL);
        }
        String eventName = event.getType().getName();
        if (LttngStrings.SCHED_SWITCH.equals(eventName)) {
            handleSchedSwitch(event);
        } else if (LttngStrings.SCHED_WAKEUP.equals(eventName) && !fHasEventSchedTTWU.get(event.getTrace())) {
            handleSchedWakeup(event);
        } else if (LttngStrings.SCHED_WAKEUP_NEW.equals(eventName) && !fHasEventSchedTTWU.get(event.getTrace())) {
            handleSchedWakeup(event);
        } else if (LttngStrings.SCHED_TTWU.equals(eventName) && fHasEventSchedTTWU.get(event.getTrace())) {
            handleSchedWakeup(event);
        } else if (LttngStrings.SCHED_PROCESS_FORK.equals(eventName)) {
            handleSchedProcessFork(event);
        } else if (LttngStrings.SCHED_PROCESS_EXIT.equals(eventName)) {
            handleSchedProcessExit(event);
        } else if (LttngStrings.SCHED_PROCESS_EXEC.equals(eventName)) {
            handleSchedProcessExec(event);
        } else if (LttngStrings.EXIT_SYSCALL.equals(eventName)) {
            handleExitSyscall(event);
        }
    }

	public TraceEventHandlerSched(AbstractTmfGraphProvider provider) {
		super();
		filter = provider.getModelRegistry().getOrCreateModel(AnalysisFilter.class);
		system = provider.getModelRegistry().getOrCreateModel(TmfSystemModelWithCpu.class);
		system.init(provider);
		system.setSwapperFactory(new ITmfWorkerFactory() {
            @Override
            public TmfWorker createModelElement(String host, int cpu, long wid) {
                TmfWorkerDeclaration decl = system.getWorkerDeclaration(LttngKernelSystemModelStrings.WORKER);
                TmfWorker swapper = decl.create(0, null); // this is ugly, should not provide a null trace, but reference is unreachable in this scope
                swapper.setName(String.format("swapper/%d", cpu)); //$NON-NLS-1$
                swapper.setField(LttngStrings.PID, 0);
                swapper.setField(LttngStrings.PPID, 0);
                return swapper;
            }
        });
		log = ALog.getInstance();
        evHistory = new HashMap<>();
        fTrace = provider.getTrace();
        fHasEventSchedTTWU = new HashMap<>();

        ITmfTrace[] traceSet = TmfTraceManager.getTraceSet(fTrace);
        for (ITmfTrace traceItem: traceSet) {
            if (traceItem instanceof ITmfTraceWithPreDefinedEvents) {
                Set<String> traceEvents = TmfEventTypeCollectionHelper.getEventNames(((ITmfTraceWithPreDefinedEvents) traceItem).getContainedEventTypes());
                fHasEventSchedTTWU.put(traceItem, traceEvents.contains(LttngStrings.SCHED_TTWU));
            }
        }

    }

    private TmfWorker createTask(ITmfTrace trace, long tid, long ts, String cmd) {
        TmfWorkerDeclaration wd = system.getWorkerDeclaration(LttngKernelSystemModelStrings.WORKER);
        TmfWorker task = wd.create(tid, trace);
        task.setStart(ts);
        task.setName(cmd);
        system.putWorker(task);
        return task;
    }

    private void handleSchedSwitch(CtfTmfEvent event) {
        int cpu = event.getCPU();
        String host = event.getTrace().getHostId();
        if (system.getCurrentCPU(host) != cpu) {
            new RuntimeException("ERROR: system.cpu != event.cpu");
        }
        long next = EventField.getLong(event, LttngStrings.NEXT_TID);
        long prev = EventField.getLong(event, LttngStrings.PREV_TID);
        long prev_state = EventField.getLong(event, LttngStrings.PREV_STATE);
        prev_state = (long) ((int) prev_state) & (0x3);

        system.setCurrentTid(host, cpu, next);

        TmfWorker nextTask = system.getWorker(host, cpu, next);
        if (nextTask == null) {
            String name = EventField.getOrDefault(event, LttngStrings.NEXT_COMM, LttngStrings.UNKNOWN);
            nextTask = createTask(event.getTrace(), next, event.getTimestamp().getValue(), name);
            log.entry("sched_switch next task was null " + nextTask);
        }
        nextTask.setField(LttngStrings.STATUS, process_status_enum.RUN);

        TmfWorker prevTask = system.getWorker(host, cpu, prev);
        if (prevTask == null) {
            String name = EventField.getOrDefault(event, LttngStrings.PREV_COMM, LttngStrings.UNKNOWN);
            prevTask = createTask(event.getTrace(), next, event.getTimestamp().getValue(), name);
            log.entry("sched_switch prev task was null " + prevTask);
        }
        process_status_enum status = (process_status_enum) prevTask.getField(LttngStrings.STATUS);
        if (status != process_status_enum.RUN && status != process_status_enum.EXIT) {
            log.entry("prev task was not running " + prevTask + " " + event.getTimestamp());
        }
        // prev_state == 0 means runnable, thus waits for cpu
        if (prev_state == 0) {
            prevTask.setField(LttngStrings.STATUS, process_status_enum.WAIT_CPU);
        } else {
            prevTask.setField(LttngStrings.STATUS, process_status_enum.WAIT_BLOCKED);
        }
    }

    private void handleSchedProcessFork(CtfTmfEvent event) {
        // TODO: add child to parent's children list
        String host = event.getTrace().getHostId();
        int cpu = event.getCPU();
        long parent = EventField.getLong(event, LttngStrings.PARENT_TID);
        long child = EventField.getLong(event, LttngStrings.CHILD_TID);
        String name = EventField.getString(event, LttngStrings.CHILD_COMM);
        TmfWorkerDeclaration wd = system.getWorkerDeclaration(LttngKernelSystemModelStrings.WORKER);
        TmfWorker task = wd.create(child, event.getTrace());
        task.setName(name);
        task.setField(LttngStrings.PID, parent);
        task.setField(LttngStrings.PPID, parent);
        task.setStart(event.getTimestamp().getValue());
        system.putWorker(task);

        // handle filtering
        TmfWorker parentTask = system.getWorker(host, cpu, parent);
        if (parentTask != null && filter.isFollowChild() &&
                filter.getTids().contains(parentTask.getId())) {
            filter.addTid(child);
        }

        // we know clone succeed, thus copy file descriptors according to flags
        EventData data = evHistory.remove(parent);
        if (data != null) {
            if (!CloneFlags.CLONE_FILES.isFlagSet(data.flags)) {
                // detach file descriptors from parent
                TmfModelElementDeclaration decl = system.getResourceDeclaration(LttngKernelSystemModelStrings.RESOURCE_FD);

                Collection<TmfModelResource> parentFDs = system.getWorkerResources(parentTask, decl);
                for (TmfModelResource fd : parentFDs) {
                    TmfModelResource childFD = new TmfModelResource(fd);
                    system.addWorkerResource(task, childFD);
                }
            }
            if (!CloneFlags.CLONE_THREAD.isFlagSet(data.flags)) {
                // Promote a thread to process
                task.setField(LttngStrings.PID, task.getId());
            }
        }
        // FIXME: in some cases, sys_clone is not matched to exit_syscall
        // thus let's make sure it returns in user mode
        task.setField(LttngStrings.MODE, execution_mode_enum.USER_MODE);
        task.setField(LttngStrings.STATUS, process_status_enum.WAIT_FORK);
    }

    private void handleSchedWakeup(CtfTmfEvent event) {
        String host = event.getTrace().getHostId();
        int cpu = event.getCPU();
        long tid = EventField.getLong(event, LttngStrings.TID);
        TmfWorker target = system.getWorker(host, cpu, tid);
        TmfWorker current = system.getWorkerCpu(host, cpu);
        if (target == null) {
            String name = EventField.getOrDefault(event, LttngStrings.COMM, LttngStrings.UNKNOWN);
            target = createTask(event.getTrace(), tid, event.getTimestamp().getValue(), name);
            target.setField(LttngStrings.STATUS, process_status_enum.WAIT_BLOCKED);
            log.entry("sched_wakeup target was null " + event.getTimestamp().toString());
        }
        // spurious wakeup
        process_status_enum status = (process_status_enum) target.getField(LttngStrings.STATUS);
        if ((current != null && target.getId() == current.getId()) ||
                status == process_status_enum.WAIT_CPU) {
            log.entry("sched_wakeup SELF_WAKEUP " + target);
            return;
        }
        if (status == process_status_enum.WAIT_BLOCKED ||
                status == process_status_enum.WAIT_FORK ||
                status == process_status_enum.UNNAMED) {
            target.setField(LttngStrings.STATUS, process_status_enum.WAIT_CPU);
            return;
        }
        System.err.println("sched_wakeup target " + target + " in invalid state: " + target.getField(LttngStrings.STATUS));
    }

    private void handleSchedProcessExit(CtfTmfEvent event) {
        String host = event.getTrace().getHostId();
        int cpu = event.getCPU();
        Long tid = EventField.getLong(event, LttngStrings.TID);
        TmfWorker task = system.getWorker(host, cpu, tid);
        if (task == null) {
            return;
        }
        task.setEnd(event.getTimestamp().getValue());
        task.setField(LttngStrings.STATUS, process_status_enum.EXIT);
    }

    private void handleSchedProcessExec(CtfTmfEvent event) {
        String host = event.getTrace().getHostId();
        int cpu = event.getCPU();
        String filename = EventField.getString(event, LttngStrings.FILENAME);
        TmfWorker task = system.getWorkerCpu(host, cpu);
        task.setName(filename);

        // check if this task needs to be monitored
        for (String c : filter.getCommands()) {
            if (filename.matches(c)) {
                filter.addTid(task.getId());
                break;
            }
        }
    }

    private void handleExitSyscall(CtfTmfEvent event) {
        String host = event.getTrace().getHostId();
        int cpu = event.getCPU();
        TmfWorker task = system.getWorkerCpu(host, cpu);
        if (task == null) {
            return;
        }

        // return to user-space
        task.setField(LttngStrings.MODE, execution_mode_enum.USER_MODE);
    }

}
