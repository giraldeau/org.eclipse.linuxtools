/*******************************************************************************
 * Copyright (c) 2013 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation (from ControlFlowView)
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.analysis.graph.ui.criticalpath.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.window.Window;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfEdge;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfEdge.EdgeType;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraphStatistics;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraphVisitor;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfVertex;
import org.eclipse.linuxtools.tmf.analysis.graph.core.criticalpath.AlgorithmManager;
import org.eclipse.linuxtools.tmf.analysis.graph.core.criticalpath.ICriticalPathAlgorithm;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfWorker;
import org.eclipse.linuxtools.tmf.analysis.graph.ui.Activator;
import org.eclipse.linuxtools.tmf.analysis.graph.ui.criticalpath.CriticalPathAlgorithmProvider;
import org.eclipse.linuxtools.tmf.analysis.graph.ui.criticalpath.CriticalPathModule;
import org.eclipse.linuxtools.tmf.analysis.graph.ui.criticalpath.Messages;
import org.eclipse.linuxtools.tmf.analysis.graph.ui.criticalpath.view.CriticalPathPresentationProvider.State;
import org.eclipse.linuxtools.tmf.core.signal.TmfSignalHandler;
import org.eclipse.linuxtools.tmf.core.signal.TmfStartAnalysisSignal;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.ui.views.timegraph.AbstractTimeGraphPerObjectView;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ILinkEvent;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.TimeGraphEntry;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.TimeLinkEvent;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * The Control Flow view main object
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 */
@SuppressWarnings("nls")
public class CriticalPathView extends AbstractTimeGraphPerObjectView {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    private static final double NANOINV = 0.000000001;

    /**
     * View ID.
     */
    public static final String ID = "org.eclipse.linuxtools.tmf.analysis.graph.ui.criticalpath.view.criticalpathview"; //$NON-NLS-1$

    private static final String COLUMN_PROCESS = Messages.CriticalFlowView_columnProcess;

    private static final String COLUMN_ELAPSED = Messages.CriticalFlowView_columnElapsed;

    private static final String COLUMN_PERCENT = Messages.CriticalFlowView_columnPercent;

    private static final String[] COLUMN_NAMES = new String[] {
            COLUMN_PROCESS,
            COLUMN_ELAPSED,
            COLUMN_PERCENT
    };

    private static final String[] FILTER_COLUMN_NAMES = new String[] {
            COLUMN_PROCESS
    };

    private Object fCurrentValue;

    // The redraw synchronization object
    private TmfGraphStatistics fStats = new TmfGraphStatistics();
    private CriticalPathModule fModule;

    private final Map<ITmfTrace, Map<Object, List<ILinkEvent>>> fLinks = new HashMap<>();
    /** The trace to entry list hash map */
    private final Map<ITmfTrace, Map<Object, TmfGraphStatistics>> fObjectStatistics = new HashMap<>();

    private Map<Object, CriticalPathEntry> fRootList;
    private Action fAlgorithmChoice;

    private final String SETTINGS_ALGORITHM_PROPERTY = "AlgorithmID";
    private String fAlgorithm;

    private class CriticalPathTreeLabelProvider extends TreeLabelProvider {

        @Override
        public String getColumnText(Object element, int columnIndex) {
            CriticalPathEntry entry = (CriticalPathEntry) element;
            if (columnIndex == 0) {
                return entry.getName();
            }
            else if (columnIndex == 1) {
                try {
                    return String.format("%.9f", fStats.getSum(entry.getWorker()) * NANOINV); //$NON-NLS-1$
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else if (columnIndex == 2) {
                return String.format("%.2f", fStats.getPercent(entry.getWorker()) * 100); //$NON-NLS-1$
            }
            return ""; //$NON-NLS-1$
        }

    }

    private class CriticalPathEntryComparator implements Comparator<ITimeGraphEntry> {

        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {

            int result = 0;

            if ((o1 instanceof CriticalPathEntry) && (o2 instanceof CriticalPathEntry)) {
                CriticalPathEntry entry1 = (CriticalPathEntry) o1;
                CriticalPathEntry entry2 = (CriticalPathEntry) o2;
                // result = ((TmfWorker)
                // entry1.getWorker()).compareTo((TmfWorker)
                // entry2.getWorker());
                result = -1 * fStats.getSum(entry1.getWorker()).compareTo(fStats.getSum(entry2.getWorker()));
            }
            return result;
        }
    }

    /**
     * Constructor
     */
    public CriticalPathView() {
        super(ID, new CriticalPathPresentationProvider());
        setTreeColumns(COLUMN_NAMES);
        setFilterColumns(FILTER_COLUMN_NAMES);
        setTreeLabelProvider(new CriticalPathTreeLabelProvider());
        setEntryComparator(new CriticalPathEntryComparator());
        loadPersistentData();
    }

    // ------------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------------



    private Map<Object, List<ILinkEvent>> getTraceLinks() {
        if (fLinks.get(getTrace()) == null) {
            fLinks.put(getTrace(), new HashMap<Object, List<ILinkEvent>>());
        }
        return fLinks.get(getTrace());
    }

    @Override
    protected void buildEventList(final ITmfTrace trace, Object object, IProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            return;
        }

        if (fModule == null) {
            return;
        }

        fModule.schedule();
        if (!fModule.waitForCompletion(monitor)) {
            return;
        }
        final TmfGraph graph = fModule.getCriticalPath();

        if (graph == null) {
            return;
        }
        setStartTime(Long.MAX_VALUE);
        setEndTime(Long.MIN_VALUE);

        fRootList = new HashMap<>();
        getTraceLinks().remove(object);

        TmfVertex vertex = graph.getHead();

        /* Calculate statistics */
        fStats = new TmfGraphStatistics();
        fStats.getGraphStatistics(graph, null);
        if (fObjectStatistics.get(getTrace()) == null) {
            fObjectStatistics.put(getTrace(), new HashMap<Object, TmfGraphStatistics>());
        }
        fObjectStatistics.get(getTrace()).put(object, fStats);

        // Hosts entries are parent of each worker entries
        final HashMap<String, CriticalPathEntry> hostEntries = new HashMap<>();

        /* create all interval entries and horizontal links */
        final CriticalPathEntry defaultParent = new CriticalPathEntry("default", trace, getStartTime(), getEndTime(), null);
        graph.scanLineTraverse(vertex, new TmfGraphVisitor() {
            @Override
            public void visitHead(TmfVertex node) {
                /* TODO possible null pointer ? */
                Object owner = graph.getParentOf(node);
                TmfVertex first = graph.getHead(owner);
                TmfVertex last = graph.getHead(owner);
                setStartTime(Math.min(getStartTime(), first.getTs()));
                setEndTime(Math.max(getEndTime(), last.getTs()));
                // create host entry
                CriticalPathEntry parent = defaultParent;
                if (owner instanceof TmfWorker) { // this is ugly, should use label providers
                    TmfWorker worker = ((TmfWorker) owner);
                    String host = worker.getTrace().getHostId(); // FIXME: reference LttngStrings here, but avoid reverse dependence on lttng2.kernel.core
                    if (!hostEntries.containsKey(host)) {
                        parent = new CriticalPathEntry(host, trace, getStartTime(), getEndTime(), null);
                        hostEntries.put(host, parent);
                    }
                    parent = hostEntries.get(host);
                }
                if (fRootList.containsKey(owner)) {
                    return;
                }
                CriticalPathEntry entry = new CriticalPathEntry(owner.toString(), trace, getStartTime(), getEndTime(), owner);
                if (parent != null) {
                    parent.addChild(entry);
                }
                fRootList.put(owner, entry);
            }

            @Override
            public void visit(TmfVertex node) {
                setStartTime(Math.min(getStartTime(), node.getTs()));
                setEndTime(Math.max(getEndTime(), node.getTs()));
            }

            @Override
            public void visit(TmfEdge link, boolean horizontal) {
                if (horizontal) {
                    Object parent = graph.getParentOf(link.getVertexFrom());
                    CriticalPathEntry entry = fRootList.get(parent);
                    TimeEvent ev = new TimeEvent(entry, link.getVertexFrom().getTs(), link.getDuration(),
                            getMatchingState(link.getType()).ordinal());
                    entry.addEvent(ev);
                }
            }
        });

        List<TimeGraphEntry> list = new ArrayList<>();
        //list.addAll(fRootList.values());
        list.addAll(hostEntries.values());
        if (defaultParent.hasChildren()) {
            list.add(defaultParent);
        }
        putObjectEntryList(fCurrentValue, list);

        refresh();
        // Collections.sort(rootList, fCriticalFlowEntryComparator);
        // synchronized (fEntryListMap) {
        // fEntryListMap.put(trace, (ArrayList<CriticalFlowEntry>)
        // rootList.clone());
        // }
        // if (trace == fTrace) {
        // refresh();
        // }
        // }
        for (TimeGraphEntry entry : list) {
            buildStatusEvents(trace, (CriticalPathEntry) entry);
        }
    }

    private static State getMatchingState(EdgeType type) {
        State state = State.UNKNOWN;
        switch (type) {
        case RUNNING:
            state = State.RUNNING;
            break;
        case PREEMPTED:
            state = State.PREEMPTED;
            break;
        case TIMER:
            state = State.TIMER;
            break;
        case BLOCK_DEVICE:
            state = State.BLOCK_DEVICE;
            break;
        case INTERRUPTED:
            state = State.INTERRUPTED;
            break;
        case NETWORK:
            state = State.NETWORK;
            break;
        case USER_INPUT:
            state = State.USER_INPUT;
            break;
        case EPS:
        case UNKNOWN:
        case DEFAULT:
        case BLOCKED:
            break;
        default:
            break;
        }
        return state;
    }

    private void buildStatusEvents(ITmfTrace trace, CriticalPathEntry entry) {

        long start = trace.getStartTime().getValue();
        long end = trace.getEndTime().getValue() + 1;
        long resolution = Math.max(1, (end - start) / getDisplayWidth());
        List<ITimeEvent> eventList = getEventList(entry, entry.getStartTime(), entry.getEndTime(), resolution, null);

        entry.setZoomedEventList(eventList);

        redraw();

        for (ITimeGraphEntry child : entry.getChildren()) {

            buildStatusEvents(trace, (CriticalPathEntry) child);
        }
    }

    @Override
    protected List<ITimeEvent> getEventList(TimeGraphEntry entry,
            long startTime, long endTime, long resolution,
            IProgressMonitor monitor) {

        final long realStart = Math.max(startTime, entry.getStartTime());
        final long realEnd = Math.min(endTime, entry.getEndTime());
        if (realEnd <= realStart) {
            return null;
        }
        List<ITimeEvent> eventList = null;
        try {
            entry.setZoomedEventList(null);
            Iterator<ITimeEvent> iterator = entry.getTimeEventsIterator();
            eventList = new ArrayList<>();

            while (iterator.hasNext()) {

                ITimeEvent event = iterator.next();
                /* is event visible */
                if (((event.getTime() >= realStart) && (event.getTime() <= realEnd)) ||
                        ((event.getTime() + event.getDuration() > realStart) &&
                        (event.getTime() + event.getDuration() < realEnd))) {
                    eventList.add(event);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return eventList;
    }

    @Override
    protected List<ILinkEvent> getLinkList(long startTime, long endTime, long resolution, IProgressMonitor monitor) {

        /*
         * Critical path typically has relatively few links, so we calculate and
         * save them all, but just return those in range
         */
        List<ILinkEvent> links = getTraceLinks().get(fCurrentValue);
        if (fModule == null) {
            return links;
        }
        fModule.schedule();
        if (!fModule.waitForCompletion(monitor)) {
            return links;
        }
        final TmfGraph graph = fModule.getCriticalPath();
        if (links == null) {

            if (graph == null) {
                return links;
            }
            setStartTime(Long.MAX_VALUE);
            setEndTime(Long.MIN_VALUE);

            TmfVertex vertex = graph.getHead();

            final List<ILinkEvent> graphLinks = new ArrayList<>();

            // find vertical links
            graph.scanLineTraverse(vertex, new TmfGraphVisitor() {
                @Override
                public void visitHead(TmfVertex node) {

                }

                @Override
                public void visit(TmfVertex node) {

                }

                @Override
                public void visit(TmfEdge link, boolean horizontal) {
                    if (!horizontal) {
                        Object parentFrom = graph.getParentOf(link.getVertexFrom());
                        Object parentTo = graph.getParentOf(link.getVertexTo());
                        CriticalPathEntry entryFrom = fRootList.get(parentFrom);
                        CriticalPathEntry entryTo = fRootList.get(parentTo);
                        TimeLinkEvent lk = new TimeLinkEvent(entryFrom, entryTo, link.getVertexFrom().getTs(),
                                link.getVertexTo().getTs() - link.getVertexFrom().getTs(), getMatchingState(link.getType()).ordinal());
                        graphLinks.add(lk);
                    }
                }
            });
            fLinks.get(getTrace()).put(fCurrentValue, graphLinks);
            links = graphLinks;

        }
        List<ILinkEvent> linksInRange = new ArrayList<>();
        for (ILinkEvent link : links) {
            if (((link.getTime() >= startTime) && (link.getTime() <= endTime)) ||
                    ((link.getTime() + link.getDuration() >= startTime) && (link.getTime() + link.getDuration() <= endTime))) {
                linksInRange.add(link);
            }
        }
        return linksInRange;
    }

    /**
     * Signal handler for analysis completed
     *
     * @param signal
     *            The signal
     */
    @TmfSignalHandler
    public void analysisStarted(TmfStartAnalysisSignal signal) {
        if (!(signal.getAnalysisModule() instanceof CriticalPathModule)) {
            return;
        }
        fModule = (CriticalPathModule) signal.getAnalysisModule();
        fCurrentValue = fModule.getParameter(CriticalPathModule.PARAM_WORKER);
        loadObject(fCurrentValue);
    }

    @Override
    protected void loadObject(Object obj) {
        if (fObjectStatistics.get(getTrace()) != null) {
            fStats = fObjectStatistics.get(getTrace()).get(obj);
        }
        super.loadObject(obj);
    }

    // ------------------------------------------------------------------------
    // Part For Button Action
    // ------------------------------------------------------------------------

    // FIXME: Copied from XmlStateSystemView, should refactor
    @NonNull
    private IDialogSettings getPersistentPropertyStore() {
        IDialogSettings settings = Activator.getDefault().getDialogSettings();
        IDialogSettings section = settings.getSection(getClass().getName());
        if (section == null) {
            section = settings.addNewSection(getClass().getName());
            if (section == null) {
                throw new IllegalStateException();
            }
        }
        return section;
    }

    private void savePersistentData() {
        IDialogSettings settings = getPersistentPropertyStore();
        settings.put(SETTINGS_ALGORITHM_PROPERTY, fAlgorithm);
    }

    private void loadPersistentData() {
        IDialogSettings settings = getPersistentPropertyStore();
        fAlgorithm = settings.get(SETTINGS_ALGORITHM_PROPERTY);
        if (null == fAlgorithm) {
            String algo = AlgorithmManager.getInstance().registeredTypes().keySet().iterator().next();
            if (algo != null) {
                setAlgorithm(algo);
            }
        }
    }

    private void setAlgorithm(String algorithmType) {
        if (algorithmType == null) {
            return;
        }
        if (fAlgorithm == null || !fAlgorithm.equals(algorithmType)) {
            fAlgorithm = algorithmType;
            Class<? extends ICriticalPathAlgorithm> type = AlgorithmManager.getInstance().registeredTypes().get(algorithmType);
            if (null != type) {
                CriticalPathAlgorithmProvider.getInstance().setAlgorithm(type);
                savePersistentData();
            } else {
                throw new RuntimeException("Class for algorithm not found: " + fAlgorithm);
            }

        }
    }

    @Override
    protected void fillLocalToolBar(IToolBarManager manager) {

        fAlgorithmChoice = new Action() {
            @Override
            public void run() {
                Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
                SettingsDialog dialog = new SettingsDialog(shell);
                dialog.create();
                dialog.setAlgorithmType(fAlgorithm);
                if (dialog.open() == Window.OK) {
                    String name = dialog.getAlgorithmType();
                    if (name != null) {
                        setAlgorithm(name);
                    }
                }
                fAlgorithmChoice.setChecked(false);
            }
        };
        fAlgorithmChoice.setToolTipText("Select the critical path algorithm to use");
        fAlgorithmChoice.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
                getImageDescriptor(ISharedImages.IMG_OBJ_ELEMENT));
        fAlgorithmChoice.setChecked(false);

        manager.add(fAlgorithmChoice);
        super.fillLocalToolBar(manager);
    }

}
