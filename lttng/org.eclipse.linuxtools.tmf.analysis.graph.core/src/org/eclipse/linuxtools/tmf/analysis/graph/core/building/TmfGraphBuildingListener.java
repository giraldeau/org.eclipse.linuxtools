/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien and Francis Giraldeau - Initial implementation and API
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.analysis.graph.core.building;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.osgi.util.NLS;

/**
 * Listener class for graph building with analysis phases. Will show an ongoing
 * progress bar with percentage completed
 *
 * @since 3.0
 */
public class TmfGraphBuildingListener implements ITmfGraphBuildingListener {

    IProgressMonitor fMonitor;
    double fScale = 0.0001;
    long fDelta, fPrev, fInterval = 1000000; // 1ms
    long fMin = 0, fMax = 0;
    double fPrevStep = 0;
    int fProgressMax = 100, fPhaseMax = 0;
    int fProgressPhase = 0;
    int fNumPhases = 1, fPhase = 0;

    /**
     * Constructor
     *
     * @param numPhases
     *            Number of phases to build graph
     * @param monitor
     *            A progress monitor
     */
    public TmfGraphBuildingListener(int numPhases, IProgressMonitor monitor) {
        setLoadingListener(monitor);
        int vNumPhases = numPhases;
        if (numPhases < 1) {
            vNumPhases = 1;
        }
        this.fNumPhases = vNumPhases;
        this.fProgressPhase = fProgressMax / vNumPhases;
    }

    @Override
    public void begin() {
        fMonitor.beginTask(Messages.TmfGraphBuildingListener_Waiting, fProgressMax);
    }

    @Override
    public void progress(long time) {
        fDelta += time - fPrev;
        if (fDelta > fInterval) {
            fDelta = 0;
            fPrev = time;
            double step;
            if (fMin != 0) {
                step = ((time - fMin) * fScale);
                step += (fPhase - 1) * fProgressPhase;
            } else {
                step = fPrevStep + ((fPhaseMax - fPrevStep) * fScale);
            }
            setStep(step);
        }

    }

    private void setStep(double step) {
        int iStep = (int) step;
        int iPrevStep = (int) fPrevStep;
        if (iStep != iPrevStep) {
            fMonitor.worked(iStep - iPrevStep);
        }
        fPrevStep = step;
    }

    @Override
    public void finished() {
        fMonitor.done();
    }

    private void setLoadingListener(IProgressMonitor monitor) {
        if (monitor == null) {
            this.fMonitor = new NullProgressMonitor();
        } else {
            this.fMonitor = monitor;
        }
    }

    @Override
    public void phase(int phase) {
        fPhase = phase;
        this.fMonitor.setTaskName(NLS.bind(Messages.TmfGraphBuildingListener_PhaseProgress, phase, fNumPhases));

        fPhaseMax = (fProgressMax * phase / fNumPhases);
        setStep(fProgressMax * (phase - 1) / fNumPhases);
    }

    @Override
    public boolean isCanceled() {
        return this.fMonitor.isCanceled();
    }

    @Override
    public void setTimeRange(ITmfTimestamp startTime, ITmfTimestamp endTime) {
        // Only update the time if start and end are not the same
        if (startTime.getValue() < endTime.getValue()) {
            fMin = startTime.getValue();
            fMax = endTime.getValue();
            fScale = 1.0f / (fNumPhases * ((double) fMax - fMin)) * fProgressMax;
        }
    }

}
