/*******************************************************************************
 * Copyright (c) 2014 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francis Giraldeau - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.analysis.graph.core.criticalpath;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfEdge;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfEdge.EdgeType;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfVertex;

/**
 * @author Francis Giraldeau
 */
public class CriticalPathAlgorithmUnbounded extends AbstractCriticalPathAlgorithm {

	/**
	 *
	 *
	 * @param main The execution graph on which to calculate the critical path
	 */
	public CriticalPathAlgorithmUnbounded(TmfGraph main) {
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
		while(curr.hasNeighbor(TmfVertex.OUTH)) {
			TmfVertex next = curr.neighbor(TmfVertex.OUTH);
			TmfEdge link = curr.getEdges()[TmfVertex.OUTH];
			switch(link.getType()) {
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
				List<TmfEdge> links = resolveBlockingUnbounded(link, start);
				Collections.reverse(links);
				System.out.println("links:");
				System.out.println(links);
				System.out.println(path.dump());
				stiches(path, link, links);
				System.out.println("after stiches:");
				System.out.println(path.dump());
				break;
			case EPS:
				if (link.getDuration() != 0) {
                    throw new RuntimeException("epsilon duration is not zero " + link);
                }
				break;
			case DEFAULT:
				throw new RuntimeException("Illegal link type " + link.getType());
            case UNKNOWN:
			default:
				break;
			}
			curr = next;
		}
		return path;
	}

	private void stiches(TmfGraph path, TmfEdge blocking, List<TmfEdge> links) {
		Object master = getGraph().getParentOf(blocking.getVertexFrom());
		if (links.isEmpty()) {
			path.append(master, new TmfVertex(blocking.getVertexTo()), EdgeType.UNKNOWN);
			return;
		}
		// rewind path if required
		TmfEdge first = links.get(0);
		TmfVertex anchor = path.getTail(master);
		if (first.getVertexFrom().compareTo(anchor) < 0 && anchor.hasNeighbor(TmfVertex.INH)) {
			EdgeType oldType = EdgeType.UNKNOWN;
			while ((first.getVertexFrom().compareTo(anchor) < 0) && anchor.hasNeighbor(TmfVertex.INH)) {
			    oldType = anchor.getEdges()[TmfVertex.INH].getType();
				anchor = path.removeTail(master);
			}
			anchor.getEdges()[TmfVertex.OUTH] = null;
			TmfEdge tmp = path.append(master, anchor);
			if (tmp != null) {
                tmp.setType(oldType);
            }
		}
		Object obj = getGraph().getParentOf(first.getVertexFrom());
		if (obj != master) {
			// fill any gap
			if (anchor.getTs() != first.getVertexFrom().getTs()) {
				anchor = new TmfVertex(first.getVertexFrom());
				path.append(master, anchor, EdgeType.UNKNOWN);
			}
		}
		// glue body
		TmfEdge prev = null;
		for (TmfEdge link: links) {
			// check connectivity
			if (prev != null && prev.getVertexTo() != link.getVertexFrom()) {
				anchor = copyLink(path, anchor, prev.getVertexTo(), link.getVertexFrom(),
						prev.getVertexTo().getTs(), EdgeType.DEFAULT);
			}
			anchor = copyLink(path, anchor, link.getVertexFrom(), link.getVertexTo(),
					link.getVertexTo().getTs(), link.getType());
			prev = link;
		}
	}

	private List<TmfEdge> resolveBlockingUnbounded(TmfEdge blocking, TmfVertex bound) {
		List<TmfEdge> subPath = new LinkedList<>();
		TmfVertex junction = findIncoming(blocking.getVertexTo(), TmfVertex.OUTH);
		// if wake-up source is not found, return empty list
		if (junction == null) {
			return subPath;
		}
		TmfEdge down = junction.getEdges()[TmfVertex.INV];
		subPath.add(down);
		TmfVertex node = down.getVertexFrom();
		while(node != null && node.compareTo(bound) > 0) {
			// prefer a path that converges
			if (node.hasNeighbor(TmfVertex.INV)) {
				TmfVertex conv = node.neighbor(TmfVertex.INV);
				Object parent = getGraph().getParentOf(conv);
				Object master = getGraph().getParentOf(bound);
				if (parent == master) {
					subPath.add(node.getEdges()[TmfVertex.INV]);
					break;
				}
			}
			if (node.hasNeighbor(TmfVertex.INH)) {
				TmfEdge link = node.getEdges()[TmfVertex.INH];
				if (link.getType() == EdgeType.BLOCKED) {
					subPath.addAll(resolveBlockingUnbounded(link, bound));
				} else {
					subPath.add(link);
				}
			}
			node = node.neighbor(TmfVertex.INH);
		}
		return subPath;
	}

}