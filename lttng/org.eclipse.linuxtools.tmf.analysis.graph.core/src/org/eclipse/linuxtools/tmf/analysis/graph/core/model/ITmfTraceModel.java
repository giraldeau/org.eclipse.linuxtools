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

import java.util.Collection;

import org.eclipse.linuxtools.tmf.analysis.graph.core.building.AbstractTmfGraphProvider;

/**
 * Interface to describe a model of the system analyzed by the trace. This model
 * is a structure useful to keep tracks of workers and resources when building a
 * graph.
 *
 * Note: All classes in this package are intended as a temporary structure for
 * building graph or other analysis structures. It is not meant to be saved, or
 * to be used as is for analysis or display.
 *
 * @since 3.0
 */
public interface ITmfTraceModel {

    /**
     * TODO: needed?
     */
    void reset();

    /**
     * Initialize the trace model
     *
     * @param provider
     *            The graph provider that will build the graph
     */
    void init(AbstractTmfGraphProvider provider);

    /**
     * Get all workers
     *
     * @return All workers in the model
     */
    Collection<TmfWorker> getWorkers();

    /**
     * Get the worker with the requested id
     * @param host
     *            The host the worker is part of
     * @param wid
     *            The id of the worker
     * @param cpu
     *            The CPU
     * @return The requested worker
     */
    TmfWorker getWorker(String host, int cpu, long wid);

    /**
     * Add a worker
     *
     * @param worker
     */
    void putWorker(TmfWorker worker);

    /**
     * Gets the worker declaration of the given name
     *
     * @param worker
     *            Name of the worker to get
     * @return The corresponding worker definition, null if absent
     */
    TmfWorkerDeclaration getWorkerDeclaration(String worker);

    /**
     * Adds a new worker declaration to the model
     *
     * @param declaration
     *            The worker declaration
     */
    void addWorkerDeclaration(TmfWorkerDeclaration declaration);

    /**
     * Adds a new resource declaration to the model
     *
     * @param declaration
     *            The resource declaration
     */
    void addResourceDeclaration(TmfModelResourceDeclaration declaration);

    /**
     * Gets the resource declaration of the given name
     *
     * @param resource
     *            Name of the resource to get
     * @return The corresponding resource definition, null if absent
     */
    TmfModelResourceDeclaration getResourceDeclaration(String resource);

    /**
     * Adds a resource to a worker
     *
     * @param worker
     *            The worker the resource belongs to
     * @param res
     *            The model resource
     */
    void addWorkerResource(TmfWorker worker, TmfModelResource res);

    /**
     * Add a resource to the model, not associated with any worker
     *
     * @param resource
     *            The resource to add
     */
    void addResource(TmfModelResource resource);

    /**
     * Get all resources of a given type in the model
     *
     * @param name
     *            The name of the resource type in the corresponding resource
     *            delcaration
     * @return The collection of resources of requested type
     */
    Collection<TmfModelResource> getResources(String name);

    /**
     * Get all resources of a given type in the model
     *
     * @param decl
     *            The resource declaration of the requested resources
     * @return the collection of resources of requested type
     */
    Collection<TmfModelResource> getResources(TmfModelElementDeclaration decl);

    /**
     * Get all resources of a type for a worker
     *
     * @param worker
     *            The worker to get resources for
     * @param decl
     *            The resource declaration of the requested resources
     * @return The collection of resources for the requested worker
     */
    Collection<TmfModelResource> getWorkerResources(TmfWorker worker, TmfModelElementDeclaration decl);

    /**
     * Gets the owner of a resource
     *
     * @param resource
     *            The resource to get owner for
     * @return The worker who owns this resource, or null if none
     */
    TmfWorker getResourceOwner(TmfModelResource resource);

    /**
     * Removes a resource from a worker's list of resources
     *
     * @param worker
     *            The worker fromw which to remove the resource
     * @param resource
     *            The resource
     */
    void removeResource(TmfWorker worker, TmfModelResource resource);

    /**
     * Finds a resource among a worker's resources
     *
     * @param worker
     *            The worker to search
     * @param filter
     *            The search criteria
     * @return The first resource corresponding to criterias, or null if not
     *         found
     */
    TmfModelResource find(TmfWorker worker, TmfModelElementFilter filter);

    /**
     * Finds a resource
     *
     * @param filter
     *            The search criteria
     * @return The first resource corresponding to criterias, or null if not
     *         found
     */
    TmfModelResource find(TmfModelElementFilter filter);

    /**
     * Push a resource on a stack for the given object
     * @param host
     *            The host name
     * @param key
     *            The key for the stack
     * @param resource
     *            The resource to push
     */
    void pushContextStack(String host, Object key, TmfModelResource resource);

    /**
     * Gets the resource at the top of a stack, without removing it
     * @param host
     *            The host name
     * @param key
     *            The key for the stack
     * @return The resource at top of stack, null if stack empty
     */
    TmfModelResource peekContextStack(String host, Object key);

    /**
     * Gets the resource at the top of a stack and removes it from stack
     * @param host
     *            The host name
     * @param key
     *            The key for the stack
     * @return The resource at top of stack, null if stack empty
     */
    TmfModelResource popContextStack(String host, Object key);

    /**
     * Set the default context element when creating new context stack
     * @param resource the resource instance
     */
    void setDefaultContextStack(TmfModelResource resource);

}
