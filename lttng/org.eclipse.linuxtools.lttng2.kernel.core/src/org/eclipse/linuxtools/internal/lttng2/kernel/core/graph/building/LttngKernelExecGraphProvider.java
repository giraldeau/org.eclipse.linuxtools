/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.linuxtools.internal.lttng2.kernel.core.LttngStrings;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.handlers.EventContextHandler;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.handlers.TraceEventHandlerExecutionGraph;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.handlers.TraceEventHandlerSched;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfEdge.EdgeType;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfVertex;
import org.eclipse.linuxtools.tmf.analysis.graph.core.building.AbstractTmfGraphProvider;
import org.eclipse.linuxtools.tmf.analysis.graph.core.building.AbstractTraceEventHandler;
import org.eclipse.linuxtools.tmf.analysis.graph.core.building.AnalysisPhase;
import org.eclipse.linuxtools.tmf.analysis.graph.core.building.TmfGraphBuildRequest;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelRegistry;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelResource;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelResourceDeclaration;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfSystemModel;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfSystemModelWithCpu;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfWorker;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfWorkerDeclaration;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventField;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfTraceManager;


/**
 * Graph provider to build the lttng kernel execution graph
 *
 * @author Geneviève Bastien
 * @since 3.1
 */
public class LttngKernelExecGraphProvider extends AbstractTmfGraphProvider {

    @SuppressWarnings("javadoc")
    public enum HRTimerState {
        INIT, START, CANCEL
    }

    @SuppressWarnings("javadoc")
    public enum Context {
        SOFTIRQ, IRQ, HRTIMER, NONE
    }

    @SuppressWarnings("javadoc")
    public enum thread_type_enum {
        USER_THREAD(0),
        KERNEL_THREAD(1);
        private final int val;

        private thread_type_enum(int val) {
            this.val = val;
        }

        public int value() {
            return val;
        }
    }

    @SuppressWarnings("javadoc")
    public enum execution_mode_enum {
        USER_MODE(0),
        SYSCALL(1),
        TRAP(2),
        IRQ(3),
        SOFTIRQ(4),
        MODE_UNKNOWN(5);
        private final int val;

        private execution_mode_enum(int val) {
            this.val = val;
        }

        public int value() {
            return val;
        }

        static public execution_mode_enum getMode(long val) {
            for (execution_mode_enum e : execution_mode_enum.values()) {
                if (e.value() == val) {
                    return e;
                }
            }
            return MODE_UNKNOWN;
        }
    }

    @SuppressWarnings("javadoc")
    public enum process_status_enum {
        UNNAMED(0),
        WAIT_FORK(1),
        WAIT_CPU(2),
        EXIT(3),
        ZOMBIE(4),
        WAIT_BLOCKED(5),
        RUN(6),
        DEAD(7);
        private final int val;

        private process_status_enum(int val) {
            this.val = val;
        }

        public int value() {
            return val;
        }

        static public process_status_enum getStatus(long val) {
            for (process_status_enum e : process_status_enum.values()) {
                if (e.value() == val) {
                    return e;
                }
            }
            return UNNAMED;
        }
    }


    /**
     * Version number of this state provider. Please bump this if you modify the
     * contents of the generated state history in some way.
     */
    private static final int VERSION = 1;

    /* All phases variables */
    private TmfSystemModelWithCpu fSystem;
    private long fPrev;
    private long numStatedumpEnd;

    /**
     * Constructor
     *
     * @param registry
     *            The model registry to use
     * @param trace
     *            The trace on which to build graph
     */
    public LttngKernelExecGraphProvider(TmfModelRegistry registry, ITmfTrace trace) {
        super(registry, trace, ITmfEvent.class, "LTTng Kernel"); //$NON-NLS-1$
        fSystem = getModelRegistry().getOrCreateModel(TmfSystemModelWithCpu.class);
        fSystem.init(this);

        // FIXME: support mix of kernel and UST traces
        // count the number of traces with statedump
        numStatedumpEnd = TmfTraceManager.getTraceSet(trace).length;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public void eventHandle(ITmfEvent ev) {

        if (getCurrentPhase() == null) {
            return;
        }

        /*
         * AbstractTmfGraphProvider should have already checked for the correct
         * class type
         */
        super.eventHandle(ev.getType().getName(), ev);
    }

    @Override
    public List<AnalysisPhase> makeAnalysis() {
        List<AnalysisPhase> phases = new ArrayList<>();
        /* Phase 1: Determine initial state */
        AnalysisPhase phase1 = new AnalysisPhase(Messages.LttngKernelExecGraphProvider_AnalysePhase1, new TmfGraphBuildRequest(this));
        String[] events = {LttngStrings.STATEDUMP_PROCESS_STATE,
                LttngStrings.STATEDUMP_END,
                LttngStrings.STATEDUMP_FD,
                LttngStrings.STATEDUMP_INET_SOCK
                };
        phase1.registerHandler(events, new TraceEventHandlerPhaseOne());
        phases.add(phase1);

        /* Phase 2: Build the graph */
        AnalysisPhase phase2 = new AnalysisPhase(Messages.LttngKernelExecGraphProvider_AnalysePhase2, new TmfGraphBuildRequest(this));
        phase2.registerHandler(TraceEventHandlerSched.getHandledEvents(), new TraceEventHandlerSched(this));
        phase2.registerHandler(EventContextHandler.getHandledEvents(), new EventContextHandler(this));
        phase2.registerHandler(TraceEventHandlerExecutionGraph.getHandledEvents(), new TraceEventHandlerExecutionGraph(this));
        phases.add(phase2);
        return phases;
    }

    @Override
    public void initializeModel(TmfSystemModel model) {

        // The worker
        TmfWorkerDeclaration worker = new TmfWorkerDeclaration(LttngKernelSystemModelStrings.WORKER);
        worker.addSameField(LttngStrings.PID);
        worker.addSameField(LttngStrings.TID);
        worker.addSameField(LttngStrings.PPID);
        worker.addField(LttngStrings.TYPE);
        worker.addField(LttngStrings.MODE);
        worker.addField(LttngStrings.SUBMODE);
        worker.addKeepField(LttngStrings.STATUS);
        model.addWorkerDeclaration(worker);

        // Create resource types for this trace
        // FD
        TmfModelResourceDeclaration fd = new TmfModelResourceDeclaration(LttngKernelSystemModelStrings.RESOURCE_FD);
        fd.addField(LttngKernelSystemModelStrings.FD_NUM);
        fd.addField(LttngKernelSystemModelStrings.FD_NAME);
        model.addResourceDeclaration(fd);

        // inet4sock
        TmfModelResourceDeclaration inet4sock = new TmfModelResourceDeclaration(LttngKernelSystemModelStrings.RESOURCE_INET4SOCK);
        inet4sock.addField(LttngKernelSystemModelStrings.INET4SOCK_SK);
        inet4sock.addSameField(LttngKernelSystemModelStrings.INET4SOCK_SADDR);
        inet4sock.addSameField(LttngKernelSystemModelStrings.INET4SOCK_DADDR);
        inet4sock.addSameField(LttngKernelSystemModelStrings.INET4SOCK_SPORT);
        inet4sock.addSameField(LttngKernelSystemModelStrings.INET4SOCK_DPORT);
        inet4sock.addField(LttngKernelSystemModelStrings.INET4SOCK_ISSET);
        inet4sock.addField(LttngKernelSystemModelStrings.INET4SOCK_STARTTIME);
        inet4sock.addField(LttngKernelSystemModelStrings.INET4SOCK_ENDTIME);
        model.addResourceDeclaration(inet4sock);

        // timer
        TmfModelResourceDeclaration timer = new TmfModelResourceDeclaration(LttngKernelSystemModelStrings.RESOURCE_HRTIMER);
        timer.addSameField(LttngKernelSystemModelStrings.HRTIMER_ID);
        timer.addSameField(LttngKernelSystemModelStrings.HRTIMER_HOST_ID);
        timer.addField(LttngKernelSystemModelStrings.HRTIMER_STATE);
        model.addResourceDeclaration(timer);

        // interrupt context
        TmfModelResourceDeclaration context = new TmfModelResourceDeclaration(LttngKernelSystemModelStrings.RESOURCE_INTCONTEXT);
        context.addSameField(LttngKernelSystemModelStrings.INTCONTEXT_EVENT);
        context.addField(LttngKernelSystemModelStrings.INTCONTEXT_CONTEXT);
        model.addResourceDeclaration(context);

        TmfModelResource res = context.create();
        res.setField(LttngKernelSystemModelStrings.INTCONTEXT_CONTEXT, Context.NONE);
        model.setDefaultContextStack(res);

    }

    private boolean setupEvent(ITmfEvent event) {
        fSystem.setCurrentCPU(event.getTrace().getHostId(), Integer.parseInt(event.getSource())); // update current CPU

        /* Check if timestamp is later than previous */
        long ts = event.getTimestamp().getValue();

        if (fPrev > ts) {
            getCurrentPhase().cancel(new RuntimeException("Error: prev timestamps is greater than current timestamps")); //$NON-NLS-1$
            return false;
        }
        fPrev = ts;

        return true;
    }

    /**
     * Simplify graph after construction
     */
    @Override
    public void done() {
        TmfGraph g = getAssignedGraph();
        Set<Object> keys = g.getNodesMap().keySet();
        ArrayList<TmfWorker> kernelWorker = new ArrayList<>();
        /* build the set of worker to eliminate */
        for (Object k: keys) {
            if (k instanceof TmfWorker) {
                TmfWorker w = (TmfWorker) k;
                if (w.getId() == -1) {
                    kernelWorker.add(w);
                }
            }
        }
        for (TmfWorker k: kernelWorker) {
            List<TmfVertex> nodes = g.getNodesOf(k);
            for(TmfVertex node: nodes) {
                /* send -> recv */
                if (node.hasNeighbor(TmfVertex.INV) &&
                        node.hasNeighbor(TmfVertex.OUTH)) {
                    TmfVertex next = node.neighbor(TmfVertex.OUTH);
                    if (!next.hasNeighbor(TmfVertex.OUTV) && node.hasNeighbor(TmfVertex.OUTH)) {
                        next = node.neighbor(TmfVertex.OUTH);
                    }
                    if (next.hasNeighbor(TmfVertex.OUTV)) {
                        TmfVertex src = node.neighbor(TmfVertex.INV);
                        TmfVertex dst = next.neighbor(TmfVertex.OUTV);
                        //TmfWorker sender = (TmfWorker) g.getParentOf(src);
                        //TmfWorker receiver = (TmfWorker) g.getParentOf(dst);
                        /* unlink */
                        node.getEdges()[TmfVertex.INV] = null;
                        next.getEdges()[TmfVertex.OUTV] = null;
                        src.linkVertical(dst).setType(EdgeType.NETWORK);
                    }
                }
            }
        }
    }

    private class TraceEventHandlerPhaseOne extends AbstractTraceEventHandler {

        private long statedumpEndCount = 0;

        @Override
        public void handleEvent(ITmfEvent ev) {

            if (!setupEvent(ev)) {
                return;
            }

            String evname = ev.getType().getName();
            String host = ev.getTrace().getHostId();
            int cpu = Integer.parseInt(ev.getSource());
            final ITmfEventField content = ev.getContent();

            switch (evname) {
            case LttngStrings.STATEDUMP_PROCESS_STATE:
            {
                long pid = (Long) content.getField(LttngStrings.PID).getValue();
                long tid = (Long) content.getField(LttngStrings.TID).getValue();
                long ppid = (Long) content.getField(LttngStrings.PPID).getValue();
                long type = (Long) content.getField(LttngStrings.TYPE).getValue();
                long mode = (Long) content.getField(LttngStrings.MODE).getValue();
                long submode = (Long) content.getField(LttngStrings.SUBMODE).getValue();
                long status = (Long) content.getField(LttngStrings.STATUS).getValue();
                String name = (String) content.getField(LttngStrings.NAME).getValue();

                TmfWorkerDeclaration wd = fSystem.getWorkerDeclaration(LttngKernelSystemModelStrings.WORKER);
                TmfWorker task = wd.create(tid, ev.getTrace());
                task.setStart(ev.getTrace().getStartTime().getValue());
                task.setField(LttngStrings.PID, pid);
                task.setField(LttngStrings.TID, tid);
                task.setField(LttngStrings.PPID, ppid);
                task.setField(LttngStrings.MODE, execution_mode_enum.getMode(mode));
                task.setField(LttngStrings.SUBMODE, submode);
                task.setField(LttngStrings.STATUS, process_status_enum.getStatus(status));
                task.setField(LttngStrings.TYPE, type);
                task.setName(name);
                fSystem.putWorker(task);
            }
                break;
            case LttngStrings.STATEDUMP_END: // lttng_statedump_end
                statedumpEndCount++;
                if (statedumpEndCount == numStatedumpEnd) {
                    cancel();
                }
                break;
            case LttngStrings.STATEDUMP_FD: // lttng_statedump_fd
            {
                long pid = (Long) content.getField(LttngStrings.PID).getValue();
                String filename = (String) content.getField(LttngStrings.FILENAME).getValue();
                long fd = (Long) content.getField(LttngStrings.FD).getValue();
                TmfWorker task = fSystem.getWorker(host, cpu, pid);
                TmfModelResource resfd = fSystem.getResourceDeclaration(LttngKernelSystemModelStrings.RESOURCE_FD).create();
                resfd.setField(LttngKernelSystemModelStrings.FD_NUM, fd);
                resfd.setField(LttngKernelSystemModelStrings.FD_NAME, filename);
                fSystem.addWorkerResource(task, resfd);
                // fSystem.addTaskFD(task, new FD(fd, filename));
            }
                break;
            case LttngStrings.STATEDUMP_INET_SOCK: // lttng_statedump_inet_sock
            {
                long pid = (Long) content.getField(LttngStrings.PID).getValue();
                long sk = (Long) content.getField(LttngKernelSystemModelStrings.INET4SOCK_SK).getValue();

                TmfModelResource resinet = fSystem.getResourceDeclaration(LttngKernelSystemModelStrings.RESOURCE_INET4SOCK).create();
                resinet.setField(LttngKernelSystemModelStrings.INET4SOCK_SK, sk);
                TmfWorker task = fSystem.getWorker(host, cpu, pid);
                fSystem.addWorkerResource(task, resinet);
            }
                break;
            default:
                break;
            }
        }
    }

}
