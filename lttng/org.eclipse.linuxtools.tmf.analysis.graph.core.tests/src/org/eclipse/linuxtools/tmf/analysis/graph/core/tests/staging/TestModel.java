package org.eclipse.linuxtools.tmf.analysis.graph.core.tests.staging;

import static org.junit.Assert.*;

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

}
