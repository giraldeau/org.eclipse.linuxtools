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

package org.eclipse.linuxtools.tmf.analysis.graph.core.tests.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.linuxtools.tmf.analysis.graph.core.model.ITmfWorkerFactory;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelResourceDeclaration;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfSystemModel;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfSystemModelWithCpu;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfWorker;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfWorkerDeclaration;
import org.junit.Before;
import org.junit.Test;

/**
 * Test suite for the {@link TmfSystemModel} class
 */
public class TmfSystemModelTest {

    private TmfSystemModel fModel = new TmfSystemModel();

    /**
     * Setting up the model
     */
    @Before
    public void setupModel() {
        TmfWorkerDeclaration workerDecl = new TmfWorkerDeclaration("worker");
        workerDecl.addSameField("id");
        workerDecl.addField("field1");

        fModel.addWorkerDeclaration(workerDecl);

        TmfModelResourceDeclaration resourceDecl = new TmfModelResourceDeclaration(
                "resource");
        resourceDecl.addSameField("id");
        resourceDecl.addField("field1");
        resourceDecl.addField("field2");

        fModel.addResourceDeclaration(resourceDecl);

        TmfModelResourceDeclaration resourceDecl2 = new TmfModelResourceDeclaration(
                "resource2");
        resourceDecl2.addSameField("id");
        resourceDecl2.addField("field1");
        resourceDecl2.addField("field2");

        fModel.addResourceDeclaration(resourceDecl2);
    }

    /**
     * Basic worker and resources setting tests
     */
    @Test
    public void testSystemModelBase() {
        /* TODO: Replace with test trace */
        assertTrue(true);
//        CtfTmfTrace trace1 = CtfTmfTestTrace.SYNC_SRC.getTrace();
//        TmfWorkerDeclaration workerDecl = fModel.getWorkerDeclaration("worker");
//        TmfModelResourceDeclaration resourceDecl = fModel
//                .getResourceDeclaration("resource");
//        TmfModelResourceDeclaration resourceDecl2 = fModel
//                .getResourceDeclaration("resource2");
//
//        TmfWorker worker1 = workerDecl.create(1, trace1);
//        TmfWorker worker2 = workerDecl.create(2, trace1);
//
//        worker1.setField("id", 1234);
//        worker1.setField("field1", 3);
//
//        fModel.putWorker(worker1);
//
//        worker2.setField("id", 2345);
//        worker2.setField("field1", 4);
//
//        fModel.putWorker(worker2);
//
//        assertEquals(2, fModel.getWorkers().size());
//
//        TmfModelResource commonRes = resourceDecl.create();
//        commonRes.setField("id", 1);
//        commonRes.setField("field1", 3);
//
//        fModel.addResource(commonRes);
//
//        TmfModelResource res1 = resourceDecl.create();
//        res1.setField("id", 2);
//        res1.setField("field1", 12);
//
//        fModel.addWorkerResource(worker1, res1);
//
//        TmfModelResource res2 = resourceDecl.create();
//        res2.setField("id", 3);
//        res2.setField("field1", 3);
//
//        fModel.addWorkerResource(worker2, res2);
//
//        assertEquals(3, fModel.getResources(resourceDecl).size());
//        assertEquals(1, fModel.getWorkerResources(worker1, resourceDecl).size());
//        assertEquals(1, fModel.getWorkerResources(worker2, resourceDecl).size());
//        assertEquals(0, fModel.getWorkerResources(worker2, resourceDecl2).size());
//        assertEquals(0, fModel.getResources(resourceDecl2).size());

    }

    /**
     * Tests of resource searching and filtering
     */
    @Test
    public void testSystemModelFilter() {
        /* TODO: Use a test trace instead */
        assertTrue(true);
//        CtfTmfTrace trace1 = CtfTmfTestTrace.SYNC_SRC.getTrace();
//        TmfWorkerDeclaration workerDecl = fModel.getWorkerDeclaration("worker");
//        TmfModelResourceDeclaration resourceDecl = fModel
//                .getResourceDeclaration("resource");
//        TmfModelResourceDeclaration resourceDecl2 = fModel
//                .getResourceDeclaration("resource2");
//
//        TmfWorker worker1 = workerDecl.create(1, trace1);
//
//        worker1.setField("id", 1234);
//        worker1.setField("field1", 3);
//
//        fModel.putWorker(worker1);
//
//        TmfModelResource res1 = resourceDecl.create();
//        res1.setField("id", 2);
//        res1.setField("field1", 12);
//
//        fModel.addWorkerResource(worker1, res1);
//
//        TmfModelResource res2 = resourceDecl.create();
//        res2.setField("id", 3);
//        res2.setField("field1", 3);
//
//        fModel.addWorkerResource(worker1, res2);
//
//        TmfModelResource res3 = resourceDecl2.create();
//        res3.setField("id", 3);
//        res3.setField("field1", 4);
//
//        fModel.addWorkerResource(worker1, res3);
//
//        TmfModelElementFilter filter = new TmfModelElementFilter(resourceDecl);
//
//        filter.setValue("id", 9);
//
//        TmfModelResource filtered = fModel.find(filter);
//        assertNull(filtered);
//
//        filter.setValue("id", 3);
//        filtered = fModel.find(filter);
//        assertTrue(filtered == res2);
//
//        filtered = fModel.find(worker1, filter);
//        assertTrue(filtered == res2);
//
//        res3.setField("id", 5);
//        filter.setValue("id", 5);
//        filtered = fModel.find(filter);
//        assertNull(filtered);
//
//        res1.setField("id", 6);
//        filter.setValue("id", 6);
//        filtered = fModel.find(filter);
//        assertTrue(filtered == res1);

    }

    /**
     * Test the model's context stack
     */
    @Test
    public void testSystemModelStack() {
        /* TODO: replace with test trace */
        assertTrue(true);
//        CtfTmfTrace trace1 = CtfTmfTestTrace.SYNC_SRC.getTrace();
//        TmfWorkerDeclaration workerDecl = fModel.getWorkerDeclaration("worker");
//        TmfModelResourceDeclaration resourceDecl = fModel
//                .getResourceDeclaration("resource");
//
//        String host = trace1.getHostId();
//        TmfWorker worker1 = workerDecl.create(1, trace1);
//
//        worker1.setField("id", 1234);
//        worker1.setField("field1", 3);
//
//        fModel.putWorker(worker1);
//
//        TmfModelResource res1 = resourceDecl.create();
//        res1.setField("id", 2);
//        res1.setField("field1", 12);
//
//        TmfModelResource res2 = resourceDecl.create();
//        res2.setField("id", 4);
//        res2.setField("field1", 11);
//
//        TmfModelResource res3 = resourceDecl.create();
//        res3.setField("id", 5);
//        res3.setField("field1", 13);
//
//        fModel.pushContextStack(host, 1, res1);
//        fModel.pushContextStack(host, 1, res2);
//        fModel.pushContextStack(host, 1, res3);
//
//        /* Check is resource is the same reference */
//        assertNull(fModel.peekContextStack(host, 2));
//        assertTrue(fModel.peekContextStack(host, 1) == res3);
//        assertTrue(fModel.popContextStack(host, 1) == res3);
//        assertTrue(fModel.peekContextStack(host, 1) == res2);
//        assertTrue(fModel.popContextStack(host, 1) == res2);
//        assertTrue(fModel.popContextStack(host, 1) == res1);
//        assertNull(fModel.peekContextStack(host, 1));
//        assertNull(fModel.popContextStack(host, 1));
    }

    /**
     * Test that swapper worker are created if not existing
     */
    @Test
    public void testSwapperFactory() {
        final String name = "swapper"; //$NON-NLS-1$
        final String idField = "id"; //$NON-NLS-1$
        final int id = 42;
        TmfSystemModelWithCpu model = new TmfSystemModelWithCpu();
        final TmfWorkerDeclaration workerDecl = new TmfWorkerDeclaration("worker");
        workerDecl.addSameField(idField);
        model.addWorkerDeclaration(workerDecl);
        model.setSwapperFactory(new ITmfWorkerFactory() {
            @Override
            public TmfWorker createModelElement(String host, int cpu, long wid) {
                TmfWorker swapper = workerDecl.create(0, null);
                swapper.setName(name);
                swapper.setField(idField, id);
                return swapper;
            }
        });

        TmfWorker current = model.getWorker("none", 0, 0);
        assertNotNull(current);
        assertEquals(current.getName(), name);
        assertEquals(current.getField(idField), id);
    }

}
