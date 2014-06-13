/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien and Francis Giraldeau - Initial implementation and API
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.analysis.graph.core.model;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * A system model specific for system who track what goes on on Cpus: current
 * tasks running on cpu and cpu swappers. Traces with parallel processing may
 * make good use of this model instead of the default one.
 *
 * @since 3.0
 */
public class TmfSystemModelWithCpu extends TmfSystemModel {

    private final Table<String, Integer, TmfWorker> fSwappers;  // (host, cpu, swapper)
    private final Map<String, Integer> fCpus;
    private final Table<String, Integer, Long> fCurrentTids;    // (host, cpu, tid)
    private ITmfWorkerFactory swapperFactory;

    /**
     * Default constructor
     */
    public TmfSystemModelWithCpu() {
        super();
        fCurrentTids = HashBasedTable.create();
        fSwappers = HashBasedTable.create();
        fCpus = new HashMap<>();
    }

    /**
     * Set the swapper worker factory for the system Swapper objects represents
     * the idle task on a system, one per CPU and they all have id zero. The
     * factory method is responsible to create the swapper worker of the CPU if
     * it does not exists.
     *
     * @param factory
     *            the factory
     */
    public void setSwapperFactory(ITmfWorkerFactory factory) {
        swapperFactory = factory;
    }

    @Override
    public TmfWorker getWorker(String host, int cpu, long wid) {
        if (wid > 0) {
            return super.getWorker(host, cpu, wid);
        }
        if (wid == 0) {
            if (!fSwappers.contains(host, cpu)) {
                /*
                 * null pointer exception occurs if the factory is not set.
                 * Defining a final field and forcing to set a swapper factory
                 * would be annoying. Maybe a default value, but then may fail
                 * silently. Maybe injection? Or having the factory passed as a
                 * parameter instead? Then, would allow to change the factory
                 * depending on the expected worker creation.
                 */
                TmfWorker swapper = swapperFactory.createModelElement(host, cpu, wid);
                fSwappers.put(host, cpu, swapper);
            }
            return fSwappers.get(host, cpu);
        }
        return null;
    }

    /**
     * Gets the worker currently active on a cpu
     *
     * @param host
     *            The host string
     * @param cpu
     *            The cpu number
     * @return The active worker
     */
    public TmfWorker getWorkerCpu(String host, int cpu) {
        long currentTid = getCurrentTid(host, cpu);
        return getWorker(host, cpu, currentTid);
    }

    /**
     * Gets the tid of the process active on a cpu
     *
     * @param host
     *            The host string
     * @param cpu
     *            The cpu number
     * @return The tid
     */
    public long getCurrentTid(String host, int cpu) {
        if (!fCurrentTids.contains(host, cpu)) {
            return 0;
        }
        return fCurrentTids.get(host, cpu);
    }

    /**
     * Sets the current active tid on a cpu
     *
     * @param host
     *            The host string
     * @param cpu
     *            The cpu number
     * @param tid
     *            The tid of the active process
     */
    public void setCurrentTid(String host, int cpu, long tid) {
        fCurrentTids.put(host, cpu, tid);
    }

    /**
     * Gets the current cpu of a trace
     *
     * @param host
     *            The host string
     * @return The active cpu
     */
    public int getCurrentCPU(String host) {
        return fCpus.get(host);
    }

    /**
     * Set the current cpu of the trace
     *
     * @param host
     *            The host string
     * @param cpu
     *            The currently active cpu
     */
    public void setCurrentCPU(String host, int cpu) {
        fCpus.put(host, cpu);
    }

    /**
     * Set the current cpu for a host
     *
     * @param cpu
     *            The currently active cpu
     * @param host
     *            the host
     */
    public void setCurrentCPU(int cpu, String host) {
        fCpus.put(host, cpu);
    }


    /**
     * Get all swappers worker for this host
     * @param host the host name
     *
     * @return swappers
     */
    public Map<Integer, TmfWorker> getSwappers(String host) {
        return fSwappers.row(host);
    }
}
