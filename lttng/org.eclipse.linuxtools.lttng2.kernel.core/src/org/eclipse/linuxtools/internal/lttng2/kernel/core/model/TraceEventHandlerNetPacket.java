package org.eclipse.linuxtools.internal.lttng2.kernel.core.model;

import java.util.HashMap;

import org.eclipse.linuxtools.internal.lttng2.kernel.core.LttngStrings;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building.LttngKernelExecGraphProvider;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfSystemModelWithCpu;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfEvent;

public class TraceEventHandlerNetPacket extends TraceEventHandlerBase {

	enum Type { SEND, RECV }

	class Event {
		public long ts;
		public long sk;
		public int seq;
		public Type type;
		public Event(long ts, long sk, int seq, Type type) {
			this.ts = ts;
			this.sk = sk;
			this.seq = seq;
			this.type = type;
		}
	}

	// FIXME: seq is not unique, should add port?

	HashMap<Integer, Event> match; // (seq, ev)
	private TmfSystemModelWithCpu system;

	public TraceEventHandlerNetPacket() {
		super();
		this.hooks.add(new TraceHook("inet_sock_local_in"));
		this.hooks.add(new TraceHook("inet_sock_local_out"));
	}

	@Override
	public void handleInit(LttngKernelExecGraphProvider reader) {
		system = reader.getModelRegistry().getOrCreateModel(TmfSystemModelWithCpu.class);
		system.init(reader);
		match = new HashMap<>();
	}

	@Override
	public void handleComplete(LttngKernelExecGraphProvider reader) {
	}

	public Event makeEvent(CtfTmfEvent event, Type type) {
		long sk = EventField.getLong(event, LttngStrings.SK);
		int seq = EventField.getLong(event, LttngStrings.SEQ).intValue();
		return new Event(event.getTimestamp().getValue(), sk, seq, type);
	}

	public void handle_inet_sock_local_in(CtfTmfEvent event) {
		Event recv = makeEvent(event, Type.RECV);
		if (recv.sk == 0) {
            return;
        }

		Event send = match.remove(recv.seq);
		if (send == null)
         {
            return;
		//System.out.println("RECV " + recv.sk + " " + recv.seq);
        }

		assert(send.seq == recv.seq);
		assert(send.type == Type.SEND);
		assert(recv.type == Type.RECV);
		/*
		Actor sender = getOrCreateActor(send.sk);
		Actor receiver = getOrCreateActor(recv.sk);
		msgs.add(new Message(sender, send.ts, receiver, recv.ts));
		*/
	}

	public void handle_inet_sock_local_out(CtfTmfEvent event) {
		Event send = makeEvent(event, Type.SEND);
		//System.out.println("SEND " + send.sk + " " + send.seq);
		match.put(send.seq, send);
	}

}
