/*******************************************************************************
 * Copyright (c) 2009, 2014 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francois Chouinard - Initial API and implementation
 *   Geneviève Bastien - Moved the add and remove code to the experiment class
 *   Patrick Tasse - Add support for folder elements
 *   Marc-Andre Laperle - Convert to tree structure and add select/deselect all
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.ui.project.wizards;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.linuxtools.internal.tmf.ui.Activator;
import org.eclipse.linuxtools.tmf.ui.project.model.ITmfProjectModelElement;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfExperimentElement;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfNavigatorContentProvider;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfNavigatorLabelProvider;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfProjectElement;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceElement;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceFolder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

/**
 * Implementation of a wizard page for selecting trace for an experiment.
 * <p>
 *
 * @version 1.0
 * @author Francois Chouinard
 */
public class SelectTracesWizardPage extends WizardPage {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private final TmfProjectElement fProject;
    private final TmfExperimentElement fExperiment;
    private Map<String, TmfTraceElement> fPreviousTraces;
    private CheckboxTreeViewer fCheckboxTreeViewer;
    private TmfNavigatorContentProvider fContentProvider;
    private TmfNavigatorLabelProvider fLabelProvider;

    private static final int COLUMN_WIDTH = 200;
    private static final int BUTTON_SPACING = 4;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------
    /**
     * Constructor
     * @param project The project model element.
     * @param experiment The experiment model experiment.
     */
    protected SelectTracesWizardPage(TmfProjectElement project, TmfExperimentElement experiment) {
        super(""); //$NON-NLS-1$
        setTitle(Messages.SelectTracesWizardPage_WindowTitle);
        setDescription(Messages.SelectTracesWizardPage_Description);
        fProject = project;
        fExperiment = experiment;
    }

    // ------------------------------------------------------------------------
    // Dialog
    // ------------------------------------------------------------------------

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        container.setLayout(new GridLayout(2, false));
        setControl(container);

        new FilteredTree(container, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER, new PatternFilter(), true) {
            @Override
            protected TreeViewer doCreateTreeViewer(Composite aparent, int style) {
                return SelectTracesWizardPage.this.doCreateTreeViewer(aparent);
            }
        };

        Composite buttonComposite = new Composite(container, SWT.NONE);
        FillLayout layout = new FillLayout(SWT.VERTICAL);
        layout.spacing = BUTTON_SPACING;
        buttonComposite.setLayout(layout);
        GridData gd = new GridData();
        gd.verticalAlignment = SWT.CENTER;
        buttonComposite.setLayoutData(gd);

        Button selectAllButton = new Button(buttonComposite, SWT.PUSH);
        selectAllButton.setText(org.eclipse.linuxtools.internal.tmf.ui.project.dialogs.Messages.Dialog_SelectAll);
        selectAllButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setAllChecked(true);
            }
        });

        Button deselectAllButton = new Button(buttonComposite, SWT.PUSH);
        deselectAllButton.setText(org.eclipse.linuxtools.internal.tmf.ui.project.dialogs.Messages.Dialog_DeselectAll);
        deselectAllButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setAllChecked(false);
            }
        });
    }

    private TreeViewer doCreateTreeViewer(Composite parent) {
        fCheckboxTreeViewer = new CheckboxTreeViewer(parent, SWT.BORDER);
        fContentProvider = new TmfNavigatorContentProvider() {

            @Override
            public Object[] getElements(Object inputElement) {
                return super.getChildren(inputElement);
            }

            @Override
            public synchronized Object[] getChildren(Object parentElement) {
                // We only care about the content of trace folders
                if (parentElement instanceof TmfTraceFolder) {
                    return super.getChildren(parentElement);
                }
                return null;
            }

            @Override
            public boolean hasChildren(Object element) {
                return getChildren(element) != null;
            }
        };
        fCheckboxTreeViewer.setContentProvider(fContentProvider);
        fLabelProvider = new TmfNavigatorLabelProvider();
        fCheckboxTreeViewer.setLabelProvider(fLabelProvider);
        fCheckboxTreeViewer.setSorter(new ViewerSorter());

        final Tree tree = fCheckboxTreeViewer.getTree();
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        tree.setLayoutData(gd);
        tree.setHeaderVisible(true);

        final TreeViewerColumn column = new TreeViewerColumn(fCheckboxTreeViewer, SWT.NONE);
        column.getColumn().setWidth(COLUMN_WIDTH);
        column.getColumn().setText(Messages.SelectTracesWizardPage_TraceColumnHeader);
        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return fLabelProvider.getText(element);
            }
            @Override
            public Image getImage(Object element) {
                return fLabelProvider.getImage(element);
            }
        });

        // Get the list of traces already part of the experiment
        fPreviousTraces = new HashMap<>();
        for (ITmfProjectModelElement child : fExperiment.getChildren()) {
            if (child instanceof TmfTraceElement) {
                TmfTraceElement trace = (TmfTraceElement) child;
                String name = trace.getElementPath();
                fPreviousTraces.put(name, trace);
            }
        }

        // Populate the list of traces to choose from
        Set<String> keys = fPreviousTraces.keySet();
        TmfTraceFolder traceFolder = fProject.getTracesFolder();
        fCheckboxTreeViewer.setInput(traceFolder);

        // Set the checkbox for the traces already included
        setCheckedAlreadyIncludedTraces(keys, fContentProvider.getElements(fCheckboxTreeViewer.getInput()));

        fCheckboxTreeViewer.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                Object element = event.getElement();
                setSubtreeChecked(element, event.getChecked());
                maintainCheckIntegrity(element);
            }
        });

        return fCheckboxTreeViewer;
    }

    private void maintainCheckIntegrity(final Object element) {
        Object parentElement = fContentProvider.getParent(element);
        boolean allChecked = true;
        boolean oneChecked = false;
        boolean oneGrayed = false;
        if (parentElement != null) {
            if (fContentProvider.getChildren(parentElement) != null) {
                for (Object child : fContentProvider.getChildren(parentElement)) {
                    boolean checked = fCheckboxTreeViewer.getChecked(child);
                    oneChecked |= checked;
                    allChecked &= checked;
                    oneGrayed |= fCheckboxTreeViewer.getGrayed(child);
                }
            }
            if (oneGrayed || oneChecked && !allChecked) {
                fCheckboxTreeViewer.setGrayChecked(parentElement, true);
            } else {
                fCheckboxTreeViewer.setGrayed(parentElement, false);
                fCheckboxTreeViewer.setChecked(parentElement, allChecked);
            }
            maintainCheckIntegrity(parentElement);
        }
    }

    private void setCheckedAlreadyIncludedTraces(Set<String> alreadyIncludedTraces, Object[] elements) {
        for (Object element : elements) {
            if (element instanceof TmfTraceElement) {
                TmfTraceElement trace = (TmfTraceElement) element;
                String elementPath = trace.getElementPath();
                if (alreadyIncludedTraces.contains(elementPath)) {
                    fCheckboxTreeViewer.setChecked(element, true);
                    maintainCheckIntegrity(element);
                }
            }
            Object[] children = fContentProvider.getChildren(element);
            if (children != null) {
                setCheckedAlreadyIncludedTraces(alreadyIncludedTraces, children);
            }
        }
    }

    /**
     * Sets all items in the element viewer to be checked or unchecked
     *
     * @param checked
     *            whether or not items should be checked
     */
    private void setAllChecked(boolean checked) {
        for (Object element : fContentProvider.getChildren(fCheckboxTreeViewer.getInput())) {
            setSubtreeChecked(element, checked);
        }
    }

    /**
     * A version of setSubtreeChecked that also handles the grayed state
     *
     * @param element
     *            the element
     * @param checked
     *            true if the item should be checked, and false if it should be
     *            unchecked
     */
    private void setSubtreeChecked(Object element, boolean checked) {
        fCheckboxTreeViewer.setChecked(element, checked);
        if (checked) {
            fCheckboxTreeViewer.setGrayed(element, false);
        }
        Object[] children = fContentProvider.getChildren(element);
        if (children != null) {
            for (Object child : children) {
                setSubtreeChecked(child, checked);
            }
        }
    }

    /**
     * Method to finalize the select operation.
     * @return <code>true</code> if successful else <code>false</code>
     */
    public boolean performFinish() {

        IFolder experiment = fExperiment.getResource();
        boolean changed = false;

        // Add the selected traces to the experiment
        Set<String> keys = fPreviousTraces.keySet();
        TmfTraceElement[] traces = getSelection();
        for (TmfTraceElement trace : traces) {
            String name = trace.getElementPath();
            if (keys.contains(name)) {
                fPreviousTraces.remove(name);
            } else {
                fExperiment.addTrace(trace);
                changed = true;
            }
        }

        // Remove traces that were unchecked (thus left in fPreviousTraces)
        keys = fPreviousTraces.keySet();
        for (String key : keys) {
            try {
                fExperiment.removeTrace(fPreviousTraces.get(key));
            } catch (CoreException e) {
                Activator.getDefault().logError("Error selecting traces for experiment " + experiment.getName(), e); //$NON-NLS-1$
            }
            changed = true;
        }
        if (changed) {
            fExperiment.closeEditors();
            fExperiment.deleteSupplementaryResources();
        }

        return true;
    }

    /**
     * Get the list of selected traces
     */
    private TmfTraceElement[] getSelection() {
        Vector<TmfTraceElement> traces = new Vector<>();
        Object[] selection = fCheckboxTreeViewer.getCheckedElements();
        for (Object sel : selection) {
            if (sel instanceof TmfTraceElement) {
                traces.add((TmfTraceElement) sel);
            }
        }
        TmfTraceElement[] result = new TmfTraceElement[traces.size()];
        traces.toArray(result);
        return result;
    }

}
