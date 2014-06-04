package org.eclipse.linuxtools.tmf.analysis.graph.core.staging;

/**
 * POJO interval
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
public class TaskInterval {

    public Task task;
    public Long start;
    public Long end;
    public Integer state;
    public Task child;

    public TaskInterval(Task task, Long start, Long end, Integer state, Task child) {
        this.task = task; this.start = start; this.end = end; this.state = state; this.child = child;
    }

}
