package org.eclipse.linuxtools.lttng2.kernel.core.graph.sht;

import org.eclipse.linuxtools.lttng2.kernel.core.graph.sht.EventHandler.Ctx;
import org.eclipse.linuxtools.lttng2.kernel.core.graph.sht.Task.StateEnum;

public interface ITaskListener {

    public void stateChange(Ctx ctx, Task task, StateEnum state);

}
