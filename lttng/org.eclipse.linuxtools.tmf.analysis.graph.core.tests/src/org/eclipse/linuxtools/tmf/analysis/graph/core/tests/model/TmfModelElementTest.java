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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.linuxtools.tmf.analysis.graph.core.model.ITmfModelElement;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelElementDeclaration;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelElementFilter;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelResource;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfModelResourceDeclaration;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfWorker;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.TmfWorkerDeclaration;
import org.junit.Test;

/**
 * Unit tests for {@link ITmfModelElement} and
 * {@link TmfModelElementDeclaration} classes and their descendants
 */
public class TmfModelElementTest {

    /**
     * Test suite for {@link TmfWorkerDeclaration}
     */
    @Test
    public void testWorkerDeclaration() {
        TmfWorkerDeclaration decl = new TmfWorkerDeclaration("worker");
        decl.addSameField("id");
        decl.addSameField("id2");
        decl.addField("field1");
        decl.addKeepField("keep1");
        Exception exception = null;
        try {
            decl.addComplementFields("field1", "field2");
        } catch (RuntimeException e) {
            exception = e;
        }
        /* fields must exist */
        assertNotNull(exception);
        decl.addField("field2");
        try {
            decl.addComplementFields("field1", "field2");
        } catch (RuntimeException e) {
            fail(e.getMessage());
        }

        assertEquals(5, decl.getFieldsList().size());
        assertEquals(1, decl.getKeepFieldsList().size());
        assertEquals(2, decl.getSameFieldsList().size());

        TmfWorker worker = decl.create();

        assertEquals(decl, worker.getDeclaration());
    }

    /**
     * Test suite for {@link TmfModelResourceDeclaration}
     */
    @Test
    public void testResourceDeclaration() {
        TmfModelResourceDeclaration decl = new TmfModelResourceDeclaration(
                "worker");
        decl.addSameField("id");
        decl.addSameField("id2");
        decl.addField("field1");
        decl.addKeepField("keep1");
        Exception exception = null;
        try {
            decl.addComplementFields("field1", "field2");
        } catch (RuntimeException e) {
            exception = e;
        }
        /* fields must exist */
        assertNotNull(exception);
        decl.addField("field2");
        try {
            decl.addComplementFields("field1", "field2");
        } catch (RuntimeException e) {
            fail(e.getMessage());
        }

        assertEquals(5, decl.getFieldsList().size());
        assertEquals(1, decl.getKeepFieldsList().size());
        assertEquals(2, decl.getSameFieldsList().size());

        TmfModelResource resource = decl.create();

        assertEquals(decl, resource.getDeclaration());
    }

    /**
     * Test suite for {@link TmfWorker}
     */
    @Test
    public void testWorker() {
        TmfWorkerDeclaration decl = new TmfWorkerDeclaration("worker");
        decl.addSameField("id");
        decl.addSameField("id2");
        decl.addField("field1");
        decl.addKeepField("keep1");
        decl.addField("field2");
        decl.addComplementFields("field1", "field2");

        TmfWorker worker = decl.create();
        assertEquals(decl, worker.getDeclaration());

        /* Test setting fields */
        worker.setField("id", 1);
        worker.setField("id2", 3);
        assertEquals(1, worker.getField("id"));
        assertEquals(3, worker.getField("id2"));

        /* Test setting unexistent field */
        Exception exception = null;
        try {
            worker.setField("id3", 2);
        } catch (RuntimeException e) {
            exception = e;
        }
        assertNotNull(exception);

        /* Test keep value field */
        worker.setField("keep1", 5);
        assertEquals(5, worker.getField("keep1"));
        worker.setField("keep1", 6);
        assertEquals(6, worker.getField("keep1"));
        assertEquals(5, worker.getOldValue("keep1"));

        /* Set start and end */
        worker.setStart(1234);
        worker.setEnd(5780854787689725542L);
        assertEquals(1234, worker.getStart());
        assertEquals(5780854787689725542L, worker.getEnd());

        /* Test the isSame method */
        TmfWorker worker2 = decl.create();
        worker2.setField("id", 1);
        worker2.setField("id2", 2);

        assertFalse(worker.isSame(worker2));
        worker2.setField("id2", 3);
        assertTrue(worker.isSame(worker2));
        assertTrue(worker2.isSame(worker));
    }

    /**
     * Test suite for {@link TmfModelResource}
     */
    @Test
    public void testModelResource() {
        TmfModelResourceDeclaration decl = new TmfModelResourceDeclaration("resource");
        decl.addSameField("id");
        decl.addSameField("id2");
        decl.addField("field1");
        decl.addKeepField("keep1");
        decl.addField("field2");
        decl.addComplementFields("field1", "field2");

        TmfModelResource resource = decl.create();
        assertEquals(decl, resource.getDeclaration());

        /* Test setting fields */
        resource.setField("id", 1);
        resource.setField("id2", 3);
        assertEquals(1, resource.getField("id"));
        assertEquals(3, resource.getField("id2"));

        /* Test setting unexistent field */
        Exception exception = null;
        try {
            resource.setField("id3", 2);
        } catch (RuntimeException e) {
            exception = e;
        }
        assertNotNull(exception);

        /* Test keep value field */
        resource.setField("keep1", 5);
        assertEquals(5, resource.getField("keep1"));
        resource.setField("keep1", 6);
        assertEquals(6, resource.getField("keep1"));
        assertEquals(5, resource.getOldValue("keep1"));

        /* Test the isSame method */
        TmfModelResource resource2 = decl.create();
        resource2.setField("id", 1);
        resource2.setField("id2", 2);

        assertFalse(resource.isSame(resource2));
        resource2.setField("id2", 3);
        assertTrue(resource.isSame(resource2));
        assertTrue(resource2.isSame(resource));
    }

    /**
     * Test suite for {@link TmfModelElementFilter}
     */
    @Test
    public void testModelFilter() {
        TmfModelResourceDeclaration decl = new TmfModelResourceDeclaration("resource");
        decl.addSameField("id");
        decl.addSameField("id2");
        decl.addField("field1");
        decl.addField("field2");

        TmfModelResource resource = decl.create();
        assertEquals(decl, resource.getDeclaration());

        /* Test setting fields */
        resource.setField("id", 1);
        resource.setField("id2", 2);
        resource.setField("field1", 3);
        resource.setField("field2", 4);

        TmfModelElementFilter filter = new TmfModelElementFilter(decl);
        filter.setValue("field1", 4);
        filter.setValue("field2", 4);

        assertFalse(resource.filter(filter));
        filter.setValue("field1", 3);
        assertTrue(resource.filter(filter));

        TmfWorkerDeclaration workerDecl = new TmfWorkerDeclaration("worker");
        workerDecl.addSameField("id");
        workerDecl.addSameField("id2");
        workerDecl.addField("field1");
        workerDecl.addField("field2");

        TmfModelElementFilter filter2 = new TmfModelElementFilter(workerDecl);
        filter2.setValue("field1", 4);
        filter2.setValue("field2", 4);

        assertFalse(resource.filter(filter2));
        filter2.setValue("field1", 3);
        assertFalse(resource.filter(filter2));
    }

}
