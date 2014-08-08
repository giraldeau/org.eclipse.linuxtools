/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial implementation and API
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.core.event.matching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.TmfEvent;
import org.eclipse.linuxtools.tmf.core.request.TmfEventRequest;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * This class matches events typically network-style, ie. where some events are
 * 'send' events and the other 'receive' events or out/in events
 *
 * @author Geneviève Bastien
 * @since 3.0
 */
public class TmfNetworkEventMatching extends TmfEventMatching {

    /**
     * Hashtables for unmatches incoming events
     */
    private final Table<ITmfTrace, PacketKey, ITmfEvent> fUnmatchedIn = HashBasedTable.create();

    /**
     * Hashtables for unmatches outgoing events
     */
    private final Table<ITmfTrace, PacketKey, ITmfEvent> fUnmatchedOut = HashBasedTable.create();

    private long maxUnmatchedCount = 0;

    /**
     * @since 4.0
     */
    public static class PacketKey {
        private final long ts;
        public PacketKey(long ts) {
            this.ts = ts;
        }
        public long getTs() {
            return ts;
        }
    }

    /**
     * Enum for in and out types
     */
    public enum Direction {
        /**
         * The event is a 'receive' type of event
         */
        IN,
        /**
         * The event is a 'send' type of event
         */
        OUT,
    }

    List<IMatchMonitor> fMonitors = new ArrayList<>();

    /**
     * Constructor with multiple traces and match processing object
     *
     * @param traces
     *            The set of traces for which to match events
     */
    public TmfNetworkEventMatching(Collection<? extends ITmfTrace> traces) {
        this(traces, new TmfEventMatches());
    }

    /**
     * Constructor with multiple traces and match processing object
     *
     * @param traces
     *            The set of traces for which to match events
     * @param tmfEventMatches
     *            The match processing class
     */
    public TmfNetworkEventMatching(Collection<? extends ITmfTrace> traces, IMatchProcessingUnit tmfEventMatches) {
        super(traces, tmfEventMatches);
    }

    /**
     * Method that initializes any data structure for the event matching
     */
    @Override
    public void initMatching() {
        // Initialize the matching infrastructure (unmatched event lists)
        fUnmatchedIn.clear();
        fUnmatchedOut.clear();
        super.initMatching();
        for (IMatchMonitor monitor : fMonitors) {
            monitor.init();
        }
    }

    /**
     * Function that counts the events in a hashtable.
     *
     * @param tbl
     *            The table to count events for
     * @return The number of events
     */
    protected int countEvents(Map<PacketKey, ITmfEvent> tbl) {
        return tbl.size();
    }

    @Override
    protected MatchingType getMatchingType() {
        return MatchingType.NETWORK;
    }

    /**
     * @since 4.0
     */
    @Override
    public void startingRequest(TmfEventRequest request) {
        for (IMatchMonitor monitor : fMonitors) {
            monitor.setRequest(request);
        }
    }

    @Override
    public synchronized void matchEvent(ITmfEvent event, ITmfTrace trace) {
        if (!(getEventDefinition(event.getTrace()) instanceof ITmfNetworkMatchDefinition)) {
            return;
        }
        ITmfNetworkMatchDefinition def = (ITmfNetworkMatchDefinition) getEventDefinition(event.getTrace());

        Direction evType = def.getDirection(event);
        if (evType == null) {
            return;
        }

        /* Get the event's unique fields */
        PacketKey eventKey = def.getUniqueField(event);
        if (eventKey == null) {
            return;
        }
        Table<ITmfTrace, PacketKey, ITmfEvent> unmatchedTbl, companionTbl;

        /* Point to the appropriate table */
        switch (evType) {
        case IN:
            unmatchedTbl = fUnmatchedIn;
            companionTbl = fUnmatchedOut;
            break;
        case OUT:
            unmatchedTbl = fUnmatchedOut;
            companionTbl = fUnmatchedIn;
            break;
        default:
            return;
        }

        boolean found = false;
        TmfEventDependency dep = null;
        /* Search for the event in the companion table */
        for (ITmfTrace mTrace: companionTbl.rowKeySet()) {
            if (companionTbl.containsColumn(eventKey)) {
                found = true;
                ITmfEvent companionEvent = companionTbl.get(mTrace, eventKey);

                /* Remove the element from the companion table */
                companionTbl.remove(mTrace, eventKey);

                /* Create the dependency object */
                switch (evType) {
                case IN:
                    dep = new TmfEventDependency(companionEvent, event);
                    break;
                case OUT:
                    dep = new TmfEventDependency(event, companionEvent);
                    break;
                default:
                    break;

                }
            }
        }

        /*
         * If no companion was found, add the event to the appropriate unMatched
         * lists
         */
        if (found) {
            getProcessingUnit().addMatch(dep);
            for (IMatchMonitor monitor : fMonitors) {
                monitor.cacheHit(dep);
            }
        } else {
            /*
             * If an event is already associated with this key, do not add it
             * again, we keep the first event chronologically, so if its match
             * is eventually found, it is associated with the first send or
             * receive event. At best, it is a good guess, at worst, the match
             * will be too far off to be accurate. Too bad!
             *
             * TODO: maybe instead of just one event, we could have a list of
             * events as value for the unmatched table. Not necessary right now
             * though
             */
            if (!unmatchedTbl.contains(event.getTrace(), eventKey)) {
                unmatchedTbl.put(event.getTrace(), eventKey, event);
                for (IMatchMonitor monitor : fMonitors) {
                    monitor.cacheMiss(eventKey);
                }
            }
        }
        if (event instanceof TmfEvent) {
            ((TmfEvent) event).compress();
        }
        updateMaxCount();
    }

    /**
     * @since 4.0
     */
    public void removeKey(PacketKey key) {
        Collection<? extends ITmfTrace> traces = getTraces();
        for (ITmfTrace mTrace: traces) {
            fUnmatchedIn.remove(mTrace, key);
            fUnmatchedOut.remove(mTrace, key);
        }
    }

    private void updateMaxCount() {
        long currentCount = fUnmatchedIn.size() + fUnmatchedOut.size();
        this.maxUnmatchedCount = Math.max(currentCount, maxUnmatchedCount);
    }

    /**
     * @return the maximum number of unmatched count (these unmached events are
     *         using memory, and is therefore a good measure of the peak memory
     *         usage)
     * @since 4.0
     */
    public long getMaxUnmatchedCount() {
        return maxUnmatchedCount;
    }

    /**
     * @return the total number of matches found at this point
     * @since 4.0
     */
    public long getMatchedCount() {
        return getProcessingUnit().countMatches();
    }

    @Override
    protected void finalizeMatching() {
        super.finalizeMatching();
    }

    /**
     * Prints stats from the matching
     *
     * @return string of statistics
     */
    @SuppressWarnings("nls")
    @Override
    public String toString() {
        final String cr = System.getProperty("line.separator");
        StringBuilder b = new StringBuilder();
        b.append(getProcessingUnit());
        int i = 0;
        for (ITmfTrace trace : getTraces()) {
            b.append("Trace " + i++ + ":" + cr +
                    "  " + fUnmatchedIn.row(trace).size() + " unmatched incoming events" + cr +
                    "  " + fUnmatchedOut.row(trace).size() + " unmatched outgoing events" + cr);
        }

        return b.toString();
    }

    /**
     * @param obj
     * @since 4.0
     */
    public void addMatchMonitor(@NonNull IMatchMonitor obj) {
        fMonitors.add(obj);
        obj.setParent(this);
    }

}
