package org.eclipse.linuxtools.tmf.analysis.graph.ui.criticalpath.view;

import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.linuxtools.tmf.analysis.graph.core.criticalpath.AlgorithmManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Set critical path parameters
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
@SuppressWarnings("nls")
public class SettingsDialog extends TitleAreaDialog {

    private Combo fAlgorithmCombo;

    private String fAlgorithmType;

    /**
     * Settings dialog
     *
     * @param parentShell
     *            the parent shell
     */
    public SettingsDialog(Shell parentShell) {
        super(parentShell);
    }

    @Override
    public void create() {
        super.create();
        setTitle("View settings");
        setMessage("Execution path settings", IMessageProvider.INFORMATION);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(GridData.FILL_BOTH));
        GridLayout layout = new GridLayout(2, false);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        container.setLayout(layout);

        createAlgorithmCombo(container);

        return area;
    }

    private void createAlgorithmCombo(Composite container) {
        Label lbtFirstName = new Label(container, SWT.NONE);
        lbtFirstName.setText("Algorithm");

        GridData dataFirstName = new GridData();
        dataFirstName.grabExcessHorizontalSpace = true;
        dataFirstName.horizontalAlignment = GridData.FILL;

        fAlgorithmCombo = new Combo(container, SWT.BORDER | SWT.READ_ONLY);
        fAlgorithmCombo.setLayoutData(dataFirstName);
        AlgorithmManager algo = AlgorithmManager.getInstance();
        ArrayList<String> names = new ArrayList<>(algo.registeredTypes().keySet());
        Collections.sort(names);
        for (String name : names) {
            fAlgorithmCombo.add(name);
        }
        // set default
        if (null != fAlgorithmType) {
            fAlgorithmCombo.setText(fAlgorithmType);
        } else {
            fAlgorithmCombo.setText(names.get(0));
        }
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    // save content of the Text fields because they get disposed
    // as soon as the Dialog closes
    private void saveInput() {
        fAlgorithmType = fAlgorithmCombo.getText();

    }

    @Override
    protected void okPressed() {
        saveInput();
        super.okPressed();
    }

    /**
     * Return the name of the algorithm
     * @return the algorithm name
     */
    public String getAlgorithmType() {
        return fAlgorithmType;
    }

    /**
     * Set the current selected algorithm
     * @param name the name
     */
    public void setAlgorithmType(String name) {
        fAlgorithmType = name;
        if (fAlgorithmCombo != null) {
            fAlgorithmCombo.setText(fAlgorithmType);
        }
    }

}
