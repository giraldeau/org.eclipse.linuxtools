package org.eclipse.linuxtools.tmf.analysis.graph.ui.staging;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Task;
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
        ArrayList<TimeGraphEntry> entryList = new ArrayList<>();
        long off = trace.getStartTime().getValue();
        setStartTime(off);
        setEndTime(off + 1000000);
/*
        if (monitor.isCanceled()) {
            return;
        }

        TmfStateSystemAnalysisModule module = trace.getAnalysisModuleOfClass(ExecGraphModule.class, ExecGraphModule.ID);
        module.schedule();
        if (!module.waitForCompletion(new NullProgressMonitor())) {
            return;
        }
        ITmfStateSystem ssq = module.getStateSystem();
        ssq.waitUntilBuilt();
*/
        for (int i = 0; i < 10; i++) {
            ExecGraphEntry entry = new ExecGraphEntry("entry " + i, trace, off, off + 10 * 100 * 100, new Task("host1", i, 100)); //$NON-NLS-1$ //$NON-NLS-2$
            getEventList(entry, off, off + 1000000, 0, null);
            entryList.add(entry);
        }
        putObjectEntryList("root", entryList);
        loadObject("root");
        redraw();
        refresh();
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
