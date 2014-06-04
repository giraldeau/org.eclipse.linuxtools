package org.eclipse.linuxtools.tmf.analysis.graph.ui.staging;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.linuxtools.statesystem.core.ITmfStateSystem;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.ExecGraphModule;
import org.eclipse.linuxtools.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.ui.views.timegraph.AbstractTimeGraphPerObjectView;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

public class ExecGraphView extends AbstractTimeGraphPerObjectView {

    private static final String ID = "org.eclipse.linuxtools.tmf.analysis.graph.ui.staging.ExecGraphView"; //$NON-NLS-1$;

    private static final String[] COLUMN_NAMES = new String[] { "Task", //$NON-NLS-1$
    };

    /**
     * @author gbastien
     *
     */
    protected static class ExecGraphTreeLabelProvider extends TreeLabelProvider {

        @Override
        public String getColumnText(Object element, int columnIndex) {
            ExecGraphEntry entry = (ExecGraphEntry) element;
            String result = ""; //$NON-NLS-1$

            switch (columnIndex) {
            case 0:
                result = entry.getName();
                break;
            default:
                break;
            }

            return result;
        }
    }

    //private ExecGraphEntry fCurrentEntry;
    //private ExecGraphModule fModule;

    public ExecGraphView() {
        super(ID, new ExecGraphPresentationProvider());
        setTreeColumns(COLUMN_NAMES);
        setTreeLabelProvider(new ExecGraphTreeLabelProvider());
    }

    @Override
    protected void buildEventList(ITmfTrace trace, Object object, IProgressMonitor monitor) {
        TmfStateSystemAnalysisModule module = trace.getAnalysisModuleOfClass(ExecGraphModule.class, ExecGraphModule.ID);
        if (module == null || monitor.isCanceled()) {
            return;
        }
        module.schedule();
        if (!module.waitForCompletion(new NullProgressMonitor())) {
            return;
        }
        ITmfStateSystem ssq = module.getStateSystem();
        if (ssq == null) {
            return;
        }

        long start = ssq.getStartTime();
        long end = ssq.getCurrentEndTime();
        setStartTime(Math.min(getStartTime(), start));
        setEndTime(Math.max(getEndTime(), end));

        List<TimeGraphEntry> entryList = makeEntryList(ssq, start, end);

        putObjectEntryList("root", entryList);
        loadObject("root");
        redraw();
        refresh();
    }

    private List<TimeGraphEntry> makeEntryList(ITmfStateSystem ssq, long start, long end) {
        // /host/tid/{state,blocking}
        ArrayList<TimeGraphEntry> entries = new ArrayList<>();
        List<Integer> rootQuarks = ssq.getQuarks("*");
        for (Integer hostQuark: rootQuarks) {
            String hostName = ssq.getAttributeName(hostQuark);
            TimeGraphEntry entry = new ExecGraphEntry(hostName, getTrace(), start, end, "none");
            entries.add(entry);
        }
        return entries;
    }

    @Override
    protected List<ITimeEvent> getEventList(TimeGraphEntry entry,
            long startTime, long endTime, long resolution,
            IProgressMonitor monitor) {
        long off = entry.getStartTime();
        ArrayList<ITimeEvent> items = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            items.add(new TimeEvent(entry, off + i * 100, 100, i % 7));
        }
        return items;
    }

}
