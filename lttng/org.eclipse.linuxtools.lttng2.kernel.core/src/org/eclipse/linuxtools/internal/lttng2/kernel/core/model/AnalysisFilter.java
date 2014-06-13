package org.eclipse.linuxtools.internal.lttng2.kernel.core.model;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfWorker;

public class AnalysisFilter  {

	HashSet<Long> tids;
	HashSet<String> commands;
	private boolean followChild;
	private boolean wildcard;

	public AnalysisFilter() {
		tids = new HashSet<>();
		commands = new HashSet<>();
		setWildcard(false);
	}


	public void addTid(Long tid) {
		if (tid != null) {
            tids.add(tid);
        }
	}

	public Set<Long> getTids() {
		return tids;
	}

	public void setFollowChild(boolean followChild) {
		this.followChild = followChild;
	}

	public boolean isFollowChild() {
		return this.followChild;
	}

	public void addCommand(String comm) {
		if (comm != null) {
            commands.add(comm);
        }
	}

	public Set<String> getCommands() {
		return commands;
	}

	public boolean containsTaskTid(TmfWorker task) {
		if (task == null) {
            return false;
        }
		if (isWildcard()) {
            return true;
        }
		return tids.contains(task.getId());
	}

	public boolean isWildcard() {
		return wildcard;
	}

	public void setWildcard(boolean wildcard) {
		this.wildcard = wildcard;
	}

}