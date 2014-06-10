package org.eclipse.linuxtools.tmf.analysis.graph.ui.staging;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.linuxtools.statesystem.core.ITmfStateSystem;
import org.eclipse.linuxtools.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.linuxtools.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.linuxtools.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.ExecGraphModule;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.ExecGraphStateProvider;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.ExecGraphStateProvider.Attributes;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.PackedLongValue;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Task;
import org.eclipse.linuxtools.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.ui.views.timegraph.AbstractTimeGraphPerObjectView;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.TimeGraphEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class ExecGraphView extends AbstractTimeGraphPerObjectView {

    private static final String ID = "org.eclipse.linuxtools.tmf.analysis.graph.ui.staging.ExecGraphView"; //$NON-NLS-1$;

    private static final String WILD_CARD = "*";  //$NON-NLS-1$

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

    Runnable updateTableRunnable = new Runnable() {
        @Override
        public void run() {
            if (fTasks != null) {
                tableViewer.setInput(fTasks);
            }
        }
    };

    protected static class TaskTreeLabelProvider extends TreeLabelProvider {

        static final String[] titles = { "TID", };
        static final int[] bounds = { 10, };

        @Override
        public String getColumnText(Object element, int columnIndex) {
            Task entry = (Task) element;
            String result = ""; //$NON-NLS-1$

            switch (columnIndex) {
            case 0:
                result = entry.getTID().toString();
                break;
            default:
                break;
            }

            return result;
        }

        public void createColumns(TableViewer viewer) {
            for (int i = 0; i < titles.length; i++) {
                TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
                TableColumn column = viewerColumn.getColumn();
                column.setText(titles[i]);
                column.setWidth(bounds[i]);
                column.setResizable(true);
                column.setMoveable(true);
            }
        }

    }

    private TableViewer tableViewer;

    private List<Task> fTasks;

    private Task fCurrentTask;
    //private ExecGraphEntry fCurrentEntry;
    //private ExecGraphModule fModule;

    private ITmfStateSystem fStateSystem;

    public ExecGraphView() {
        super(ID, new ExecGraphPresentationProvider());
        setTreeColumns(COLUMN_NAMES);
        setTreeLabelProvider(new ExecGraphTreeLabelProvider());
    }

    @Override
    public void createPartControl(Composite parent) {
        SashForm sash = new SashForm(parent, SWT.HORIZONTAL);
        setWeight(new int[] { 3, 7 });
        super.createPartControl(sash);

        Composite right = new Composite(sash, SWT.BORDER);
        right.setLayout(new FillLayout());

        // search box here
        createTableViewer(right);
        sash.setWeights(new int[] { 10, 3 });
    }

    private void createTableViewer(Composite parent) {
        tableViewer = new TableViewer(parent, SWT.SINGLE
                | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
        Table table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        tableViewer.setContentProvider(new ArrayContentProvider());
        getSite().setSelectionProvider(tableViewer);
        TaskTreeLabelProvider provider = new TaskTreeLabelProvider();
        provider.createColumns(tableViewer);
        tableViewer.setInput(new Task[] {});
        tableViewer.setLabelProvider(provider);
        tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection sel = (IStructuredSelection) event.getSelection();
                Task model = (Task) sel.getFirstElement();
                setCurrentTask(model);
            }
        });
    }

    protected void setCurrentTask(Task task) {
        fCurrentTask = task;
        updateTimeControl();
    }

    @Override
    protected void buildEventList(ITmfTrace trace, Object object, IProgressMonitor monitor) {
        TmfStateSystemAnalysisModule module = trace.getAnalysisModuleOfClass(ExecGraphModule.class, ExecGraphModule.ID);
        if (module == null || monitor.isCanceled()) {
            return;
        }
        System.out.println("module.schedule()");
        module.schedule();
        if (!module.waitForCompletion(new NullProgressMonitor())) {
            return;
        }
        fStateSystem = module.getStateSystem();
        if (fStateSystem == null) {
            return;
        }
        updateTableView();
    }

    protected void updateTimeControl() {
        if (fStateSystem == null || fCurrentTask == null) {
            return;
        }
        if (fStateSystem.isCancelled()) {
            return;
        }
        long start = fStateSystem.getStartTime();
        long end = fStateSystem.getCurrentEndTime();
        setStartTime(Math.min(getStartTime(), start));
        setEndTime(Math.max(getEndTime(), end));

        List<TimeGraphEntry> entryList = new ArrayList<>();
        try {
            makeEntryList(entryList, start, end);
        } catch (AttributeNotFoundException | StateSystemDisposedException e) {
            e.printStackTrace();
        }

        putObjectEntryList("root", entryList);
        loadObject("root");
        redraw();
        refresh();
    }

    private void updateTableView() {
        if (fStateSystem == null) {
            return;
        }
        fTasks = new ArrayList<>();
        List<Integer> rootQuarks = fStateSystem.getQuarks(WILD_CARD);
        for (Integer hostQuark: rootQuarks) {
            String host = fStateSystem.getAttributeName(hostQuark);
            List<Integer> taskQuarks = fStateSystem.getQuarks(host, ExecGraphStateProvider.LABEL_TASK , WILD_CARD);
            for (Integer taskQuark: taskQuarks) {
                String taskTid = fStateSystem.getAttributeName(taskQuark);
                fTasks.add(new Task(host, Long.parseLong(taskTid), 0));
            }
        }
        Display.getDefault().asyncExec(updateTableRunnable);
    }

    private void makeEntryList(List<TimeGraphEntry> entryList, long start, long end) throws AttributeNotFoundException, StateSystemDisposedException {
        /*
         * /host/cpu/id/state
         * /host/task/id/state
         */
        TimeGraphEntry root = new ExecGraphEntry(fCurrentTask.getHostID(), getTrace(), start, end, -1);
        int q = fStateSystem.getQuarkAbsolute(fCurrentTask.getHostID(), ExecGraphStateProvider.LABEL_TASK, fCurrentTask.getTID().toString(), Attributes.STATE.label());
        ExecGraphEntry child1 = new ExecGraphEntry(fCurrentTask.getTID().toString(), getTrace(), start, end, q);
        root.addChild(child1);
        entryList.add(root);
    }

    @Override
    protected List<ITimeEvent> getEventList(TimeGraphEntry entry,
            long startTime, long endTime, long resolution,
            IProgressMonitor monitor) {

        ArrayList<ITimeEvent> items = new ArrayList<>();
        ExecGraphEntry exGraphEntry = (ExecGraphEntry) entry;
        int q = exGraphEntry.getQuark();
        if (q == -1) {
            return items;
        }
        try {
            List<ITmfStateInterval> range = fStateSystem.queryHistoryRange(q, startTime, endTime, resolution, monitor);
            for (ITmfStateInterval i: range) {
                long v = i.getStateValue().unboxLong();
                int stateVal = PackedLongValue.unpack(0, v);
                items.add(new TimeEvent(entry, i.getStartTime(), i.getEndTime(), stateVal));
            }
        } catch (AttributeNotFoundException | StateSystemDisposedException e) {
            e.printStackTrace();
        }
        return items;
    }

}
