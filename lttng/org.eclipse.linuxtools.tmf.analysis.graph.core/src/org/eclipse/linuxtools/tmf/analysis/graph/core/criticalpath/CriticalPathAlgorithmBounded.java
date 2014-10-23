/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francis Giraldeau- Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.analysis.graph.core.criticalpath;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfEdge;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfEdge.EdgeType;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfVertex;

/**
 * Critical path bounded algorithm: backward resolution of blockage limited to
 * the blocking window
 *
 * This algorithm is described in
 *
 * F. Giraldeau and M.Dagenais,
 * "System-level Computation of Program Execution Critical Path", not published
 * yet
 *
 * @author Francis Giraldeau
 * @since 3.0
 */
public class CriticalPathAlgorithmBounded extends AbstractCriticalPathAlgorithm {

    /**
     * Constructor
     *
     * @param main
     *            The graph on which to calculate the critical path
     */
    public CriticalPathAlgorithmBounded(TmfGraph main) {
        super(main);
    }

    @Override
    public TmfGraph compute(TmfVertex start, TmfVertex end) {
        TmfGraph path = new TmfGraph();
        if (start == null) {
            return path;
        }
        Object parent = getGraph().getParentOf(start);
        path.add(parent, new TmfVertex(start));
        TmfVertex curr = start;
        while (curr.hasNeighbor(TmfVertex.OUTH)) {
            TmfVertex next = curr.neighbor(TmfVertex.OUTH);
            TmfEdge link = curr.getEdges()[TmfVertex.OUTH];
            switch (link.getType()) {
            case USER_INPUT:
            case BLOCK_DEVICE:
            case TIMER:
            case INTERRUPTED:
            case PREEMPTED:
            case RUNNING:
                path.append(getGraph().getParentOf(link.getVertexTo()), new TmfVertex(link.getVertexTo()), link.getType());
                break;
            case NETWORK:
            case BLOCKED:
                List<TmfEdge> links = resolveBlockingBounded(link, link.getVertexFrom());
                Collections.reverse(links);
                glue(path, curr, links);
                break;
            case EPS:
                if (link.getDuration() != 0) {
                    throw new RuntimeException("epsilon duration is not zero " + link); //$NON-NLS-1$
                }
                break;
            case DEFAULT:
                throw new RuntimeException("Illegal link type " + link.getType()); //$NON-NLS-1$
            case UNKNOWN:
            default:
                break;
            }
            curr = next;
        }
        return path;
    }

    private void glue(TmfGraph path, TmfVertex curr, List<TmfEdge> links) {
        Object currentActor = getGraph().getParentOf(curr);
        if (links.isEmpty()) {
            TmfVertex next = curr.neighbor(TmfVertex.OUTH);
            path.append(currentActor, new TmfVertex(next), curr.getEdges()[TmfVertex.OUTH].getType());
            return;
        }
        // FIXME: assert last link.to actor == currentActor

        // attach subpath to b1 and b2
        TmfVertex b1 = path.getTail(currentActor);
        // TmfVertex b2 = new TmfVertex(curr.neighbor(TmfVertex.OUTH));
        TmfVertex anchor;

        // glue head
        TmfEdge lnk = links.get(0);
        Object objSrc = getGraph().getParentOf(lnk.getVertexFrom());
        if (objSrc == currentActor) {
            anchor = b1;
        } else {
            anchor = new TmfVertex(curr);
            path.add(objSrc, anchor);
            b1.linkVertical(anchor);
            // fill any gap with UNKNOWN
            if (lnk.getVertexFrom().compareTo(anchor) > 0) {
                anchor = new TmfVertex(lnk.getVertexFrom());
                path.append(objSrc, anchor).setType(TmfEdge.EdgeType.UNKNOWN);
            }
        }

        // glue body
        TmfEdge prev = null;
        for (TmfEdge link : links) {
            // check connectivity
            if (prev != null && prev.getVertexTo() != link.getVertexFrom()) {
                anchor = copyLink(path, anchor, prev.getVertexTo(), link.getVertexFrom(),
                        prev.getVertexTo().getTs(), TmfEdge.EdgeType.DEFAULT);
            }
            anchor = copyLink(path, anchor, link.getVertexFrom(), link.getVertexTo(),
                    link.getVertexTo().getTs(), link.getType());
            prev = link;
        }
    }

    // FIXME: build a tree with partial subpath in order to return the best
    // path,
    // not the last one traversed
    private List<TmfEdge> resolveBlockingBounded(TmfEdge blocking, TmfVertex bound) {
        TmfVertex currentBound = bound;

        LinkedList<TmfEdge> subPath = new LinkedList<>();
        TmfVertex junction = findIncoming(blocking.getVertexTo(), TmfVertex.OUTH);
        /* if wake-up source is not found, return empty list */
        if (junction == null) {
            return subPath;
        }
        TmfEdge down = junction.getEdges()[TmfVertex.INV];
        subPath.add(down);
        TmfVertex node = down.getVertexFrom();
        currentBound = currentBound.compareTo(blocking.getVertexFrom()) < 0 ? blocking.getVertexFrom() : currentBound;
        Stack<TmfVertex> stack = new Stack<>();

        // HACK: look ahead for incoming network edge in case wakeup occurs *before* the packet tracepoint
        if (node.hasNeighbor(TmfVertex.OUTH)) {
            TmfVertex next = node.neighbor(TmfVertex.OUTH);
            if (next.hasNeighbor(TmfVertex.INV) &&
                    next.getEdges()[TmfVertex.INV].getType() == TmfEdge.EdgeType.NETWORK) {
                TmfGraph graph = getGraph();
                TmfVertex from = next.neighbor(TmfVertex.INV);
                TmfVertex hack = new TmfVertex(node.getTs());
                graph.add(graph.getParentOf(next), hack);
                if (node.hasNeighbor(TmfVertex.INH)) {
                    EdgeType type = node.getEdges()[TmfVertex.INH].getType();
                    node.neighbor(TmfVertex.INH).linkHorizontal(hack).setType(type);
                }
                EdgeType type = node.getEdges()[TmfVertex.OUTH].getType();
                hack.linkHorizontal(node).setType(type);
                from.linkVertical(hack).setType(TmfEdge.EdgeType.NETWORK);
            }
        }

        while (node != null && node.compareTo(currentBound) > 0) {
            /* shortcut for down link that goes beyond the blocking */
            if (node.hasNeighbor(TmfVertex.INV) && node.inv().compareTo(currentBound) <= 0) {
                subPath.add(node.getEdges()[TmfVertex.INV]);
                break;
            }

            /*
             * Add DOWN links to explore stack in case dead-end occurs Do not
             * add if left is BLOCKED, because this link would be visited twice
             */
            if (node.hasNeighbor(TmfVertex.INV) &&
                    (!node.hasNeighbor(TmfVertex.INH) || (node.hasNeighbor(TmfVertex.INH)
                    && (node.getEdges()[TmfVertex.INH].getType() != TmfEdge.EdgeType.BLOCKED ||
                    node.getEdges()[TmfVertex.INH].getType() != TmfEdge.EdgeType.NETWORK)))) {
                stack.push(node);
            }
            if (node.hasNeighbor(TmfVertex.INH)) {
                TmfEdge link = node.getEdges()[TmfVertex.INH];
                if (link.getType() == TmfEdge.EdgeType.BLOCKED || link.getType() == TmfEdge.EdgeType.NETWORK) {
                    subPath.addAll(resolveBlockingBounded(link, currentBound));
                } else {
                    subPath.add(link);
                }
            } else {
                if (!stack.isEmpty()) {
                    TmfVertex n = stack.pop();
                    /* rewind subpath */
                    while (!subPath.isEmpty() && subPath.getLast().getVertexFrom() != n) {
                        subPath.removeLast();
                    }
                    subPath.add(n.getEdges()[TmfVertex.INV]);
                    node = n.neighbor(TmfVertex.INV);
                    continue;
                }
            }
            node = node.inh();
        }
        return subPath;
    }

}
