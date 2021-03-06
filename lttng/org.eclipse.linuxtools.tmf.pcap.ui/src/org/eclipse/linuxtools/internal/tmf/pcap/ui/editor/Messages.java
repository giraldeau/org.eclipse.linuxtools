/*******************************************************************************
 * Copyright (c) 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Vincent Perot - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.internal.tmf.pcap.ui.editor;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

@SuppressWarnings("javadoc")
public class Messages extends NLS {

    private static final String BUNDLE_NAME = "org.eclipse.linuxtools.internal.tmf.pcap.ui.editor.messages"; //$NON-NLS-1$

    public static @Nullable String PcapEventsTable_Content;
    public static @Nullable String PcapEventsTable_Destination;
    public static @Nullable String PcapEventsTable_Protocol;
    public static @Nullable String PcapEventsTable_Reference;
    public static @Nullable String PcapEventsTable_Source;
    public static @Nullable String PcapEventsTable_Timestamp;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
