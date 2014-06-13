package org.eclipse.linuxtools.internal.lttng2.kernel.core.model;

import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfWorker;



public class WakeupInfo {

	public enum Type { TIMER, SOCK, WAITPID }

	public long vec;
	public long sk;
	public long seq;
	public long timer;
	public TmfWorker awakener;
	public Type type;

	@Override
	public String toString() {
		return "[wakeup " + type + "]";
	}

}
