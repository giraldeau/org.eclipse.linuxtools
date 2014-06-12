package org.eclipse.linuxtools.tmf.analysis.graph.core.tests.staging;

import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Task;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Task.StateEnum;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.algo.IntervalVisitor;

public class CountIntervalVisitor implements IntervalVisitor {

    private int count = 0;

    public CountIntervalVisitor() {
        count = 0;
    }

    @Override
    public void visit(Task task, StateEnum state, long t1, long t2) {
        System.out.println(task + " " + state + " [" + t1 + "," + t2 + "]");
        count++;
    }

    public int getCount() {
        return count;
    }

}