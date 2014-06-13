package org.eclipse.linuxtools.internal.lttng2.kernel.core.model;

import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfWorker;

import com.google.common.collect.ArrayListMultimap;

public class TaskBlockings {

	private final ArrayListMultimap<TmfWorker, TaskBlockingEntry> entries;

	public TaskBlockings() {
		entries = ArrayListMultimap.create();
	}

	public ArrayListMultimap<TmfWorker, TaskBlockingEntry> getEntries() {
		return entries;
	}

}
