/*******************************************************************************
 * Copyright (c) 2012, 2013 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Patrick Tasse - Initial API and implementation
 *   Bernd Hufmann - Updated signal handling
 *   Geneviève Bastien - Move code to provide base classes for time graph view
 *   Marc-Andre Laperle - Add time zone preference
 *   Geneviève Bastien - Add event links between entries
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.ui.views.timegraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.linuxtools.tmf.core.signal.TmfSignalHandler;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * An abstract view all time graph views can inherit
 *
 * This view contains either a time graph viewer, or a time graph combo which is
 * divided between a tree viewer on the left and a time graph viewer on the right.
 *
 * @since 4.0
 */
public abstract class AbstractTimeGraphPerObjectView extends AbstractTimeGraphView {

    /** The trace to entry list hash map */
    private final Map<ITmfTrace, Map<Object, List<TimeGraphEntry>>> fObjectEntryListMap = new HashMap<>();

    /** Current object the actual entry list is associated to */
    private Object fCurrentObject;

    /**
     * Build thread for object of traces
     */
    protected final class BuildThreadObject extends BuildThread {

        private final Object fObject;

        /**
         * Constructor
         *
         * @param trace
         *            The trace the build thread is for
         * @param obj
         *            Object the build thread is for
         * @param name
         *            Name of the thread
         */
        public BuildThreadObject(final ITmfTrace trace, Object obj, final String name) {
            super(trace, null, name);
            fObject = obj;
        }

        @Override
        public void run() {
            buildEventList(fBuildTrace, fObject, fMonitor);
            synchronized (fBuildThreadMap) {
                fBuildThreadMap.remove(this);
            }
        }

    }

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Constructor
     *
     * @param id
     *            The id of the view
     * @param pres
     *            The presentation provider
     */
    public AbstractTimeGraphPerObjectView(String id, TimeGraphPresentationProvider pres) {
        super(id, pres);
    }

    // ------------------------------------------------------------------------
    // Getters and setters
    // ------------------------------------------------------------------------

    /**
     * Gets the entry list map
     *
     * @return the entry list map
     */
    protected Map<ITmfTrace, Map<Object, List<TimeGraphEntry>>> getObjectEntryListMap() {
        return Collections.unmodifiableMap(fObjectEntryListMap);
    }

    private Map<Object, List<TimeGraphEntry>> getCurrentObjectEntryList() {
        if (fObjectEntryListMap.get(getTrace()) == null) {
            fObjectEntryListMap.put(getTrace(), new HashMap<Object, List<TimeGraphEntry>>());
        }
        return fObjectEntryListMap.get(getTrace());
    }

    /**
     * Adds an entry to the entry list for a trace's object
     *
     * @param obj
     *            the object to add
     * @param list
     *            The list of time graph entries
     */
    protected void putObjectEntryList(Object obj, List<TimeGraphEntry> list) {
        synchronized(fObjectEntryListMap) {
            getCurrentObjectEntryList().put(obj, list);
        }
    }

    // ------------------------------------------------------------------------
    // Signal handlers
    // ------------------------------------------------------------------------

    /**
     * Trace is closed: clear the data structures and the view
     *
     * @param signal
     *            the signal received
     */
    @Override
    @TmfSignalHandler
    public void traceClosed(final TmfTraceClosedSignal signal) {
        super.traceClosed(signal);
        synchronized (fObjectEntryListMap) {
            fObjectEntryListMap.remove(signal.getTrace());
        }
        if (getTrace() == null) {
            fCurrentObject = null;
            refresh();
        }
    }

    // ------------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------------

    /**
     * Load the entries for a current object
     *
     * @param obj
     *            The object to load entries for
     */
    protected void loadObject(Object obj) {
        fCurrentObject = obj;
        synchronized (fObjectEntryListMap) {
            fEntryList = getCurrentObjectEntryList().get(fCurrentObject);
            if (fEntryList == null) {
                synchronized (fBuildThreadMap) {
                    BuildThread buildThread = new BuildThreadObject(getTrace(), fCurrentObject, getName());
                    /* remove other build thread for this trace (only one should be working at a time */
                    BuildThread oldBuildThread = fBuildThreadMap.remove(getTrace());
                    if (oldBuildThread != null) {
                        oldBuildThread.cancel();
                    }
                    fBuildThreadMap.put(getTrace(), buildThread);
                    buildThread.start();
                }
            } else {
                setStartTime(getTrace().getStartTime().normalize(0, ITmfTimestamp.NANOSECOND_SCALE).getValue());
                setEndTime(getTrace().getEndTime().normalize(0, ITmfTimestamp.NANOSECOND_SCALE).getValue());
                refresh();
            }
        }
    }

    @Override
    protected final void updateEntryList() {
        synchronized (fObjectEntryListMap) {
            fEntryList = getCurrentObjectEntryList().get(fCurrentObject);
            if (fEntryList == null) {
                fEntryList = new ArrayList<>();
            }
        }
    }

    @Override
    protected final void buildEventList(ITmfTrace trace, ITmfTrace parentTrace, IProgressMonitor monitor) {
        buildEventList(parentTrace, fCurrentObject, monitor);
    }

    /**
     * Build the entries list to show in this time graph
     *
     * Called from the BuildThread
     *
     * @param trace
     *            The trace being built
     * @param object
     *            The object for which to build event list
     * @param monitor
     *            The progress monitor object
     */
    protected abstract void buildEventList(ITmfTrace trace, Object object, IProgressMonitor monitor);

}
