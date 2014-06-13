package org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.handlers;

import org.eclipse.linuxtools.internal.lttng2.kernel.core.LttngStrings;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building.LttngKernelSystemModelStrings;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building.LttngKernelExecGraphProvider.Context;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.model.ALog;
import org.eclipse.linuxtools.tmf.analysis.graph.core.building.AbstractTmfGraphProvider;
import org.eclipse.linuxtools.tmf.analysis.graph.core.building.AbstractTraceEventHandler;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelResource;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelResourceDeclaration;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfSystemModelWithCpu;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfEvent;

@SuppressWarnings("nls")
public class EventContextHandler extends AbstractTraceEventHandler {

	private TmfSystemModelWithCpu system;
	private ALog log;

	public static String[] getHandledEvents() {
        return new String[] { LttngStrings.SOFTIRQ_ENTRY, LttngStrings.SOFTIRQ_EXIT,
                LttngStrings.HRTIMER_EXPIRE_ENTRY, LttngStrings.HRTIMER_EXPIRE_EXIT,
                LttngStrings.IRQ_HANDLER_ENTRY, LttngStrings.IRQ_HANDLER_EXIT };
    }

	public EventContextHandler(AbstractTmfGraphProvider provider) {
		super();
        system = provider.getModelRegistry().getOrCreateModel(TmfSystemModelWithCpu.class);
        system.init(provider);
        log = ALog.getInstance();
	}


    @Override
    public void handleEvent(ITmfEvent ev) {
        CtfTmfEvent event = (CtfTmfEvent) ev;
        String eventName = event.getType().getName();
        if (LttngStrings.SOFTIRQ_ENTRY.equals(eventName)) {
            handleSoftirqEntry(event);
        } else if (LttngStrings.SOFTIRQ_EXIT.equals(eventName)) {
            handleSoftirqExit(event);
        } else if (LttngStrings.HRTIMER_EXPIRE_ENTRY.equals(eventName)) {
            handleHrtimerExpireEntry(event);
        } else if (LttngStrings.HRTIMER_EXPIRE_EXIT.equals(eventName)) {
            handleHrtimerExpireExit(event);
        } else if (LttngStrings.IRQ_HANDLER_ENTRY.equals(eventName)) {
            handleIrqHandlerEntry(event);
        } else if (LttngStrings.IRQ_HANDLER_EXIT.equals(eventName)) {
            handleIrqHandlerExit(event);
        }
    }

	private void pushInterruptContext(CtfTmfEvent event, Context ctx) {
	    TmfModelResourceDeclaration decl = system.getResourceDeclaration(LttngKernelSystemModelStrings.RESOURCE_INTCONTEXT);
	    TmfModelResource resource = decl.create();
	    resource.setField(LttngKernelSystemModelStrings.INTCONTEXT_EVENT, event);
	    resource.setField(LttngKernelSystemModelStrings.INTCONTEXT_CONTEXT, ctx);
	    system.pushContextStack(event.getTrace().getHostId(), event.getCPU(), resource);
	}

	private void popInterruptContext(CtfTmfEvent event, Context ctx) {
	    TmfModelResource interruptCtx = system.peekContextStack(event.getTrace().getHostId(), event.getCPU());
		if (interruptCtx == null) {
			log.entry("popInterruptContext stack is empty " + event.toString());
			return;
		}
		if ((Context)interruptCtx.getField(LttngKernelSystemModelStrings.INTCONTEXT_CONTEXT) == ctx) {
			system.popContextStack(event.getTrace().getHostId(), event.getCPU());
		} else {
			log.entry("popInterruptContext unexpected top stack context " + event);
		}
	}

	private void handleSoftirqEntry(CtfTmfEvent event) {
		pushInterruptContext(event, Context.SOFTIRQ);
	}

	private void handleSoftirqExit(CtfTmfEvent event) {
		popInterruptContext(event, Context.SOFTIRQ);
	}

	private void handleIrqHandlerEntry(CtfTmfEvent event) {
		pushInterruptContext(event, Context.IRQ);
	}

	private void handleIrqHandlerExit(CtfTmfEvent event) {
		popInterruptContext(event, Context.IRQ);
	}

	private void handleHrtimerExpireEntry(CtfTmfEvent event) {
		pushInterruptContext(event, Context.HRTIMER);
	}

	private void handleHrtimerExpireExit(CtfTmfEvent event) {
		popInterruptContext(event, Context.HRTIMER);
	}

}
