package org.eclipse.linuxtools.tmf.analysis.graph.core.staging;

import java.util.EnumMap;

import org.eclipse.linuxtools.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.linuxtools.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.linuxtools.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.linuxtools.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.linuxtools.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.EventHandler.Ctx;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Task.StateEnum;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class StateSystemTaskListener implements ITaskListener {

    /**
     * Attributes keys in the state system
     *
     * @author Francis Giraldeau <francis.giraldeau@gmail.com>
     *
     */
    public enum Attributes {
        TASK("task"), //$NON-NLS-1$
        CPU("cpu"), //$NON-NLS-1$
        STATE("state"); //$NON-NLS-1$
        private final String label;
        private Attributes(String label) {
            this.label = label;
        }
        public String label() {
            return label;
        }
    }

    Table<String, Long, EnumMap<Attributes, Integer>> quarkTidCache; // (host, tid, (attr, quark))
    Table<String, Long, EnumMap<Attributes, Integer>> quarkCpuCache; // (host, cpu, (attr, quark))
    private ITmfStateSystemBuilder ss;



    public StateSystemTaskListener(ITmfStateSystemBuilder ssb) {
        this.ss = ssb;
        quarkTidCache = HashBasedTable.create();
        quarkCpuCache = HashBasedTable.create();
    }

    @Override
    public void stateChange(Ctx ctx, Task task, StateEnum state) {
        try {
            updateCpuState(ctx, task, state);
            updateTaskState(ctx, task, state);
        } catch (StateValueTypeException | AttributeNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("doStateChange() failed"); //$NON-NLS-1$
        }
    }

    // store directly the quark of the task (instead of it's TID)
    private void updateCpuState(Ctx ctx, Task task, StateEnum state) throws StateValueTypeException, AttributeNotFoundException {
        if (state == StateEnum.RUNNING) {
            Integer qTask = getOrCreateQuark(quarkTidCache, ctx.hostId, Attributes.TASK, task.getTID(), Attributes.STATE);
            Integer qCpuState = getOrCreateQuark(quarkCpuCache, ctx.hostId, Attributes.CPU, ctx.cpu.longValue(), Attributes.STATE);
            ITmfStateValue value = TmfStateValue.newValueInt(qTask);
            ss.modifyAttribute(ctx.ts, value, qCpuState);
        }
    }

    /**
     * @param state the next task state
     */
    private void updateTaskState(Ctx ctx, Task task, StateEnum state) throws StateValueTypeException, AttributeNotFoundException {
        Integer qTaskState = getOrCreateQuark(quarkTidCache, ctx.hostId, Attributes.TASK, task.getTID(), Attributes.STATE);
        long packed = 0;
        switch (task.getState()) {
        case WAIT_UNKNOWN:
            packed = PackedLongValue.pack(task.getState().value(), 0);
            break;
        case WAIT_CPU:
            Integer qCPU = getOrCreateQuark(quarkCpuCache, ctx.hostId, Attributes.CPU, ctx.cpu.longValue(), Attributes.STATE);
            packed = PackedLongValue.pack(task.getState().value(), qCPU);
            break;
        case WAIT_TASK:
            Task wup = ctx.wakeupSource;
            Integer qwup = getOrCreateQuark(quarkTidCache, wup.getHostID(), Attributes.TASK, wup.getTID(), Attributes.STATE);
            packed = PackedLongValue.pack(task.getState().value(), qwup);
            break;
        case EXIT:
        case RUNNING:
        case UNKNOWN:
        case WAIT_BLOCK_DEV:
        case WAIT_NETWORK:
        case WAIT_TIMER:
        case WAIT_USER_INPUT:
        case INTERRUPTED:
        default:
            packed = task.getState().value();
            break;
        }
        ITmfStateValue value = TmfStateValue.newValueLong(packed);
        ss.modifyAttribute(task.getLastUpdate(), value, qTaskState);

        // cleanup
        if (state == StateEnum.EXIT) {
            value = TmfStateValue.newValueLong(StateEnum.EXIT.value());
            ss.modifyAttribute(ctx.ts, value, qTaskState);
            quarkTidCache.remove(task.getHostID(), task.getTID());
        }
    }

    private Integer getOrCreateQuark(Table<String, Long, EnumMap<Attributes, Integer>> cache, String host, Attributes type, Long id, Attributes attribute) {
        if (!cache.contains(host, id)) {
            String clean = host.replaceAll("^\"|\"$", "");  //$NON-NLS-1$ //$NON-NLS-2$ // unquote
            // create all attributes
            EnumMap<Attributes, Integer> quarks = new EnumMap<>(Attributes.class);
            for (Attributes attr: Attributes.values()) {
                int quark = ss.getQuarkAbsoluteAndAdd(clean, type.label(), id.toString(), attr.label());
                quarks.put(attr, quark);
            }
            cache.put(host, id, quarks);
        }
        return cache.get(host, id).get(attribute);
    }

    @Override
    public void stateFlush(Task task) {
        Integer qTaskState = getOrCreateQuark(quarkTidCache, task.getHostID(), Attributes.TASK, task.getTID(), Attributes.STATE);
        ITmfStateValue value = TmfStateValue.newValueLong(task.getState().value());
        try {
            ss.modifyAttribute(task.getLastUpdate(), value, qTaskState);
        } catch (StateValueTypeException | AttributeNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
