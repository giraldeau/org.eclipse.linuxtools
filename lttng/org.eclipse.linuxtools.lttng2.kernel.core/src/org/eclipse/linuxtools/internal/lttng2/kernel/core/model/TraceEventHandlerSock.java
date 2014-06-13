package org.eclipse.linuxtools.internal.lttng2.kernel.core.model;

import java.util.Collection;

import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building.LttngKernelExecGraphProvider;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building.LttngKernelSystemModelStrings;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelElementDeclaration;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelElementFilter;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelResource;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelResourceDeclaration;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfSystemModelWithCpu;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfWorker;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfEvent;

public class TraceEventHandlerSock extends TraceEventHandlerBase {

	private TmfSystemModelWithCpu system;

	private ALog log;

	public TraceEventHandlerSock() {
		super();
		this.hooks.add(new TraceHook("inet_connect"));
		this.hooks.add(new TraceHook("inet_accept"));
		this.hooks.add(new TraceHook("inet_sock_clone"));
		this.hooks.add(new TraceHook("inet_sock_delete"));
		this.hooks.add(new TraceHook("inet_sock_create"));
	}

	@Override
	public void handleInit(LttngKernelExecGraphProvider reader) {
		system = reader.getModelRegistry().getOrCreateModel(TmfSystemModelWithCpu.class);
		system.init(reader);
		log = ALog.getInstance();
	}

	@Override
	public void handleComplete(LttngKernelExecGraphProvider reader) {
		Collection<TmfModelResource> socks = system.getResources(system.getResourceDeclaration(LttngKernelSystemModelStrings.RESOURCE_INET4SOCK));
		long now = reader.getTrace().getEndTime().getValue();
		for (TmfModelResource sock: socks) {
			sock.setField("end_time",now);
		}
	}

	private TmfModelElementFilter getSockFilter(long sk) {
	    TmfModelElementDeclaration decl = system.getResourceDeclaration(LttngKernelSystemModelStrings.RESOURCE_INET4SOCK);

        TmfModelElementFilter filter = new TmfModelElementFilter(decl);
        filter.setValue("sk", sk);
        return filter;
	}

	private TmfModelResource createSock(long sk) {
	    TmfModelResourceDeclaration decl = system.getResourceDeclaration(LttngKernelSystemModelStrings.RESOURCE_INET4SOCK);
	    TmfModelResource sock = decl.create();
        sock.setField("sk", sk);
        return sock;
	}

	public void defineInet4Sock(CtfTmfEvent event) {
        String host = event.getTrace().getHostId();
        int cpu = event.getCPU();
		long sk = EventField.getLong(event, "sk");
		long saddr = EventField.getLong(event, "saddr");
		long daddr = EventField.getLong(event, "daddr");
		long sport = EventField.getLong(event, "sport");
		long dport = EventField.getLong(event, "dport");
		TmfWorker task = system.getWorkerCpu(host, cpu);

		TmfModelElementFilter filter = getSockFilter(sk);

		TmfModelResource sock = system.find(task, filter);

		if (sock == null) {
			log.entry(String.format("missing inet_sock_create for sock 0x%x", sk));
			sock = createSock(sk);
			system.addWorkerResource(task, sock);
		}
		sock.setField("saddr", (int)saddr);
		sock.setField("daddr", (int)daddr);
		sock.setField("sport", (int)sport);
		sock.setField("dport", (int)dport);
//		system.matchPeer(sock);
	}

	public void handle_inet_connect(CtfTmfEvent event) {
		defineInet4Sock(event);
	}

	public void handle_inet_accept(CtfTmfEvent event) {
		defineInet4Sock(event);
	}

	public void handle_inet_sock_clone(CtfTmfEvent event) {
		long osk = EventField.getLong(event, "osk");
		long nsk = EventField.getLong(event, "nsk");
		TmfModelElementFilter filter = getSockFilter(osk);
		TmfModelResource oldSock = system.find(filter);
		if (oldSock == null) {
			log.entry("cloning unkown sock osk=" +
					Long.toHexString(osk) + " at " + event.getTimestamp().getValue());

			return;
		}
		TmfWorker owner = system.getResourceOwner(oldSock);
		TmfModelResource newSock = createSock(nsk);
		newSock.setField("saddr", oldSock.getField("saddr"));
		newSock.setField("daddr", oldSock.getField("daddr"));
		newSock.setField("sport", oldSock.getField("sport"));
		newSock.setField("dport", oldSock.getField("dport"));
		newSock.setField("startTime", event.getTimestamp().getValue());
		system.addWorkerResource(owner, newSock);
	}

	public void handle_inet_sock_create(CtfTmfEvent event) {
        String host = event.getTrace().getHostId();
        int cpu = event.getCPU();
		TmfWorker current = system.getWorkerCpu(host, cpu);
		long sk = EventField.getLong(event, "sk");

		TmfModelResource sock = createSock(sk);
		sock.setField("startTime", event.getTimestamp().getValue());
		system.addWorkerResource(current, sock);
	}

	public void handle_inet_sock_delete(CtfTmfEvent event) {
		// TODO: add state to Inet4Sock instead of delete
		long sk = EventField.getLong(event, "sk");
		TmfModelElementFilter filter = getSockFilter(sk);
        TmfModelResource sock = system.find(filter);
        sock.setField("endTime", event.getTimestamp().getValue());
	}

}