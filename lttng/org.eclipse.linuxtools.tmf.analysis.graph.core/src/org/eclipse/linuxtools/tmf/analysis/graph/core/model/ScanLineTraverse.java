package org.eclipse.linuxtools.tmf.analysis.graph.core.model;

import java.util.HashSet;
import java.util.Stack;

import org.eclipse.linuxtools.tmf.analysis.graph.core.base.ITmfGraphVisitor;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfVertex;

public class ScanLineTraverse {
    public static void traverse(TmfVertex start, ITmfGraphVisitor visitor) {
        if (start == null) {
            return;
        }
        Stack<TmfVertex> stack = new Stack<>();
        HashSet<TmfVertex> visited = new HashSet<>();
        stack.add(start);
        while(!stack.isEmpty()) {
            TmfVertex curr = stack.pop();
            if (visited.contains(curr)) {
                continue;
            }
            //  process one line
            TmfVertex n = Ops.head(curr);
            visitor.visitHead(n);
            while(n != null) {
                visitor.visit(n);
                // Only visit links up-right, guarantee to visit once only
                if (n.outv() != null) {
                    stack.push(n.outv());
                    visitor.visit(n.getEdges()[TmfVertex.OUTV], false);
                }
                if (n.inv() != null) {
                    stack.push(n.inv());
                }
                if (n.outh() != null) {
                    visitor.visit(n.getEdges()[TmfVertex.OUTH], true);
                }
                visited.add(n);
                n = n.outh();
            }
        }
    }
}