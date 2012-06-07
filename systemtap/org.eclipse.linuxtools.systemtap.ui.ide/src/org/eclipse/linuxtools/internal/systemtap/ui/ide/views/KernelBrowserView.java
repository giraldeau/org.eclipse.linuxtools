/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Jeff Briggs, Henry Hughes, Ryan Morse
 *******************************************************************************/

package org.eclipse.linuxtools.internal.systemtap.ui.ide.views;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.linuxtools.internal.systemtap.ui.ide.IDEPlugin;
import org.eclipse.linuxtools.internal.systemtap.ui.ide.Localization;
import org.eclipse.linuxtools.internal.systemtap.ui.ide.actions.hidden.KernelSourceAction;
import org.eclipse.linuxtools.internal.systemtap.ui.ide.preferences.IDEPreferenceConstants;
import org.eclipse.linuxtools.internal.systemtap.ui.ide.preferences.PathPreferencePage;
import org.eclipse.linuxtools.systemtap.ui.consolelog.internal.ConsoleLogPlugin;
import org.eclipse.linuxtools.systemtap.ui.consolelog.preferences.ConsoleLogPreferenceConstants;
import org.eclipse.linuxtools.systemtap.ui.logging.LogManager;
import org.eclipse.linuxtools.systemtap.ui.structures.KernelSourceTree;
import org.eclipse.linuxtools.systemtap.ui.structures.TreeNode;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.progress.UIJob;

/**
 * The Kernel Source Browser module for the SystemTap GUI. This browser provides a list of kernel source
 * files and allows the user to open those files in an editor in order to place probes in arbitary locations.
 * @author Henry Hughes
 * @author Ryan Morse
 */

@SuppressWarnings("deprecation")
public class KernelBrowserView extends BrowserView {
	private class KernelRefreshJob extends UIJob {
		public KernelRefreshJob() {
			super(Localization.getString("KernelBrowserView.RefreshingKernelSource")); //$NON-NLS-1$
		}

		public IStatus runInUIThread(IProgressMonitor monitor) {
			IPreferenceStore p = IDEPlugin.getDefault().getPreferenceStore();
			String kernelSource = p.getString(IDEPreferenceConstants.P_KERNEL_SOURCE);
			String localOrRemote = p.getString(IDEPreferenceConstants.P_REMOTE_LOCAL_KERNEL_SOURCE);
			if(null == kernelSource || kernelSource.length() < 1) {
				LogManager.logInfo("Kernel Source Directory not found", this); //$NON-NLS-1$
				TreeNode t = new TreeNode("", "", false); //$NON-NLS-1$ //$NON-NLS-2$
				t.add(new TreeNode("", Localization.getString("KernelBrowserView.NoKernelSourceFound"), false)); //$NON-NLS-1$ //$NON-NLS-2$
				viewer.setInput(t);
				return Status.OK_STATUS;
			}

			monitor.beginTask(Localization.getString("KernelBrowserView.ReadingKernelSourceTree"), 100); //$NON-NLS-1$
			KernelSourceTree kst = new KernelSourceTree();
			String excluded[] = p.getString(IDEPreferenceConstants.P_EXCLUDED_KERNEL_SOURCE).split(File.pathSeparator);
			if (localOrRemote.equals(PathPreferencePage.REMOTE))
				kernelSource = createUri(kernelSource);
			kst.buildKernelTree(kernelSource, excluded);
			viewer.setInput(kst.getTree());
			kst.dispose();
			monitor.done();
			return Status.OK_STATUS;
		}
	}

	public static final String ID = "org.eclipse.linuxtools.internal.systemtap.ui.ide.views.KernelBrowserView"; //$NON-NLS-1$
	private KernelRefreshJob refreshJob = new KernelRefreshJob();
	private KernelSourceAction doubleClickAction;
	private IDoubleClickListener dblClickListener;

	public KernelBrowserView() {
		super();
		refreshJob.setUser(true);
		refreshJob.setPriority(Job.SHORT);
		LogManager.logInfo("Initializing", this); //$NON-NLS-1$
	}

	/**
	 * Creates the UI on the given <code>Composite</code>
	 */
	public void createPartControl(Composite parent) {
		LogManager.logDebug("Start createPartControl: parent-" + parent, this); //$NON-NLS-1$
		super.createPartControl(parent);

		refresh();
		makeActions();
		LogManager.logDebug("End createPartControl", this); //$NON-NLS-1$
	}

	/**
	 * Wires up all of the actions for this browser, such as double and right click handlers.
	 */
	public void makeActions() {
		LogManager.logDebug("Start makeActions:", this); //$NON-NLS-1$
		doubleClickAction = new KernelSourceAction(getSite().getWorkbenchWindow(), this);
		dblClickListener = new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				LogManager.logDebug("Start doubleClick: event-" + event, this); //$NON-NLS-1$
				doubleClickAction.run();
				LogManager.logDebug("End doubleClick:", this); //$NON-NLS-1$
			}
		};
		viewer.addDoubleClickListener(dblClickListener);
		IDEPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(propertyChangeListener);
		LogManager.logDebug("End makeActions:", this); //$NON-NLS-1$
	}

	/**
	 * Updates the kernel source displayed to the user with the new kernel source tree. Usually
	 * a response to the user changing the preferences related to the kernel source location, requiring
	 * that the application update the kernel source information.
	 */
	public void refresh() {
		LogManager.logDebug("Start refresh:", this); //$NON-NLS-1$
		
		refreshJob.schedule();
		LogManager.logDebug("End refresh:", this); //$NON-NLS-1$
	}
	
	/**
	 * A <code>IPropertyChangeListener</code> that detects changes to the Kernel Source location
	 * and runs the <code>updateKernelSourceTree</code> method.
	 */
	private final IPropertyChangeListener propertyChangeListener = new IPropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent event) {
			LogManager.logDebug("Start propertyChange: event-" + event, this); //$NON-NLS-1$
			if(event.getProperty().equals(IDEPreferenceConstants.P_KERNEL_SOURCE)) {
				refresh();
			}
			LogManager.logDebug("End propertyChange:", this); //$NON-NLS-1$
		}
	};
	
	public void dispose() {
		LogManager.logInfo("Disposing", this); //$NON-NLS-1$
		super.dispose();
		IDEPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(propertyChangeListener);
		if(null != viewer)
			viewer.removeDoubleClickListener(dblClickListener);
		dblClickListener = null;
		if(null != doubleClickAction)
			doubleClickAction.dispose();
		doubleClickAction = null;
	}

	private String createUri(String path) {
		Preferences p = ConsoleLogPlugin.getDefault().getPluginPreferences();
		String user = p.getString(ConsoleLogPreferenceConstants.SCP_USER);
		String host = p.getString(ConsoleLogPreferenceConstants.HOST_NAME);
		try {
			URI uri = new URI("ssh", user, host, -1, path, null, null); //$NON-NLS-1$
			return uri.toString();
		} catch (URISyntaxException uri) {
			//fallback
			return "ssh://" + user + "@" + host + "/" + path; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}
}
