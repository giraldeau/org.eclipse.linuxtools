/*******************************************************************************
 * Copyright (c) 2014 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francis Giraldeau - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.analysis.graph.core.staging;

import org.eclipse.linuxtools.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfEvent;

/**
 * Generate blocking interval tree
 *
 * @author Francis Giraldeau
 *
 */
public class ExecGraphStateProvider extends AbstractTmfStateProvider {

    /**
     * Version number of this state provider. Please bump this if you modify the
     * contents of the generated state history in some way.
     */
    private static final int VERSION = 1;

    private EventHandler fHandler;

    private StateSystemTaskListener listener;

    /**
     * Instantiate a new state provider plugin.
     *
     * @param trace
     *            The LTTng 2.0 kernel trace directory
     */
    public ExecGraphStateProvider(ITmfTrace trace) {
        super(trace, CtfTmfEvent.class, "LTTng Kernel"); //$NON-NLS-1$
    }

    // ------------------------------------------------------------------------
    // IStateChangeInput
    // ------------------------------------------------------------------------

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public void assignTargetStateSystem(ITmfStateSystemBuilder ssb) {
        super.assignTargetStateSystem(ssb);
        listener = new StateSystemTaskListener(ssb);
        fHandler = new EventHandler();
        fHandler.addListener(listener);
    }

    @Override
    public ExecGraphStateProvider getNewInstance() {
        return new ExecGraphStateProvider(this.getTrace());
    }

    @Override
    protected void eventHandle(ITmfEvent ev) {
        fHandler.eventHandle((CtfTmfEvent) ev);
    }

    @Override
    protected void handleDone() {
        fHandler.handleDone();
    }

}