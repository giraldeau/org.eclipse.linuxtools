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

package org.eclipse.linuxtools.tmf.analysis.graph.core.stubs;

import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfVertex;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfEdge.EdgeType;
import org.eclipse.linuxtools.tmf.analysis.graph.core.model.Ops;

/**
 * Factory generating various scenarios of graphs to test critical path
 * algorithms on
 *
 * @author Francis Giraldeau
 */
public class GraphFactory {

    /**
     * Simple RUNNING edge involving one object
     */
    public static final GraphBuilder GRAPH_BASIC =
            new GraphBuilder("basic", 1) {
                @Override
                public void build(int index) {
                    fData[index].head = Ops.basic(fData[index].len, EdgeType.RUNNING);
                }

                @Override
                public void buildData() {
                    for (int i = 0; i < fData.length; i++) {
                        fData[i] = new GraphBuilderData();
                        fData[i].id = i;
                        fData[i].len = 2;
                    }
                }

                @Override
                public void criticalPathBounded(int index) {
                    fData[index].bounded = makeit(index);
                }

                @Override
                public void criticalPathUnbounded(int index) {
                    fData[index].unbounded = makeit(index);
                }

                private TmfVertex makeit(int index) {
                    return Ops.basic(fData[index].len, EdgeType.RUNNING);
                }

            };

    /**
     * Single object, timer starts at t2 and wakes up at t4.  Blocked at t3
     */
    public static final GraphBuilder GRAPH_WAKEUP_SELF =
            new GraphBuilder("wakeup_self", 1) {
                @Override
                public void build(int index) {
                    TmfVertex t1 = Ops.sequence(5, fData[index].len, EdgeType.RUNNING);
                    TmfVertex t2 = Ops.seek(t1, 1);
                    TmfVertex t3 = Ops.seek(t1, 2);
                    TmfVertex t4 = Ops.seek(t1, 3);
                    t3.getEdges()[TmfVertex.OUTH].setType(EdgeType.BLOCKED);
                    t2.linkVertical(t4).setType(EdgeType.TIMER);
                    fData[index].head = t1;
                }

                @Override
                public void buildData() {
                    for (int i = 0; i < fData.length; i++) {
                        fData[i] = new GraphBuilderData();
                        fData[i].id = i;
                        fData[i].len = 1;
                    }
                }

                @Override
                public void criticalPathBounded(int index) {
                    TmfVertex n1 = Ops.sequence(5, fData[index].len, EdgeType.RUNNING);
                    Ops.seek(n1, 2).getEdges()[TmfVertex.OUTH].setType(EdgeType.TIMER);
                    fData[index].bounded = n1;
                }

                @Override
                public void criticalPathUnbounded(int index) {
                    TmfVertex n1 = Ops.basic(fData[index].len, EdgeType.RUNNING);
                    TmfVertex n2 = Ops.basic(fData[index].len * 2, EdgeType.TIMER);
                    TmfVertex n3 = Ops.basic(fData[index].len, EdgeType.RUNNING);
                    Ops.concatInPlace(n1, n2);
                    Ops.concatInPlace(n2, n3);
                    fData[index].unbounded = n1;
                }
            };

    /**
     * Single object, 4 vertices, blocked between 2 and 3, but nothing wakes up
     */
    public static final GraphBuilder GRAPH_WAKEUP_MISSING =
            new GraphBuilder("wakeup_missing", 1) {
                @Override
                public void build(int index) {
                    TmfVertex t1 = Ops.sequence(4, fData[index].len, EdgeType.RUNNING);
                    Ops.seek(t1, 1).getEdges()[TmfVertex.OUTH].setType(EdgeType.BLOCKED);
                    fData[index].head = t1;
                }

                @Override
                public void buildData() {
                    for (int i = 0; i < fData.length; i++) {
                        fData[i] = new GraphBuilderData();
                        fData[i].id = i;
                        fData[i].len = 2;
                    }
                }

                @Override
                public void criticalPathBounded(int index) {
                    TmfVertex n1 = Ops.sequence(4, fData[index].len, EdgeType.RUNNING);
                    Ops.seek(n1, 1).getEdges()[TmfVertex.OUTH].setType(EdgeType.BLOCKED);
                    fData[index].bounded = n1;
                }

                @Override
                public void criticalPathUnbounded(int index) {
                    fData[index].unbounded = new TmfVertex(0);
                }
            };

    /**
     * Object woken from blockage by another network object
     *
     * <pre>
     * o - R - o - B - o - R - o
     *         /
     *         o - N
     * </pre>
     */
    public static final GraphBuilder GRAPH_WAKEUP_UNKNOWN =
            new GraphBuilder("wakeup_unknown", 1) {
                @Override
                public void build(int index) {
                    TmfVertex t1 = Ops.sequence(4, fData[index].len, EdgeType.RUNNING);
                    Ops.seek(t1, 1).getEdges()[TmfVertex.OUTH].setType(EdgeType.BLOCKED);
                    TmfVertex t2 = new TmfVertex(fData[index].len * 2 - fData[index].delay);
                    t2.linkVertical(Ops.seek(t1, 2)).setType(EdgeType.NETWORK);
                    fData[index].head = t1;
                }

                @Override
                public void buildData() {
                    for (int i = 0; i < fData.length; i++) {
                        fData[i] = new GraphBuilderData();
                        fData[i].id = i;
                        fData[i].len = 2;
                        fData[i].delay = 1;
                    }
                }

                @Override
                public void criticalPathBounded(int index) {
                    TmfVertex n1 = Ops.basic(fData[index].len, EdgeType.RUNNING);
                    long duration = (fData[index].len <= fData[index].delay) ? 0 : fData[index].len - fData[index].delay;
                    TmfVertex n2 = Ops.basic(duration, EdgeType.UNKNOWN, fData[index].len);
                    TmfVertex n3 = Ops.basic(fData[index].len, EdgeType.RUNNING, fData[index].len * 2);
                    Ops.tail(n1).linkVertical(n2);
                    Ops.tail(n2).linkVertical(n3).setType(EdgeType.NETWORK);
                    fData[index].bounded = n1;
                }

                @Override
                public void criticalPathUnbounded(int index) {
                    TmfVertex n1 = Ops.basic(fData[index].len, EdgeType.RUNNING);
                    TmfVertex n2 = Ops.basic(fData[index].len - fData[index].delay, EdgeType.UNKNOWN);
                    TmfVertex n3 = Ops.basic(fData[index].len, EdgeType.RUNNING, fData[index].len * 2);
                    Ops.concatInPlace(n1, n2);
                    // Ops.offset(n3, fData[index].len * 2);
                    Ops.tail(n2).linkVertical(n3).setType(EdgeType.NETWORK);
                    fData[index].unbounded = n1;
                }
            };

    /**
     * Object woken from blockage by another running object that was created by
     * first object
     *
     * <pre>
     * o -R- o -R- o -B- o -R- o
     *        \          |
     *       ( o   ) -R- o
     * </pre>
     */
    public static GraphBuilder GRAPH_WAKEUP_NEW =
            new GraphBuilder("wakeup_new", 5) {
                @Override
                public void build(int index) {
                    TmfVertex t1 = Ops.sequence(5, fData[index].len, EdgeType.RUNNING);
                    Ops.seek(t1, 2).getEdges()[TmfVertex.OUTH].setType(EdgeType.BLOCKED);
                    TmfVertex t2 = Ops.basic(fData[index].len * 2 - fData[index].delay, EdgeType.RUNNING, fData[index].len + fData[index].delay);
                    Ops.seek(t1, 1).linkVertical(t2).setType(EdgeType.DEFAULT);
                    Ops.tail(t2).linkVertical(Ops.seek(t1, 3)).setType(EdgeType.DEFAULT);
                    fData[index].head = t1;
                }

                @Override
                public void buildData() {
                    for (int i = 0; i < fData.length; i++) {
                        fData[i] = new GraphBuilderData();
                        fData[i].id = i;
                        fData[i].len = 2;
                        fData[i].delay = i;
                    }
                }

                @Override
                public void criticalPathBounded(int index) {
                    TmfVertex n1 = Ops.sequence(3, fData[index].len, EdgeType.RUNNING);

                    long duration = (fData[index].delay < fData[index].len) ? fData[index].len : (fData[index].len * 2) - fData[index].delay;
                    TmfVertex n2 = Ops.basic(duration, EdgeType.RUNNING, fData[index].len * 2 + (fData[index].len - duration));
                    TmfVertex n3 = Ops.basic(fData[index].len, EdgeType.RUNNING, fData[index].len * 3);
                    Ops.seek(n1, 2).linkVertical(n2);
                    Ops.tail(n2).linkVertical(n3);
                    fData[index].bounded = n1;
                }

                @Override
                public void criticalPathUnbounded(int index) {
                    TmfVertex n1 = Ops.basic(fData[index].len, EdgeType.RUNNING);
                    TmfVertex n2 = Ops.basic(fData[index].len * 2 - fData[index].delay, EdgeType.RUNNING, fData[index].len + fData[index].delay);
                    TmfVertex n3 = Ops.basic(fData[index].len, EdgeType.RUNNING, fData[index].len * 3);
                    Ops.tail(n1).linkVertical(n2);
                    Ops.tail(n2).linkVertical(n3);
                    fData[index].unbounded = n1;
                }
            };

    /**
     * Two objects join to unblock the first but with or without delay
     *
     * <pre>
     * o --R-- o --B-- o --R--R
     *                /
     * o -R- (    o    ) --R-- o
     * </pre>
     */
    public static GraphBuilder GRAPH_OPENED =
            new GraphBuilder("opened", 5) {
                @Override
                public void build(int index) {
                    TmfVertex t1 = Ops.sequence(3, fData[index].len, EdgeType.RUNNING);
                    t1.getEdges()[TmfVertex.OUTH].getVertexTo().getEdges()[TmfVertex.OUTH].setType(EdgeType.BLOCKED);
                    TmfVertex t2 = Ops.basic(fData[index].len * 2 - fData[index].delay, EdgeType.RUNNING);
                    Ops.unionInPlaceRight(t1, t2, EdgeType.DEFAULT);
                    TmfVertex t3 = Ops.basic(fData[index].len, EdgeType.RUNNING);
                    TmfVertex t4 = Ops.basic(fData[index].len, EdgeType.RUNNING);
                    Ops.concatInPlace(t2, t3);
                    Ops.concatInPlace(t1, t4);
                    fData[index].head = t1;
                }

                @Override
                public void buildData() {
                    for (int i = 0; i < fData.length; i++) {
                        fData[i] = new GraphBuilderData();
                        fData[i].id = i;
                        fData[i].len = 3;
                        fData[i].delay = i;
                    }
                }

                @Override
                public void criticalPathBounded(int index) {
                    TmfVertex n1 = Ops.basic(fData[index].len, EdgeType.RUNNING);
                    long duration = (fData[index].len <= fData[index].delay) ? 0 : fData[index].len - fData[index].delay;
                    TmfVertex n2 = new TmfVertex(fData[index].len);
                    if (duration > 0) {
                        n2 = Ops.basic(duration, EdgeType.RUNNING, fData[index].len);
                    }
                    TmfVertex n3 = Ops.basic(fData[index].len, EdgeType.RUNNING, fData[index].len * 2);
                    Ops.tail(n1).linkVertical(n2);
                    Ops.tail(n2).linkVertical(n3);
                    fData[index].bounded = n1;
                }

                @Override
                public void criticalPathUnbounded(int index) {
                    fData[index].unbounded = new TmfVertex(0);
                }
            };

    /**
     * Two objects are blocked and mutually unblock at different times
     *
     * <pre>
     * o -R- o -R- o -R- o -B- o -R- o
     *             |           |
     * o -R- o -B- o -R- o -R- o -R- o
     * </pre>
     */
    public static GraphBuilder GRAPH_WAKEUP_MUTUAL =
            new GraphBuilder("wakeup_mutual", 1) {
                @Override
                public void build(int index) {
                    TmfVertex t1 = Ops.sequence(6, fData[index].len, EdgeType.RUNNING);
                    TmfVertex t2 = Ops.sequence(6, fData[index].len, EdgeType.RUNNING);
                    Ops.seek(t1, 3).getEdges()[TmfVertex.OUTH].setType(EdgeType.BLOCKED);
                    Ops.seek(t2, 1).getEdges()[TmfVertex.OUTH].setType(EdgeType.BLOCKED);
                    Ops.seek(t1, 2).linkVertical(Ops.seek(t2, 2));
                    Ops.seek(t2, 4).linkVertical(Ops.seek(t1, 4));
                    fData[index].head = t1;
                }

                @Override
                public void buildData() {
                    for (int i = 0; i < fData.length; i++) {
                        fData[i] = new GraphBuilderData();
                        fData[i].id = i;
                        fData[i].len = 1;
                    }
                }

                @Override
                public void criticalPathBounded(int index) {
                    TmfVertex n1 = Ops.sequence(4, fData[index].len, EdgeType.RUNNING);
                    TmfVertex n2 = Ops.basic(fData[index].len, EdgeType.RUNNING, Ops.tail(n1).getTs());
                    TmfVertex n3 = Ops.basic(fData[index].len, EdgeType.RUNNING, Ops.tail(n2).getTs());
                    Ops.tail(n1).linkVertical(n2);
                    Ops.tail(n2).linkVertical(n3);
                    fData[index].bounded = n1;
                }

                @Override
                public void criticalPathUnbounded(int index) {
                    TmfVertex n1 = Ops.sequence(3, fData[index].len, EdgeType.RUNNING);
                    TmfVertex n2 = Ops.sequence(3, fData[index].len, EdgeType.RUNNING, Ops.tail(n1).getTs());
                    TmfVertex n3 = Ops.basic(fData[index].len, EdgeType.RUNNING, Ops.tail(n2).getTs());
                    Ops.tail(n1).linkVertical(n2);
                    Ops.tail(n2).linkVertical(n3);
                    fData[index].unbounded = n1;
                }
            };

    /**
     * One or many objects wakeup the first object, the calls are embedded
     *
     * <pre>
     * o -R- o -R- o -R- o -B- o -B- o -R- o
     *       |     |           |     |
     *       |     o --- R --- o     |
     *       |                       |
     *       o    ------ R ------    o
     * ...
     * </pre>
     */
    public static GraphBuilder GRAPH_WAKEUP_EMBEDED =
            new GraphBuilder("wakeup_embeded", 3) {
                @Override
                public void build(int index) {
                    TmfVertex t1 = Ops.sequence(3, fData[index].len, EdgeType.RUNNING);
                    for (int i = 0; i < fData[index].depth; i++) {
                        Ops.tail(t1).getEdges()[TmfVertex.INH].setType(EdgeType.BLOCKED);
                        long duration = Ops.tail(t1).getTs() - t1.getTs();
                        TmfVertex sub = Ops.basic(duration, EdgeType.RUNNING);
                        Ops.unionInPlace(t1, sub, EdgeType.DEFAULT, EdgeType.DEFAULT);
                        TmfVertex x1 = Ops.basic(fData[index].len, EdgeType.RUNNING);
                        TmfVertex x2 = Ops.basic(fData[index].len, EdgeType.RUNNING);
                        Ops.concatInPlace(x1, t1);
                        Ops.concatInPlace(x1, x2);
                        t1 = x1;
                    }
                    fData[index].head = t1;
                }

                @Override
                public void buildData() {
                    for (int i = 0; i < fData.length; i++) {
                        fData[i] = new GraphBuilderData();
                        fData[i].id = i;
                        fData[i].len = 2;
                        fData[i].depth = i;
                    }
                }

                @Override
                public void criticalPathBounded(int index) {
                    TmfVertex n1 = Ops.sequence(fData[index].depth + 2, fData[index].len, EdgeType.RUNNING);
                    TmfVertex curr = n1;
                    for (int i = 0; i < fData[index].depth; i++) {
                        TmfVertex sub = Ops.basic(fData[index].len, EdgeType.RUNNING, Ops.tail(curr).getTs());
                        Ops.tail(curr).linkVertical(sub);
                        TmfVertex x = new TmfVertex(Ops.tail(sub));
                        Ops.tail(sub).linkVertical(x);
                        curr = x;
                    }
                    Ops.concatInPlace(curr, Ops.basic(fData[index].len, EdgeType.RUNNING));
                    fData[index].bounded = n1;
                }

                @Override
                public void criticalPathUnbounded(int index) {
                    fData[index].unbounded = new TmfVertex(0);
                }
            };

    /**
     * One or many objects wakeup the first object, the calls interleave
     *
     * <pre>
     * o -R- o -R- o -R- o -B- o -B- o -R- o
     *       |     |           |     |
     *       o ----- R -----   o     |
     *             |                 |
     *             o  ---- R ------  o
     * ...
     * </pre>
     */
    public static GraphBuilder GRAPH_WAKEUP_INTERLEAVE =
            new GraphBuilder("wakeup_interleave", 3) {
                @Override
                public void build(int index) {
                    TmfVertex t1 = Ops.sequence(3 + 2 * fData[index].depth, fData[index].len, EdgeType.RUNNING);
                    for (int i = 0; i < fData[index].depth; i++) {
                        TmfVertex x = Ops.seek(t1, i + 1);
                        TmfVertex y = Ops.seek(t1, i + 4);
                        y.getEdges()[TmfVertex.INH].setType(EdgeType.BLOCKED);
                        TmfVertex sub = Ops.basic(3 * fData[index].len, EdgeType.RUNNING, x.getTs());
                        // Ops.offset(sub, x.getTs() );
                        x.linkVertical(sub);
                        Ops.tail(sub).linkVertical(y);
                    }
                    Ops.concatInPlace(t1, Ops.basic(fData[index].len, EdgeType.RUNNING));
                    fData[index].head = t1;
                }

                @Override
                public void buildData() {
                    for (int i = 0; i < fData.length; i++) {
                        fData[i] = new GraphBuilderData();
                        fData[i].id = i;
                        fData[i].len = 2;
                        fData[i].depth = i;
                    }
                }

                @Override
                public void criticalPathBounded(int index) {
                    int n = fData[index].depth == 0 ? 3 : 4;
                    TmfVertex n1 = Ops.sequence(n, fData[index].len, EdgeType.RUNNING);
                    TmfVertex curr = n1;
                    for (int i = 0; i < fData[index].depth; i++) {
                        TmfVertex sub = Ops.basic(fData[index].len, EdgeType.RUNNING, Ops.tail(curr).getTs());
                        Ops.tail(curr).linkVertical(sub);
                        TmfVertex x = new TmfVertex(Ops.tail(sub));
                        Ops.tail(sub).linkVertical(x);
                        curr = x;
                    }
                    Ops.concatInPlace(curr, Ops.basic(fData[index].len, EdgeType.RUNNING));
                    if (fData[index].depth == 2) {
                        Ops.concatInPlace(curr, Ops.basic(fData[index].len, EdgeType.RUNNING));
                    }
                    fData[index].bounded = n1;
                }

                @Override
                public void criticalPathUnbounded(int index) {
                    fData[index].unbounded = new TmfVertex(0);
                }
            };

    /**
     * Objects block when creating new ones, nesting the blocks
     *
     * <pre>
     * ...
     * o -R- o -----     B     ----- o -R- o
     *       |                       |
     *       o -R- o   --B--   o -R- o
     *             |           |
     *             o --- R --- o
     * </pre>
     */
    public static GraphBuilder GRAPH_NESTED =
            new GraphBuilder("wakeup_nested", 4) {
                @Override
                public void build(int index) {
                    TmfVertex inner = Ops.basic(fData[index].len * 2, EdgeType.RUNNING);
                    for (int i = 0; i < fData[index].depth; i++) {
                        long duration = Ops.tail(inner).getTs() - Ops.head(inner).getTs();
                        TmfVertex t1 = Ops.basic(fData[index].len, EdgeType.RUNNING);
                        TmfVertex t2 = Ops.basic(duration, EdgeType.BLOCKED);
                        TmfVertex t3 = Ops.basic(fData[index].len, EdgeType.RUNNING);
                        inner = Ops.alignRight(t2, inner);

                        Ops.unionInPlace(t2, inner, EdgeType.DEFAULT, EdgeType.DEFAULT);
                        Ops.concatInPlace(t2, t3);
                        Ops.concatInPlace(t1, t2);
                        inner = t1;
                    }
                    fData[index].head = inner;
                }

                @Override
                public void buildData() {
                    for (int i = 0; i < fData.length; i++) {
                        fData[i] = new GraphBuilderData();
                        fData[i].id = i;
                        fData[i].len = 1;
                        fData[i].depth = i;
                    }
                }

                @Override
                public void criticalPathBounded(int index) {
                    TmfVertex inner = Ops.basic(fData[index].len * 2, EdgeType.RUNNING);
                    for (int i = 0; i < fData[index].depth; i++) {
                        TmfVertex t1 = Ops.basic(fData[index].len, EdgeType.RUNNING);
                        inner = Ops.offset(inner, fData[index].len);
                        Ops.tail(t1).linkVertical(inner);
                        // TODO : debug here when depth > 1 it seems the tail is
                        // not attached... graph shortened
                        TmfVertex t2 = Ops.basic(fData[index].len, EdgeType.RUNNING, Ops.end(inner).getTs());
                        Ops.end(inner).linkVertical(t2);
                        inner = t1;
                    }
                    fData[index].bounded = inner;
                }

                @Override
                public void criticalPathUnbounded(int index) {
                    if (fData[index].bounded == null) {
                        criticalPathBounded(index);
                    }
                    fData[index].unbounded = fData[index].bounded;
                }
            };

    /**
     * An object is blocked until a few other objects exchange network messages
     *
     * <pre>
     * o -R- o ----------------------- B ---------------------- o -R- o
     *                                                          |
     *           o -R- o -R- o                                  |
     *                  \               \                       |
     *                            o -R- o -R- o                 |
     *                                         \         \      |
     *                                              o -R- o -R- o
     * </pre>
     */
    public static final GraphBuilder GRAPH_NET1 =
            new GraphBuilder("wakeup_net1", 1) {
                @Override
                public void build(int index) {
                    TmfVertex n1 = Ops.basic(fData[index].len, EdgeType.RUNNING);
                    TmfVertex n2 = Ops.basic(fData[index].len * 10, EdgeType.BLOCKED);
                    TmfVertex n3 = Ops.basic(fData[index].len, EdgeType.RUNNING);
                    Ops.concatInPlace(n1, n2);
                    Ops.concatInPlace(n1, n3);

                    TmfVertex n4 = Ops.sequence(3, fData[index].len, EdgeType.RUNNING, fData[index].len * 3);
                    TmfVertex n5 = Ops.sequence(3, fData[index].len, EdgeType.RUNNING, fData[index].len * 6);
                    TmfVertex n6 = Ops.sequence(3, fData[index].len, EdgeType.RUNNING, fData[index].len * 9);

                    Ops.seek(n4, 1).linkVertical(Ops.seek(n5, 1)).setType(EdgeType.NETWORK);
                    Ops.seek(n5, 2).linkVertical(Ops.seek(n6, 1)).setType(EdgeType.NETWORK);
                    Ops.seek(n6, 2).linkVertical(Ops.seek(n1, 2));
                    fData[index].head = n1;
                }

                @Override
                public void buildData() {
                    for (int i = 0; i < fData.length; i++) {
                        fData[i] = new GraphBuilderData();
                        fData[i].id = i;
                        fData[i].len = 1;
                    }
                }

                @Override
                public void criticalPathBounded(int index) {
                    TmfVertex n1 = Ops.basic(fData[index].len, EdgeType.RUNNING);
                    TmfVertex n2 = Ops.basic(fData[index].len * 2, EdgeType.UNKNOWN, fData[index].len);
                    TmfVertex n3 = Ops.basic(fData[index].len, EdgeType.RUNNING);
                    TmfVertex n4 = Ops.basic(fData[index].len, EdgeType.RUNNING, fData[index].len * 7);
                    TmfVertex n5 = Ops.basic(fData[index].len, EdgeType.RUNNING, fData[index].len * 10);
                    TmfVertex n6 = Ops.basic(fData[index].len, EdgeType.RUNNING, fData[index].len * 11);
                    Ops.concatInPlace(n2, n3);
                    // Ops.offset(n2, fData[index].len);
                    // Ops.offset(n4, fData[index].len * 7);
                    // Ops.offset(n5, fData[index].len * 10);
                    // Ops.offset(n6, fData[index].len * 11);
                    Ops.tail(n1).linkVertical(n2).setType(EdgeType.DEFAULT);
                    Ops.tail(n2).linkVertical(n4).setType(EdgeType.NETWORK);
                    Ops.tail(n4).linkVertical(n5).setType(EdgeType.NETWORK);
                    Ops.tail(n5).linkVertical(n6).setType(EdgeType.DEFAULT);
                    fData[index].bounded = n1;
                }

                @Override
                public void criticalPathUnbounded(int index) {
                    if (fData[index].bounded == null) {
                        criticalPathBounded(index);
                    }
                    fData[index].unbounded = fData[index].bounded;
                }
            };

}
