/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.internal.lttng2.kernel.core.graph.building;

/**
 * List of strings used by the graph provider to build the lttng kernel system
 * model
 *
 * @author Geneviève Bastien
 * @since 4.0
 */
@SuppressWarnings({ "nls", "javadoc" })
public class LttngKernelSystemModelStrings {

    /* Resources and worker names */
    public static final String RESOURCE_HRTIMER = "HR_TIMER";
    public static final String RESOURCE_INET4SOCK = "INET4SOCK";
    public static final String RESOURCE_FD = "FD";
    public static final String RESOURCE_INTCONTEXT = "INTERRUPTCONTEXT";
    public static final String WORKER = "THREAD";

    /* File descriptor fields */
    public static final String FD_NUM = "num";
    public static final String FD_NAME = "name";

    /* Network socket fields */
    public static final String INET4SOCK_SK = "sk";
    public static final String INET4SOCK_SADDR = "saddr";
    public static final String INET4SOCK_DADDR = "daddr";
    public static final String INET4SOCK_SPORT = "sport";
    public static final String INET4SOCK_DPORT = "dport";
    public static final String INET4SOCK_ISSET = "isSet";
    public static final String INET4SOCK_STARTTIME = "startTime";
    public static final String INET4SOCK_ENDTIME = "endTime";

    /* HR timer fields */
    public static final String HRTIMER_ID = "id";
    public static final String HRTIMER_STATE = "state";
    public static final String HRTIMER_HOST_ID = "host";

    /* Interrupt context fields */
    public static final String INTCONTEXT_EVENT = "event";
    public static final String INTCONTEXT_CONTEXT = "context";

}
