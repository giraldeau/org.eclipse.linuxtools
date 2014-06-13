package org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.handlers;

import java.util.HashMap;

import org.eclipse.linuxtools.internal.lttng2.kernel.core.LttngStrings;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building.LttngKernelSystemModelStrings;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.model.EventField;
import org.eclipse.linuxtools.tmf.analysis.graph.core.building.AbstractTmfGraphProvider;
import org.eclipse.linuxtools.tmf.analysis.graph.core.building.AbstractTraceEventHandler;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelElementFilter;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelResource;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelResourceDeclaration;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfSystemModelWithCpu;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfWorker;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfEvent;

public class TraceEventHandlerFD extends AbstractTraceEventHandler {

	/* Keep tmp info until corresponding sys_exit */
	public enum EventType { SYS_CLOSE, SYS_OPEN, SYS_DUP2 }
	public class EventData {
		public EventType type;
		public String name;
		public long fd;
		public long oldfd;
		public long newfd;
	}

	TmfSystemModelWithCpu system;

	HashMap<Long, EventData> evHistory;

	public static String[] getHandledEvents() {
        return new String[] { LttngStrings.SYS_OPEN, LttngStrings.SYS_CLOSE,
                LttngStrings.SYS_DUP2, LttngStrings.EXIT_SYSCALL };
    }

	public TraceEventHandlerFD(AbstractTmfGraphProvider reader) {
		super();
		system = reader.getModelRegistry().getOrCreateModel(TmfSystemModelWithCpu.class);
        system.init(reader);
        evHistory = new HashMap<>();

	}

    @Override
    public void handleEvent(ITmfEvent ev) {
        CtfTmfEvent event = (CtfTmfEvent) ev;
        String eventName = event.getType().getName();
        if (LttngStrings.SYS_OPEN.equals(eventName)) {
            handleSysOpen(event);
        } else if (LttngStrings.SYS_CLOSE.equals(eventName)) {
            handleSysClose(event);
        } else if (LttngStrings.SYS_DUP2.equals(eventName)) {
            handleSysDup2(event);
        } else if (LttngStrings.EXIT_SYSCALL.equals(eventName)) {
            handleExitSyscall(event);
        }
    }

	private void handleSysOpen(CtfTmfEvent event) {
	    String host = event.getTrace().getHostId();
		int cpu = event.getCPU();
		TmfWorker task = system.getWorkerCpu(host, cpu);
		if (task == null) {
            return;
        }
		EventData ev = new EventData();
		ev.name = EventField.getString(event, "filename");
		ev.type = EventType.SYS_OPEN;
		evHistory.put(task.getId(), ev);
	}

	private void handleSysClose(CtfTmfEvent event) {
	    String host = event.getTrace().getHostId();
		int cpu = event.getCPU();
		TmfWorker task = system.getWorkerCpu(host, cpu);
		if (task == null) {
            return;
        }
		EventData ev = new EventData();
		ev.fd = EventField.getLong(event, "fd");
		ev.type = EventType.SYS_CLOSE;
		evHistory.put(task.getId(), ev);
	}

	private void handleSysDup2(CtfTmfEvent event) {
        String host = event.getTrace().getHostId();
        int cpu = event.getCPU();
        TmfWorker task = system.getWorkerCpu(host, cpu);
		if (task == null) {
            return;
        }
		EventData ev = new EventData();
		ev.oldfd = EventField.getLong(event, "oldfd");
		ev.newfd = EventField.getLong(event, "newfd");
		ev.type = EventType.SYS_DUP2;
		evHistory.put(task.getId(), ev);
	}

	private void handleExitSyscall(CtfTmfEvent event) {
        String host = event.getTrace().getHostId();
        int cpu = event.getCPU();
        TmfWorker task = system.getWorkerCpu(host, cpu);
		if (task == null) {
            return;
        }
		long ret = EventField.getLong(event, "ret");
		EventData ev = evHistory.remove(task.getId());
		if (ev == null) {
            return;
        }
		TmfModelResourceDeclaration decl = system.getResourceDeclaration(LttngKernelSystemModelStrings.RESOURCE_FD);

		switch (ev.type) {
		case SYS_CLOSE:
			if (ret == 0) {
			    TmfModelElementFilter filter = new TmfModelElementFilter(decl);
		        filter.setValue("num", ev.fd);
		        TmfModelResource fd = system.find(task, filter);
		        system.removeResource(task, fd);
//				FDSet fdSet = system.getFDSet(task);
//				FD fd = fdSet.getFD(ev.fd);
//				fdSet.remove(fd);
			}
			break;
		case SYS_OPEN:
			if (ret >= 0) {
			    TmfModelResource fd = decl.create();
			    fd.setField("num", ret);
			    fd.setField("name", ev.name);
			    system.addWorkerResource(task, fd);
			}
			break;
		case SYS_DUP2:
			if (ret >= 0) {
				// verify system call success
				assert(ret == ev.newfd);

				// dup2 does nothing if oldfd == newfd
				if (ev.oldfd != ev.newfd) {
				    TmfModelElementFilter filter = new TmfModelElementFilter(decl);
	                filter.setValue("num", ev.oldfd);
	                TmfModelResource ofd = system.find(task, filter);

	                String name = null;
	                if (ofd != null) {
	                    name = (String)ofd.getField("name");
	                }
	                TmfModelResource nfd = decl.create();
	                nfd.setField("num", ev.newfd);
	                nfd.setField("name", name);
	                system.removeResource(task,  ofd);
	                system.addWorkerResource(task,  nfd);
				}
			}
			break;
		default:
			break;
		}

	}


}
