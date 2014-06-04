package org.eclipse.linuxtools.tmf.analysis.graph.core.staging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

/**
 * POJO state object for efficiency
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
public class Machine {

    public String host;
    public ArrayList<Task> swappers;    // (cpu, swappers)
    public ArrayList<Long> current;     // (cpu, tid)
    public ArrayList<Stack<Interrupt>> irq; // (cpu, stack(irq))
    public HashMap<Long, Task> tasks;   // (tid, task)

    public Machine(String host) {
        this.host = host;
        this.current = new ArrayList<>();
        this.swappers = new ArrayList<>();
        this.tasks = new HashMap<>();
        this.irq = new ArrayList<>();
    }

    public Machine() {
        this("");
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Machine)) {
            return false;
        }
        Machine other = (Machine) o;
        return other.host.equals(this.host);
    }

    @Override
    public int hashCode() {
        return this.host.hashCode();
    }

    public void update(int cpu) {
        // ensure CPUs
        if (cpu >= current.size()) {
            int add = cpu - current.size() + 1;
            for (int i = 0; i < add; i++) {
                current.add(-1L);
                Task swapper = new Task(host, 0, 0);
                swapper.setComm(String.format("swapper/%d", (cpu + i)));
                swappers.add(swapper);
                Stack stack = new Stack();
                irq.add(stack);
            }
        }
    }

    public Long getCurrentTid(int cpu) {
        update(cpu);
        return current.get(cpu);
    }

    public void setCurrentTid(Integer cpu, long tid) {
        update(cpu);
        current.set(cpu, tid);
    }

    public Task getOrCreateTask(Integer cpu, Long tid, Long ts) {
        update(cpu);
        if (tid > 0L) {
            Task task = tasks.get(tid);
            if (task == null) {
                task = new Task(host, tid, ts);
                tasks.put(tid, task);
            }
            return task;
        }
        return swappers.get(cpu);
    }

    public Stack<Interrupt> getInterruptStack(Integer cpu) {
        update(cpu);
        return irq.get(cpu);
    }

    public void removeTask(Task task) {
        tasks.remove(task.getTID());
    }

}