package org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.handlers;

import org.eclipse.linuxtools.internal.lttng2.kernel.core.LttngStrings;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building.LttngKernelSystemModelStrings;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building.LttngKernelExecGraphProvider.HRTimerState;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.model.EventField;
import org.eclipse.linuxtools.tmf.analysis.graph.core.building.AbstractTmfGraphProvider;
import org.eclipse.linuxtools.tmf.analysis.graph.core.building.AbstractTraceEventHandler;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelElementFilter;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelResource;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelResourceDeclaration;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfSystemModelWithCpu;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfEvent;

public class TraceEventHandlerHRTimer extends AbstractTraceEventHandler {

    TmfSystemModelWithCpu system;

	/*
	 * hrtimer_cancel:
     * hrtimer_init:
     * hrtimer_start:
	 */
    public static String[] getHandledEvents() {
        return new String[] { LttngStrings.HRTIMER_CANCEL, LttngStrings.HRTIMER_INIT,
                LttngStrings.HRTIMER_START };
    }

	public TraceEventHandlerHRTimer(AbstractTmfGraphProvider provider) {
		super();
		system = provider.getModelRegistry().getOrCreateModel(TmfSystemModelWithCpu.class);
        system.init(provider);
	}


	private TmfModelResource getOrCreateHRTimer(long id, CtfTmfEvent event) {
	    TmfModelResourceDeclaration decl = system.getResourceDeclaration(LttngKernelSystemModelStrings.RESOURCE_HRTIMER);
	    TmfModelElementFilter filter = new TmfModelElementFilter(decl);
        filter.setValue("id", id);
        filter.setValue(LttngKernelSystemModelStrings.HRTIMER_HOST_ID, event.getTrace().getHostId());
        TmfModelResource timer = system.find(filter);

		if (timer == null) {
			timer = decl.create();
			timer.setField("id", id);
			timer.setField(LttngKernelSystemModelStrings.HRTIMER_HOST_ID, event.getTrace().getHostId());
			timer.setField("state", HRTimerState.INIT);
			system.addResource(timer);
		}
		return timer;
	}

	private void handleHRTimerEventGeneric(CtfTmfEvent event, HRTimerState state) {
		long hrtimer = EventField.getLong(event, "hrtimer");
		TmfModelResource timer = getOrCreateHRTimer(hrtimer, event);
		timer.setField("state", state);
	}

	private void handleHrTimerInit(CtfTmfEvent event) {
		handleHRTimerEventGeneric(event, HRTimerState.INIT);
	}

	private void handleHrTimerStart(CtfTmfEvent event) {
		handleHRTimerEventGeneric(event, HRTimerState.START);
	}

	private void handleHrTimerCancel(CtfTmfEvent event) {
		handleHRTimerEventGeneric(event, HRTimerState.CANCEL);
	}

    @Override
    public void handleEvent(ITmfEvent ev) {
        CtfTmfEvent event = (CtfTmfEvent) ev;
        String eventName = event.getType().getName();
        if (LttngStrings.HRTIMER_INIT.equals(eventName)) {
            handleHrTimerInit(event);
        } else if (LttngStrings.HRTIMER_CANCEL.equals(eventName)) {
            handleHrTimerCancel(event);
        } else if (LttngStrings.HRTIMER_START.equals(eventName)) {
            handleHrTimerStart(event);
        }
    }

}
