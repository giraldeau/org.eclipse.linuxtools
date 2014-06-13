package org.eclipse.linuxtools.tmf.analysis.graph.core.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfEdge;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfGraph;
import org.eclipse.linuxtools.tmf.analysis.graph.core.base.TmfVertex;
import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;

import com.google.common.collect.ArrayListMultimap;

/**
 * Plain old good graphviz dot file output
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
public class Dot {

	private interface LabelProvider {
		public String TmfVertexLabel(TmfVertex TmfVertex);
		public String TmfEdgeLabel(TmfVertex n0, TmfVertex n1, TmfEdge TmfEdge);
	}

	private static class VerboseLabelProvider implements LabelProvider {
		private static final String fmtTmfVertex = "    %d [ label=\"[%d,%d]\" ];\n"; 		// id, id, timestamps
		private static final String fmtTmfEdge = "    %d -> %d [ label=\"%s,%d\" ];\n"; 	// id, id, type, duration
		@Override
		public String TmfVertexLabel(TmfVertex TmfVertex) {
			return String.format(fmtTmfVertex, TmfVertex.getID(), TmfVertex.getID(), TmfVertex.getTs());
		}
		@Override
		public String TmfEdgeLabel(TmfVertex n0, TmfVertex n1, TmfEdge TmfEdge) {
			return String.format(fmtTmfEdge, n0.getID(), n1.getID(), TmfEdge.getType(), TmfEdge.getDuration());
		}
	}

	/**
	 * The verbose label provider
	 */
	public static LabelProvider verbose = new VerboseLabelProvider();

	private static class PrettyLabelProvider implements LabelProvider {
		private static final String fmtTmfVertex = "    %d [ shape=box label=\"%d\" ]; // %s\n"; 		// id, id, timestamps
		private static final String fmtTmfEdge = "    %d -> %d [ label=\" %s, %.1f \" ];\n"; 	// id, id, type, duration
		private static final String fmtTmfEdgeRelax = "    %d -> %d [ label=\" %s, %.1f \" constraint=false ];\n"; 	// id, id, type, duration
		@Override
		public String TmfVertexLabel(TmfVertex TmfVertex) {
			if (TmfVertex.numberOfNeighbor() == 0) {
                return "";
            }
			TmfTimestamp ts = new TmfTimestamp(TmfVertex.getTs(), ITmfTimestamp.NANOSECOND_SCALE);
			return String.format(fmtTmfVertex, TmfVertex.getID(), TmfVertex.getID(), ts.toString());
		}
		@Override
		public String TmfEdgeLabel(TmfVertex n0, TmfVertex n1, TmfEdge TmfEdge) {
			boolean isVertical = n0.neighbor(TmfVertex.INV) == n1 || n1.neighbor(TmfVertex.OUTV) == n1;
			String fmt = fmtTmfEdge;
			if (isVertical) {
				fmt = fmtTmfEdgeRelax;
			}
			return String.format(fmt, n0.getID(), n1.getID(), TmfEdge.getType(), TmfEdge.getDuration() / 1000000.0);
		}
	}
	/**
	 * The pretty label provider
	 */
	public static LabelProvider pretty = new PrettyLabelProvider();

	/**
	 * By default, use the verbose provider
	 */
	private static LabelProvider provider = verbose;

	/**
	 * Generate dot string from head TmfVertex
	 * @param TmfVertex the start vertex
	 * @return dot string
	 */
	public static String todot(TmfVertex TmfVertex) {
		TmfGraph g = Ops.toGraphInPlace(TmfVertex);
		return todot(g);
	}

	/**
	 * Generate dot string for the complete graph, grouped by objects
	 * @param graph graph
	 * @return dot string
	 */
	public static String todot(TmfGraph graph) {
		if (graph == null) {
            return "";
        }
		return todot(graph, graph.getNodesMap().keySet());
	}

	/**
	 * Generate dot string for provided objects
	 * @param graph the input graph
	 * @param keys set of objecs to render the graph
	 * @return dot string
	 */
	public static String todot(TmfGraph graph, Collection<? extends Object> keys) {
		if (graph == null || keys == null) {
            return "";
        }
		int i = 0;
		StringBuilder str = new StringBuilder();
		str.append("/* ");
		str.append(graph.toString());
		str.append(" */\n");
		str.append("digraph G {\n");
		//str.append("  rankdir=LR;\n");
		str.append("  overlap=false;");
		ArrayListMultimap<Object, TmfVertex> extra = ArrayListMultimap.create();
		HashSet<Object> set = new HashSet<>();
		set.addAll(keys);
		HashSet<TmfVertex> visited = new HashSet<>();
		for (Object obj : keys) {
			List<TmfVertex> list = graph.getNodesOf(obj);
			subgraph(str, obj, list, i);
			i++;
			for (TmfVertex TmfVertex: list) {
				List<TmfVertex> neighbors = visit(str, visited, TmfVertex);
				for (TmfVertex n: neighbors) {
					Object o = graph.getParentOf(n);
					if (!set.contains(o)) {
						extra.put(o, n);
					}
				}
			}
		}
		for (Object obj: extra.keySet()) {
			List<TmfVertex> list = extra.get(obj);
			subgraph(str, obj, list, i);
			i++;
		}
		str.append("}\n");
		return str.toString();
	}

	private static List<TmfVertex> visit(StringBuilder str, Set<TmfVertex> visited, TmfVertex vertex) {
		List<TmfVertex> neighbor = new ArrayList<>();
		if (visited.contains(vertex)) {
            return neighbor;
        }
		visited.add(vertex);
		for (int dir = 0; dir < vertex.getEdges().length; dir++) {
			TmfVertex n = vertex.neighbor(dir);
			if (n == null) {
                continue;
            }
			TmfEdge lnk = vertex.getEdges()[dir];
			TmfVertex n0 = vertex;
			TmfVertex n1 = n;
			if (dir == TmfVertex.INH || dir == TmfVertex.INV) {
				n0 = n;
				n1 = vertex;
			}
			if (!visited.contains(n)) {
				str.append(provider.TmfEdgeLabel(n0, n1, lnk));
				neighbor.add(n);
			}
		}
		return neighbor;
	}

	private static void subgraph(StringBuilder str, Object obj, List<TmfVertex> list, int i) {
		str.append(String.format("  subgraph \"cluster_%d\" {\n", i));
		str.append("    rank=same;\n");
		str.append(String.format(
				"    title%d [ label=\"%s\", shape=plaintext ];\n", i,
				obj.toString()));
		for (TmfVertex TmfVertex: list) {
			str.append(provider.TmfVertexLabel(TmfVertex));
		}
		str.append("}\n");
	}

	/**
	 * Write string
	 *
	 * @param folder folder
	 * @param fname name
	 * @param content content
	 */
	public static void writeString(String folder, String fname, String content) {
		File dir = new File("results", folder);
        dir.mkdirs();
        File fout = new File(dir, fname);
        try (BufferedWriter fwriter = new BufferedWriter(new FileWriter(fout))) {
            fwriter.write(content);
            fwriter.flush();
            fwriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

	/**
	 * Write string
	 *
	 * @param writer writer
	 * @param fname name
	 * @param content content
	 */
	public static void writeString(Class<? extends Object> writer, String fname, String content) {
		String folder = writer.getName();
		writeString(folder, fname, content);
	}

	/**
	 * Set label provider
	 *
	 * @param provider provider
	 */
	public static void setLabelProvider(LabelProvider provider) {
		Dot.provider = provider;
	}

}
