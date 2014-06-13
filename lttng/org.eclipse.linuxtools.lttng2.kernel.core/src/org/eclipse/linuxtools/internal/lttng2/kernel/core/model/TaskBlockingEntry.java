package org.eclipse.linuxtools.internal.lttng2.kernel.core.model;

import java.util.Comparator;

import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfWorker;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfEvent;

/*
 * Represents when a task is blocked waiting
 */
public class TaskBlockingEntry {

	private CtfTmfEvent syscall;
	private WakeupInfo wakeup;
	private TmfWorker task;
	private final TimeInterval interval;

	public static final Comparator<TaskBlockingEntry> cmpStart = new Comparator<TaskBlockingEntry>() {
		@Override
		public int compare(TaskBlockingEntry e0, TaskBlockingEntry e1) {
			long s0 = e0.getInterval().getStart();
			long s1 = e1.getInterval().getStart();
			return s0 > s1 ? 1 : ( s0 == s1 ? 0 : -1);
		}
	};

	public static final Comparator<TaskBlockingEntry> cmpEnd = new Comparator<TaskBlockingEntry>() {
		@Override
		public int compare(TaskBlockingEntry e0, TaskBlockingEntry e1) {
			long s0 = e0.getInterval().getEnd();
			long s1 = e1.getInterval().getEnd();
			return s0 > s1 ? 1 : ( s0 == s1 ? 0 : -1);
		}
	};

	public TaskBlockingEntry() {
		interval = new TimeInterval(0, 0);
	}

	public CtfTmfEvent getSyscall() {
		return syscall;
	}

	public void setSyscall(CtfTmfEvent syscall) {
		this.syscall = syscall;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("[");
		if (syscall != null) {
			str.append(syscall.getType().getName());
		} else {
			str.append("none");
		}
		str.append("]");
		return str.toString();
	}

	public void setWakeupInfo(WakeupInfo info) {
		this.wakeup = info;
	}

	public WakeupInfo getWakeupInfo() {
		return this.wakeup;
	}

	public TmfWorker getTask() {
		return task;
	}

	public void setTask(TmfWorker task) {
		this.task = task;
	}

	public TimeInterval getInterval() {
		return interval;
	}

}
