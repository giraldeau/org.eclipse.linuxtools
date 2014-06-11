package org.eclipse.linuxtools.tmf.analysis.graph.core.staging;

import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.EventHandler.Ctx;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Task.StateEnum;

public interface ITaskListener {

    public void stateChange(Ctx ctx, Task task, StateEnum state);

    public void stateFlush(Task task);

}
