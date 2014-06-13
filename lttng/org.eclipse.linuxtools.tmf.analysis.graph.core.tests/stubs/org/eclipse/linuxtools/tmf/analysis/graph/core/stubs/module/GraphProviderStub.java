/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien & Francis Giraldeau- Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.analysis.graph.core.stubs.module;

import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfVertex;
import org.eclipse.linuxtools.tmf.analysis.graph.core.building.AbstractTmfGraphProvider;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelElementFilter;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelRegistry;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfSystemModel;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfWorker;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfWorkerDeclaration;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

/**
 * Simple graph provider stub, just managing sched_switch
 *
 * @author Geneviève Bastien
 */
public class GraphProviderStub extends AbstractTmfGraphProvider {

    private static final int VERSION = 1;

    /**
     * Constructor
     *
     * @param registry
     *            The model registry
     * @param trace
     *            The trace
     * @param id
     *            The id of this graph provider
     */
    public GraphProviderStub(TmfModelRegistry registry, ITmfTrace trace,
            String id) {
        super(registry, trace, ITmfEvent.class, id);
        TmfSystemModel model = getModelRegistry().getOrCreateModel(TmfSystemModel.class);
        model.init(this);
    }

    @Override
    public void initializeModel(TmfSystemModel model) {
        TmfWorkerDeclaration workerDecl = new TmfWorkerDeclaration("worker");
        workerDecl.addSameField("id");
        workerDecl.addField("field1");

        model.addWorkerDeclaration(workerDecl);
    }

    @Override
    protected void eventHandle(ITmfEvent event) {
        String host = event.getTrace().getHostId();
        int cpu = Integer.parseInt(event.getSource());
        String evname = event.getType().getName();

        TmfSystemModel model = getModelRegistry().getModel(TmfSystemModel.class, true);

        if (evname.equals("sched_switch")) {
            TmfWorkerDeclaration workerDecl = model.getWorkerDeclaration("worker");
            TmfGraph graph = getAssignedGraph();
            Long prevTid = (Long) event.getContent().getField("prev_tid").getValue();
            Long nextTid = (Long) event.getContent().getField("next_tid").getValue();

            TmfModelElementFilter filter = new TmfModelElementFilter(workerDecl);
            filter.setValue("id", prevTid);

            TmfWorker prevWorker = model.getWorker(host, cpu, prevTid);
            if (prevWorker == null) {
                prevWorker = workerDecl.create(prevTid, event.getTrace());
                model.putWorker(prevWorker);
            }

            TmfVertex n0 = new TmfVertex(event.getTimestamp().getValue());
            graph.append(prevWorker, n0);

            TmfWorker nextWorker = model.getWorker(host, cpu, nextTid);
            if (nextWorker == null) {
                nextWorker = workerDecl.create(nextTid, event.getTrace());
                model.putWorker(nextWorker);
            }

            TmfVertex n1 = new TmfVertex(event.getTimestamp().getValue());
            graph.add(nextWorker, n1);

            graph.link(n0, n1);
        }
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

}
