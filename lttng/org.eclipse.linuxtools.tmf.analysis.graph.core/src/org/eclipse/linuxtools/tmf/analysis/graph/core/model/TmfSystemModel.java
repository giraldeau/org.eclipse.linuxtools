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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.eclipse.linuxtools.tmf.analysis.graph.core.building.AbstractTmfGraphProvider;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

/**
 * A generic system model with worker and resources. Workers can have resources
 * and resources can also be stacked for some object
 *
 * @since 3.0
 */
public class TmfSystemModel implements ITmfTraceModel {

    private final Map<String, TmfWorkerDeclaration> fWorkerTypes;
    private final Table<String, Long, TmfWorker> fWorkers;
    //private final HashMap<String, TmfWorker> fWorkers; // (worker unique id,
                                                       // task)

    private final Map<String, TmfModelResourceDeclaration> fResourceTypes;
    private final Multimap<TmfModelElementDeclaration, TmfModelResource> fResources;
    private final Map<TmfWorker, Multimap<TmfModelElementDeclaration, TmfModelResource>> fWorkerResources;
    private final Table<String, Object, Stack<TmfModelResource>> fContextStackTable;
    private TmfModelResource fDefaultInterruptContext;

    private boolean isInitialized = false;

    /**
     * Default constructor
     */
    public TmfSystemModel() {
        fResourceTypes = new HashMap<>();
        fWorkerTypes = new HashMap<>();
        fWorkerResources = new HashMap<>();
        fResources = HashMultimap.create();
        fContextStackTable = HashBasedTable.create();
        fWorkers = HashBasedTable.create();
    }

    @Override
    public void init(AbstractTmfGraphProvider reader) {
        AbstractTmfGraphProvider builder = reader;
        if (isInitialized == false) {
            builder.initializeModel(this);
            isInitialized = true;
        }
    }

    @Override
    public Collection<TmfWorker> getWorkers() {
        return fWorkers.values();
    }

    @Override
    public TmfWorker getWorker(String host, int cpu, long wid) {
        // FIXME: worker unique ID is the tuple (host, wid, birth)
        return fWorkers.get(host, wid);
    }

    @Override
    public void putWorker(TmfWorker worker) {
        fWorkers.put(worker.getHostId(), worker.getId(), worker);
        Multimap<TmfModelElementDeclaration, TmfModelResource> put = HashMultimap.create();
        fWorkerResources.put(worker, put);
    }

    @Override
    public void addWorkerResource(TmfWorker task, TmfModelResource res) {
        fResources.put(res.getDeclaration(), res);
        if (task == null) {
            return;
        }
        Multimap<TmfModelElementDeclaration, TmfModelResource> taskmap = fWorkerResources.get(task);
        if (taskmap == null) {
            putWorker(task);
            taskmap = fWorkerResources.get(task);
        }
        taskmap.put(res.getDeclaration(), res);
    }

    @Override
    public void addWorkerDeclaration(TmfWorkerDeclaration declaration) {
        fWorkerTypes.put(declaration.getName(), declaration);
    }

    @Override
    public TmfWorkerDeclaration getWorkerDeclaration(String worker) {
        return fWorkerTypes.get(worker);
    }

    @Override
    public void addResourceDeclaration(TmfModelResourceDeclaration declaration) {
        fResourceTypes.put(declaration.getName(), declaration);
    }

    @Override
    public TmfModelResourceDeclaration getResourceDeclaration(String resource) {
        return fResourceTypes.get(resource);
    }

    @Override
    public void addResource(TmfModelResource resource) {
        addWorkerResource(null, resource);
    }

    @Override
    public Collection<TmfModelResource> getResources(String name) {
        for (TmfModelElementDeclaration decl : fResources.keySet()) {
            if (decl.getName().equals(name)) {
                return fResources.get(decl);
            }
        }
        return new ArrayList<>();
    }

    @Override
    public Collection<TmfModelResource> getResources(TmfModelElementDeclaration decl) {
        return fResources.get(decl);
    }

    @Override
    public Collection<TmfModelResource> getWorkerResources(TmfWorker worker, TmfModelElementDeclaration decl) {
        if (fWorkerResources.containsKey(worker)) {
            return fWorkerResources.get(worker).get(decl);
        }
        return new ArrayList<>();
    }

    @Override
    public TmfWorker getResourceOwner(TmfModelResource resource) {
        for (TmfWorker worker : fWorkerResources.keySet()) {
            if (fWorkerResources.get(worker).get(resource.getDeclaration()).contains(resource)) {
                return worker;
            }
        }
        return null;
    }

    @Override
    public void removeResource(TmfWorker worker, TmfModelResource resource) {
        if (resource == null) {
            return;
        }
        if (!fWorkerResources.containsKey(worker)) {
            return;
        }
        fWorkerResources.get(worker).get(resource.getDeclaration()).remove(resource);
    }

    @Override
    public TmfModelResource find(TmfWorker worker, TmfModelElementFilter filter) {
        if (worker == null) {
            return null;
        }
        if (!fWorkerResources.containsKey(worker)) {
            return null;
        }
        for (TmfModelResource resource : fWorkerResources.get(worker).get(filter.getDeclaration())) {
            if (resource.filter(filter)) {
                return resource;
            }
        }
        return null;
    }

    @Override
    public TmfModelResource find(TmfModelElementFilter filter) {
        for (TmfModelResource resource : fResources.get(filter.getDeclaration())) {
            if (resource.filter(filter)) {
                return resource;
            }
        }
        return null;
    }

    private Stack<TmfModelResource> ensureStackExists(String host, Object key) {
        if (!fContextStackTable.contains(host, key)) {
            Stack<TmfModelResource> stack = new Stack<>();
            stack.push(fDefaultInterruptContext);
            fContextStackTable.put(host, key, stack);
        }
        return fContextStackTable.get(host, key);
    }

    @Override
    public void pushContextStack(String host, Object key, TmfModelResource resource) {
        Stack<TmfModelResource> stack = ensureStackExists(host, key);
        stack.add(resource);
    }

    @Override
    public TmfModelResource peekContextStack(String host, Object key) {
        Stack<TmfModelResource> stack = ensureStackExists(host, key);
        if (stack.isEmpty()) {
            return null;
        }
        return stack.peek();
    }

    @Override
    public TmfModelResource popContextStack(String host, Object key) {
        Stack<TmfModelResource> stack = ensureStackExists(host, key);
        if (stack.isEmpty()) {
            return null;
        }
        return stack.pop();
    }

    @Override
    public void setDefaultContextStack(TmfModelResource res) {
        fDefaultInterruptContext = res;
    }


    @Override
    public void reset() {
        isInitialized = false;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("Workers\n"); //$NON-NLS-1$
        for (TmfWorker worker : fWorkers.values()) {
            str.append(worker);
        }
        return str.toString();
    }

}
