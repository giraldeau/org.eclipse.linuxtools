package org.eclipse.linuxtools.tmf.analysis.graph.core.tests.staging;

import static org.junit.Assert.assertEquals;

import org.eclipse.linuxtools.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.linuxtools.statesystem.core.StateSystemFactory;
import org.eclipse.linuxtools.statesystem.core.backend.IStateHistoryBackend;
import org.eclipse.linuxtools.statesystem.core.backend.InMemoryBackend;
import org.eclipse.linuxtools.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.linuxtools.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.linuxtools.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Task.StateEnum;
import org.junit.Test;

public class TestModel {

    /**
     * Test the task state enum fromValue()
     */
    @Test
    public void testTaskState() {
        for (StateEnum s: StateEnum.values()) {
            assertEquals(s, StateEnum.fromValue(s.value()));
        }
    }

    /**
     * Is it possible to modify attributes, even if the time is less than another interval
     * @throws StateValueTypeException
     * @throws AttributeNotFoundException
     */
    @Test
    public void testStateSystemInsertOrder() throws StateValueTypeException, AttributeNotFoundException {
        IStateHistoryBackend backend = new InMemoryBackend(0);
        ITmfStateSystemBuilder ss = StateSystemFactory.newStateSystem("foo", backend);
        int qFoo = ss.getQuarkAbsoluteAndAdd("foo");
        int qBar = ss.getQuarkAbsoluteAndAdd("bar");
        TmfStateValue v1 = TmfStateValue.newValueLong(1);
        TmfStateValue v2 = TmfStateValue.newValueLong(2);
        ss.modifyAttribute(10L, v1, qFoo);
        ss.modifyAttribute(30L, v1, qBar);
        ss.modifyAttribute(20L, v2, qFoo);
    }

}
