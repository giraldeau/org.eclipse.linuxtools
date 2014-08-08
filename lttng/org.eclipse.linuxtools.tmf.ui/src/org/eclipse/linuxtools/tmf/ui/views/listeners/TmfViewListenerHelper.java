/*******************************************************************************
 * Copyright (c) 2014 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.ui.views.listeners;

import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.PlatformUI;

/**
 * Utility class providing shortcut methods to add listeners to specific views
 *
 * @author - Geneviève Bastien
 * @since 4.0
 */
public final class TmfViewListenerHelper {

    private TmfViewListenerHelper() {

    }

    /**
     * Adds a post selection listener to the requested view. When the view is
     * set to active, the selection listener will automatically be added to the
     * view.
     *
     * This method uses the active workbench window's selection service to add
     * the listener.
     *
     * @param viewId
     *            The ID of the view to which to add this selection listener
     * @param listener
     *            The {@link ISelectionListener} object
     */
    public static void addPostSelectionListener(String viewId, ISelectionListener listener) {
        Object obj = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getService(ISelectionService.class);
        if (obj instanceof ISelectionService) {
            ((ISelectionService) obj).addPostSelectionListener(viewId, listener);
        }
    }

    /**
     * Removes a post selection listener from the requested view. When the view
     * is set to active, the selection listener will automatically be added to
     * the view.
     *
     * This method uses the active workbench window's selection service to add
     * the listener.
     *
     * @param viewId
     *            The ID of the view to which to add this selection listener
     * @param listener
     *            The {@link ISelectionListener} object
     */
    public static void removePostSelectionListener(String viewId, ISelectionListener listener) {
        Object obj = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getService(ISelectionService.class);
        if (obj instanceof ISelectionService) {
            ((ISelectionService) obj).removePostSelectionListener(viewId, listener);
        }
    }

    /**
     * Adds a part listener to the workbench
     *
     * This method uses the active workbench window's part service to add the
     * listener
     *
     * @param listener
     *            The {@link IPartListener2} object
     */
    public static void addPartListener(IPartListener2 listener) {
        Object obj = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getService(IPartService.class);
        if (obj instanceof IPartService) {
            ((IPartService) obj).addPartListener(listener);
        }
    }

    /**
     * Removes a part listener from the workbench
     *
     * This method uses the active workbench window's part service to add the
     * listener
     *
     * @param listener
     *            The {@link IPartListener2} object
     */
    public static void removePartListener(IPartListener2 listener) {
        Object obj = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getService(IPartService.class);
        if (obj instanceof IPartService) {
            ((IPartService) obj).removePartListener(listener);
        }
    }

}
