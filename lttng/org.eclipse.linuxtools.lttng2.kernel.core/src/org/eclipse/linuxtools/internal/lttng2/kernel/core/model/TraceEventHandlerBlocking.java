package org.eclipse.linuxtools.internal.lttng2.kernel.core.model;

import java.util.HashMap;

import org.eclipse.linuxtools.internal.lttng2.kernel.core.LttngStrings;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building.LttngKernelExecGraphProvider;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building.LttngKernelExecGraphProvider.process_status_enum;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.model.WakeupInfo.Type;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfSystemModelWithCpu;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfWorker;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfEvent;

/*
 * Detect blocking in a process
 */
public class TraceEventHandlerBlocking extends TraceEventHandlerBase {

	public long count;
	private TaskBlockings blockings;
	private HashMap<TmfWorker, CtfTmfEvent> syscall;
	private WakeupInfo[] wakeup;
	private HashMap<TmfWorker, TaskBlockingEntry> latestBlockingMap;

	private TmfSystemModelWithCpu system;
	//private AnalysisFilter filter;

	public TraceEventHandlerBlocking() {
		super();
		this.hooks.add(new TraceHook());
		this.hooks.add(new TraceHook("sched_switch"));
		this.hooks.add(new TraceHook("sched_wakeup"));
		this.hooks.add(new TraceHook("exit_syscall"));
		this.hooks.add(new TraceHook("softirq_entry"));
		this.hooks.add(new TraceHook("softirq_exit"));
		this.hooks.add(new TraceHook("hrtimer_expire_entry"));
		this.hooks.add(new TraceHook("hrtimer_expire_exit"));
		this.hooks.add(new TraceHook("inet_sock_local_in"));
	}

	@Override
	public void handleInit(LttngKernelExecGraphProvider reader) {
		system = reader.getModelRegistry().getOrCreateModel(TmfSystemModelWithCpu.class);
		system.init(reader);
		blockings = reader.getModelRegistry().getOrCreateModel(TaskBlockings.class);
		// FIXME: change wakeup to HashMap
		//wakeup = new WakeupInfo[reader.getNumCpu(reader.getTrace())];
		wakeup = new WakeupInfo[10];
		syscall = new HashMap<>();
		latestBlockingMap = new HashMap<>();
	}

	@Override
	public void handleComplete(LttngKernelExecGraphProvider reader) {
	}

	public void handle_softirq_entry(CtfTmfEvent event) {
		long vec = EventField.getLong(event, "vec");
		WakeupInfo info = new WakeupInfo();
		info.vec = vec;
		wakeup[event.getCPU()] = info;
	}

	public void handle_softirq_exit(CtfTmfEvent event) {
		wakeup[event.getCPU()] = null;
	}

	public void handle_hrtimer_expire_entry(CtfTmfEvent event) {
		long timer = EventField.getLong(event, "hrtimer");
		WakeupInfo info = new WakeupInfo();
		info.type = Type.TIMER;
		info.timer = timer;
		wakeup[event.getCPU()] = info;
	}

	public void handle_hrtimer_expire_exit(CtfTmfEvent event) {
		wakeup[event.getCPU()] = null;
	}

	public void handle_inet_sock_local_in(CtfTmfEvent event) {
		long sk = EventField.getLong(event, "sk");
		long seq = EventField.getLong(event, "seq");
		WakeupInfo info = wakeup[event.getCPU()];
		if (info != null) {
			info.type = WakeupInfo.Type.SOCK;
			info.sk = sk;
			info.seq = seq;
		}
	}

	public void handle_sched_switch(CtfTmfEvent event) {
        String host = event.getTrace().getHostId();
        int cpu = event.getCPU();
		long state = EventField.getLong(event, "prev_state");
		long prevTid = EventField.getLong(event, "prev_tid");
		long nextTid = EventField.getLong(event, "next_tid");

		// task is blocking
		if (state >= 1) {
			TmfWorker prevTask = system.getWorker(host, cpu, prevTid);
			TaskBlockingEntry entry = new TaskBlockingEntry();
			entry.getInterval().setStart(event.getTimestamp().getValue());
			latestBlockingMap.put(prevTask, entry);
			/*
			if (filter.containsTaskTid(prevTask)) {
				System.out.println("sched_switch task is blocking " + prevTask + " " + event.getTimestamp());
			}
			*/
		}
		// task may be scheduled after wake-up
		TmfWorker nextTask = system.getWorker(host, cpu, nextTid);
		TaskBlockingEntry entry = latestBlockingMap.remove(nextTask);
		if (entry != null) {
			entry.getInterval().setEnd(event.getTimestamp().getValue());
			/*
			if (filter.containsTaskTid(nextTask))
				System.out.println("sched_switch task is waking up " + nextTask + " " + event.getTimestamp());
			*/
		}
	}

	public void handle_all_event(CtfTmfEvent event) {
        String host = event.getTrace().getHostId();
        int cpu = event.getCPU();
		TmfWorker task = system.getWorkerCpu(host, cpu);
		String name = event.getType().getName();
		// record the current system call
		if (name.startsWith("sys_")) {
			syscall.put(task, event);
		}
	}

	public void handle_exit_syscall(CtfTmfEvent event) {
        String host = event.getTrace().getHostId();
        int cpu = event.getCPU();
        TmfWorker task = system.getWorkerCpu(host, cpu);
		syscall.remove(task);
	}

	public void handle_sched_wakeup(CtfTmfEvent event) {
        String host = event.getTrace().getHostId();
        int cpu = event.getCPU();
 		long tid = EventField.getLong(event, "tid");
		TmfWorker blockedTask = system.getWorker(host, cpu, tid);

		if (blockedTask == null) {
            return;
        }

		// spurious wake-up
		if (blockedTask.getField(LttngStrings.STATUS) != process_status_enum.WAIT_BLOCKED) {
			wakeup[event.getCPU()] = null;
			return;
		}

		TaskBlockingEntry blocking = latestBlockingMap.get(blockedTask);
		if (blocking == null) {
            return;
        }
		/*
		if (filter.containsTaskTid(blockedTask))
			System.out.println("sched_wakeup " + blockedTask + " " + blockedTask.getProcessStatus() + " " + blocking);
		*/
		blocking.setSyscall(syscall.get(blockedTask));
		blocking.setTask(blockedTask);
		blocking.setWakeupInfo(wakeup[event.getCPU()]);
		blockings.getEntries().put(blockedTask, blocking);
	}

}