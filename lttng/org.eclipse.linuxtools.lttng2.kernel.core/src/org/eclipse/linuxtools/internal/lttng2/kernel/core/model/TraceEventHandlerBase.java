package org.eclipse.linuxtools.internal.lttng2.kernel.core.model;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building.LttngKernelExecGraphProvider;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfEvent;

public class TraceEventHandlerBase implements ITraceEventHandler {

	private static int autoPriority = 0;
	protected Set<TraceHook> hooks;
	private final Integer priority;
	private static Class<?>[] argTypes = new Class<?>[] { CtfTmfEvent.class };

	public TraceEventHandlerBase(Integer priority) {
		super();
		this.hooks = new HashSet<>();
		this.priority = priority;
	}

	public TraceEventHandlerBase() {
		this(autoPriority++);
	}

	@Override
	public Set<TraceHook> getHooks() {
		return hooks;
	}

	public void setHooks(Set<TraceHook> hooks) {
		this.hooks = hooks;
	}

	@Override
	public void handleInit(LttngKernelExecGraphProvider reader) {

	}

	@Override
	public void handleComplete(LttngKernelExecGraphProvider reader) {

	}

	@Override
	public Integer getPriority() {
		return priority;
	}

	@Override
	public int compareTo(ITraceEventHandler other) {
		return priority.compareTo(other.getPriority());
	}

	@Override
	public String toString() {
		String name = this.getClass().getSimpleName();
		return "[" + name + "," + getPriority() + "]";
	}

    public void handle(CtfTmfEvent event) {
        String evname = event.getType().getName();

        for (TraceHook hook: hooks) {
            boolean runHook = false;
            if (hook.isAllEvent()) {
                runHook = true;
                if (hook.method == null) {
                    String method = "handle_all_event";
                    hook.instance = this;
                    try {
                        hook.method = this.getClass().getMethod(method, argTypes);
                    } catch (SecurityException e) {
                        e.printStackTrace();

                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                }
            }
            else if (hook.eventName.equals(evname)) {
                runHook = true;
                if (hook.method == null) {
                    String method = "handle_" + evname;
                    hook.instance = this;
                    try {
                        hook.method = this.getClass().getMethod(method, argTypes);
                    } catch (SecurityException e) {
                        e.printStackTrace();

                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (runHook) {
                try {
                    hook.method.invoke(hook.instance, event);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    System.err.println("error while executing " + hook.method + " on " + hook.instance);
                    e.printStackTrace();
                }
            }
        }

    }
}
