package org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.handlers;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.linuxtools.internal.lttng2.kernel.core.LttngStrings;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.TcpEventStrings;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building.LttngKernelExecGraphProvider.Context;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building.LttngKernelExecGraphProvider.process_status_enum;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building.LttngKernelSystemModelStrings;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.model.EventField;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.model.Softirq;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfEdge;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfEdge.EdgeType;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfVertex;
import org.eclipse.linuxtools.tmf.analysis.graph.core.building.AbstractTmfGraphProvider;
import org.eclipse.linuxtools.tmf.analysis.graph.core.building.AbstractTraceEventHandler;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelResource;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfSystemModelWithCpu;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfWorker;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfWorkerDeclaration;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.matching.IMatchProcessingUnit;
import org.eclipse.linuxtools.tmf.core.event.matching.TmfEventDependency;
import org.eclipse.linuxtools.tmf.core.event.matching.TmfNetworkEventMatching;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTraceWithPreDefinedEvents;
import org.eclipse.linuxtools.tmf.core.trace.TmfEventTypeCollectionHelper;
import org.eclipse.linuxtools.tmf.core.trace.TmfTraceManager;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfEvent;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * @author Geneviève Bastien
 *
 */
public class TraceEventHandlerExecutionGraph extends AbstractTraceEventHandler {

    AbstractTmfGraphProvider fProvider;
    TmfSystemModelWithCpu system;
    Table<String, Integer, TmfWorker> kernel;
    TmfGraph graph;
    IMatchProcessingUnit matchProcessing;
    HashMap<ITmfEvent, TmfVertex> tcpNodes;
    TmfNetworkEventMatching tcpMatching;
    private Map<ITmfTrace, Boolean> fHasEventSchedTTWU;

    public static String[] getHandledEvents() {
        return new String[] { LttngStrings.SCHED_SWITCH, LttngStrings.SCHED_WAKEUP_NEW,
                LttngStrings.SCHED_WAKEUP, LttngStrings.SCHED_TTWU, LttngStrings.SOFTIRQ_ENTRY,
                TcpEventStrings.INET_SOCK_LOCAL_IN, TcpEventStrings.INET_SOCK_LOCAL_OUT,
                TcpEventStrings.NET_DEV_QUEUE, TcpEventStrings.NETIF_RECEIVE_SKB };
    }

    public TraceEventHandlerExecutionGraph(AbstractTmfGraphProvider provider) {
        super();
        fProvider = provider;
        system = provider.getModelRegistry().getOrCreateModel(TmfSystemModelWithCpu.class);
        system.init(provider);
        graph = provider.getAssignedGraph();
        kernel = HashBasedTable.create();

        // init graph
        Collection<TmfWorker> tasks = system.getWorkers();
        for (TmfWorker task : tasks) {
            graph.add(task, new TmfVertex(task.getStart()));
        }
        tcpNodes = new HashMap<>();
        matchProcessing = new IMatchProcessingUnit() {

            @Override
            public void matchingEnded() {
            }

            @Override
            public void init(Collection<? extends ITmfTrace> trace) {
            }

            @Override
            public int countMatches() {
                return 0;
            }

            @Override
            public void addMatch(TmfEventDependency match) {
                TmfVertex output = tcpNodes.remove(match.getSourceEvent());
                TmfVertex input = tcpNodes.remove(match.getDestinationEvent());
                if (output != null && input != null) {
                    output.linkVertical(input).setType(EdgeType.NETWORK);
                }
            }

        };

        ITmfTrace[] traces = TmfTraceManager.getTraceSet(provider.getTrace());
        tcpMatching = new TmfNetworkEventMatching(Arrays.asList(traces), matchProcessing);
        tcpMatching.initMatching();

        fHasEventSchedTTWU = new HashMap<>();
        for (ITmfTrace traceItem: traces) {
            // FIXME: migrate to ITmfTrace
            if (traceItem instanceof ITmfTraceWithPreDefinedEvents) {
                Set<String> traceEvents = TmfEventTypeCollectionHelper.getEventNames(((ITmfTraceWithPreDefinedEvents) traceItem).getContainedEventTypes());
                fHasEventSchedTTWU.put(traceItem, traceEvents.contains(LttngStrings.SCHED_TTWU));
            }
        }
    }

    private TmfWorker getOrCreateKernelWorker(ITmfEvent event) {
        String host = event.getTrace().getHostId();
        int cpu = Integer.parseInt(event.getSource());
        if (!kernel.contains(host, cpu)) {
            TmfWorkerDeclaration wd = system.getWorkerDeclaration(LttngKernelSystemModelStrings.WORKER);
            TmfWorker k = wd.create(-1, event.getTrace());
            k.setName("kernel/" + event.getSource()); //$NON-NLS-1$
            k.setField(LttngStrings.STATUS, process_status_enum.RUN);
            k.setField(LttngStrings.PID, -1);
            k.setField(LttngStrings.PPID, -1);
            kernel.put(host, cpu, k);
        }
        return kernel.get(host, cpu);
    }

    @Override
    public void handleEvent(ITmfEvent ev) {
        CtfTmfEvent event = (CtfTmfEvent) ev;
        String eventName = event.getType().getName();

        if (LttngStrings.SCHED_SWITCH.equals(eventName)) {
            handleSchedSwitch(event);
        } else if (LttngStrings.SCHED_TTWU.equals(eventName) && fHasEventSchedTTWU.get(event.getTrace())) {
            handleSchedWakeup(event);
        } else if (LttngStrings.SCHED_WAKEUP.equals(eventName) && !fHasEventSchedTTWU.get(event.getTrace())) {
            handleSchedWakeup(event);
        } else if (LttngStrings.SCHED_WAKEUP_NEW.equals(eventName) && !fHasEventSchedTTWU.get(event.getTrace())) {
            handleSchedWakeup(event);
        } else if (LttngStrings.SOFTIRQ_ENTRY.equals(eventName)) {
            handleSoftirqEntry(event);
        } else if (TcpEventStrings.INET_SOCK_LOCAL_IN.equals(eventName) ||
                TcpEventStrings.NETIF_RECEIVE_SKB.equals(eventName)) {
            handleInetSockLocalIn(event);
        } else if (TcpEventStrings.INET_SOCK_LOCAL_OUT.equals(eventName) ||
                TcpEventStrings.NET_DEV_QUEUE.equals(eventName)) {
            handleInetSockLocalOut(event);
        }
    }

    private void handleSchedSwitch(CtfTmfEvent event) {
        String host = event.getTrace().getHostId();
        int cpu = event.getCPU();
        long ts = event.getTimestamp().getValue();
        long next = EventField.getLong(event, LttngStrings.NEXT_TID);
        long prev = EventField.getLong(event, LttngStrings.PREV_TID);

        TmfWorker nextTask = system.getWorker(host, cpu, next);
        TmfWorker prevTask = system.getWorker(host, cpu, prev);

        if (prevTask == null || nextTask == null) {
            return;
        }
        stateChange(prevTask, ts);
        stateChange(nextTask, ts);
    }

    private TmfVertex stateExtend(TmfWorker task, long ts) {
        TmfVertex node = new TmfVertex(ts);
        process_status_enum status = (process_status_enum) task.getField(LttngStrings.STATUS);
        graph.append(task, node, resolveProcessStatus(status));
        return node;
    }

    private TmfVertex stateChange(TmfWorker task, long ts) {
        TmfVertex node = new TmfVertex(ts);
        process_status_enum status = (process_status_enum) task.getOldValue(LttngStrings.STATUS);
        graph.append(task, node, resolveProcessStatus(status));
        return node;
    }

    private static EdgeType resolveProcessStatus(process_status_enum status) {
        EdgeType ret = EdgeType.UNKNOWN;
        if (status == null) {
            return ret;
        }
        switch (status) {
        case DEAD:
            break;
        case EXIT:
        case RUN:
            ret = EdgeType.RUNNING;
            break;
        case UNNAMED:
            ret = EdgeType.UNKNOWN;
            break;
        case WAIT_BLOCKED:
            ret = EdgeType.BLOCKED;
            break;
        case WAIT_CPU:
        case WAIT_FORK:
            ret = EdgeType.PREEMPTED;
            break;
        case ZOMBIE:
            ret = EdgeType.UNKNOWN;
            break;
        default:
            break;
        }
        return ret;
    }

    private void handleSchedWakeup(CtfTmfEvent event) {
        String host = event.getTrace().getHostId();
        int cpu = event.getCPU();
        long ts = event.getTimestamp().getValue();
        long tid = EventField.getLong(event, LttngStrings.TID);

        TmfWorker target = system.getWorker(host, cpu, tid);
        TmfWorker current = system.getWorkerCpu(host, cpu);
        if (target == null) {
            return;
        }

        process_status_enum status = (process_status_enum) target.getOldValue(LttngStrings.STATUS);
        if (status == null) {
            return;
        }

        switch (status) {
        case WAIT_FORK:
            if (current != null) {
                TmfVertex n0 = stateExtend(current, ts);
                TmfVertex n1 = stateChange(target, ts);
                graph.link(n0, n1);
            } else {
                stateChange(target, ts);
            }
            break;
        case WAIT_BLOCKED:
            TmfModelResource context = system.peekContextStack(host, cpu);
            if (context == null) {
                return;
            }
            switch ((Context) context.getField(LttngKernelSystemModelStrings.INTCONTEXT_CONTEXT)) {
            case HRTIMER:
                // shortcut of appendTaskNode: resolve blocking source in situ
                graph.append(target, new TmfVertex(ts), EdgeType.TIMER);
                break;
            case IRQ:
                TmfEdge l3 = graph.append(target, new TmfVertex(ts));
                if (l3 != null) {
                    l3.setType(resolveIRQ((CtfTmfEvent) context.getField(LttngKernelSystemModelStrings.INTCONTEXT_EVENT)));
                }
                break;
            case SOFTIRQ:
                TmfVertex wup = new TmfVertex(ts);
                TmfEdge l2 = graph.append(target, wup);
                if (l2 != null) {
                    l2.setType(resolveSoftirq((CtfTmfEvent) context.getField(LttngKernelSystemModelStrings.INTCONTEXT_EVENT)));
                }
                // special case for network related softirq
                Long vec = EventField.getLong((ITmfEvent) context.getField(LttngKernelSystemModelStrings.INTCONTEXT_EVENT), LttngStrings.VEC);
                if (vec == Softirq.NET_RX || vec == Softirq.NET_TX) {
                    // create edge if wake up is caused by incoming packet
                    TmfWorker k = getOrCreateKernelWorker(event);
                    TmfVertex tail = graph.getTail(k);
                    if (tail.hasNeighbor(TmfVertex.INV)) {
                        TmfVertex kwup = stateExtend(k, event.getTimestamp().getValue());
                        kwup.linkVertical(wup);
                    }
                }
                break;
            case NONE:
                // task context wakeup
                if (current != null) {
                    TmfVertex n0 = stateExtend(current, ts);
                    TmfVertex n1 = stateChange(target, ts);
                    n0.linkVertical(n1);
                } else {
                    stateChange(target, ts);
                }
                break;
            default:
                break;
            }
            break;
        case DEAD:
        case EXIT:
        case RUN:
        case UNNAMED:
        case WAIT_CPU:
        case ZOMBIE:
            break;
        default:
            break;
        }
    }

    private static EdgeType resolveIRQ(CtfTmfEvent event) {
        int vec = EventField.getLong(event, LttngStrings.IRQ).intValue();
        EdgeType ret = EdgeType.UNKNOWN;
        switch (vec) {
        case 0: // resched
            ret = EdgeType.INTERRUPTED;
            break;
        case 19: // ehci_hcd:usb, well, at least on my machine
        case 23:
            ret = EdgeType.USER_INPUT;
            break;
        default:
            ret = EdgeType.UNKNOWN;
            break;
        }
        return ret;
    }

    private static EdgeType resolveSoftirq(CtfTmfEvent event) {
        int vec = EventField.getLong(event, LttngStrings.VEC).intValue();
        EdgeType ret = EdgeType.UNKNOWN;
        switch (vec) {
        case Softirq.HRTIMER:
        case Softirq.TIMER:
            ret = EdgeType.TIMER;
            break;
        case Softirq.BLOCK:
        case Softirq.BLOCK_IOPOLL:
            ret = EdgeType.BLOCK_DEVICE;
            break;
        case Softirq.NET_RX:
        case Softirq.NET_TX:
            ret = EdgeType.NETWORK;
            break;
        case Softirq.SCHED:
            ret = EdgeType.INTERRUPTED;
            break;
        default:
            ret = EdgeType.UNKNOWN;
            break;
        }
        return ret;
    }

    private void handleInetSockLocalIn(CtfTmfEvent event) {
        Integer cpu = event.getCPU();
        String host = event.getTrace().getHostId();
        Context context = (Context) system.peekContextStack(host, cpu).getField(LttngKernelSystemModelStrings.INTCONTEXT_CONTEXT);
        if (context == Context.SOFTIRQ) {
            TmfWorker k = getOrCreateKernelWorker(event);
            TmfVertex endpoint = stateExtend(k, event.getTimestamp().getValue());
            tcpNodes.put(event, endpoint);
            tcpMatching.matchEvent(event, event.getTrace());
        }
    }

    private void handleInetSockLocalOut(CtfTmfEvent event) {
        Integer cpu = event.getCPU();
        String host = event.getTrace().getHostId();
        Context context = (Context) system.peekContextStack(host, cpu).getField(LttngKernelSystemModelStrings.INTCONTEXT_CONTEXT);
        TmfWorker sender = null;
        if (context == Context.NONE) {
            sender = system.getWorkerCpu(event.getTrace().getHostId(), event.getCPU());
        } else if (context == Context.SOFTIRQ) {
            sender = getOrCreateKernelWorker(event);
        }
        if (sender == null) {
            return;
        }
        TmfVertex endpoint = stateExtend(sender, event.getTimestamp().getValue());
        tcpNodes.put(event, endpoint);
        tcpMatching.matchEvent(event, event.getTrace());
    }

    private void handleSoftirqEntry(CtfTmfEvent event) {
        Long vec = EventField.getLong(event, LttngStrings.VEC);
        if (vec == Softirq.NET_RX || vec == Softirq.NET_TX) {
            TmfWorker k = getOrCreateKernelWorker(event);
            graph.add(k, new TmfVertex(event.getTimestamp().getValue()));
        }
    }

}
