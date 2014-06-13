package org.eclipse.linuxtools.internal.lttng2.kernel.core.model;

import java.util.Set;

import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building.LttngKernelExecGraphProvider;

public interface ITraceEventHandler extends Comparable<ITraceEventHandler> {

	public Set<TraceHook> getHooks();

	public void handleInit(LttngKernelExecGraphProvider builder);

	public void handleComplete(LttngKernelExecGraphProvider builder);

	public Integer getPriority();
}
