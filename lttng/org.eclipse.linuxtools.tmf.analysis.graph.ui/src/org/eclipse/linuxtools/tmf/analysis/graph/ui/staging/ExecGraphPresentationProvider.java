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

package org.eclipse.linuxtools.tmf.analysis.graph.ui.staging;

import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Task.StateEnum;
import org.eclipse.linuxtools.tmf.analysis.graph.ui.criticalpath.Messages;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.swt.graphics.RGB;

/**
 * Presentation provider for the critical path view
 */
public class ExecGraphPresentationProvider extends TimeGraphPresentationProvider {

    /**
     * The enumeration of possible states for the view
     */
    public static enum State {
        /** Worker is running */
        RUNNING         (0, new RGB(0x33, 0x99, 0x00)),
        /** Worker is interrupted */
        INTERRUPTED     (1, new RGB(0xff, 0xdc, 0x00)),
        /** Worker has been preempted */
        WAIT_UNKOWN     (2, new RGB(0xf4, 0x48, 0x00)),
        /** Worker has been preempted */
        WAIT_CPU        (3, new RGB(0xc8, 0x64, 0x00)),
        /** Worker is waiting another task */
        WAIT_TASK       (4, new RGB(0xcc, 0xff, 0x99)),
        /** Worker waiting on a timer */
        WAIT_TIMER      (5, new RGB(0x33, 0x66, 0x99)),
        /** Worker is blocked, waiting on a device */
        WAIT_BLOCK_DEV  (6, new RGB(0x66, 0x00, 0xcc)),
        /** Worker is waiting for user input */
        WAIT_USER_INPUT (7, new RGB(0x5a, 0x01, 0x01)),
        /** Worker is waiting on network */
        WAIT_NETWORK    (8, new RGB(0xff, 0x9b, 0xff)),
        /** Exit **/
        EXIT            (9, new RGB(0xff, 0xff, 0xff)),
        /** Any other reason */
        UNKNOWN         (10, new RGB(0x88, 0x88, 0x88));

        /** RGB color associated with a state */
        public final RGB rgb;
        private final Integer index;
        private State (Integer index, RGB rgb) {
            this.index = index;
            this.rgb = rgb;
        }

        public Integer index() {
            return index;
        }

    }

    @Override
    public String getStateTypeName() {
        return Messages.CriticalFlowView_stateTypeName;
    }

    @Override
    public StateItem[] getStateTable() {
        StateItem[] stateTable = new StateItem[State.values().length];
        for (int i = 0; i < stateTable.length; i++) {
            State state = State.values()[i];
            stateTable[i] = new StateItem(state.rgb, state.toString());
        }
        return stateTable;
    }

    @Override
    public int getStateTableIndex(ITimeEvent event) {
        if (event instanceof TimeEvent && ((TimeEvent) event).hasValue()) {
            return getMatchingState(((TimeEvent) event).getValue()).index();
        }
        return TRANSPARENT;
    }

    private static State getMatchingState(int status) {
        StateEnum state = StateEnum.fromValue(status);
        switch(state) {
        case EXIT:
            return State.EXIT;
        case INTERRUPTED:
            return State.INTERRUPTED;
        case RUNNING:
            return State.RUNNING;
        case WAIT_UNKNOWN:
            return State.WAIT_UNKOWN;
        case WAIT_BLOCK_DEV:
            return State.WAIT_BLOCK_DEV;
        case WAIT_CPU:
            return State.WAIT_CPU;
        case WAIT_NETWORK:
            return State.WAIT_NETWORK;
        case WAIT_TASK:
            return State.WAIT_TASK;
        case WAIT_TIMER:
            return State.WAIT_TIMER;
        case WAIT_USER_INPUT:
            return State.WAIT_USER_INPUT;
        case UNKNOWN:
        default:
            return State.UNKNOWN;
        }
    }

    @Override
    public String getEventName(ITimeEvent event) {
        if (event instanceof TimeEvent) {
            TimeEvent ev = (TimeEvent) event;
            if (ev.hasValue()) {
                return getMatchingState(ev.getValue()).toString();
            }
        }
        return Messages.CriticalFlowView_multipleStates;
    }
}

