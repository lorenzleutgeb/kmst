package ads1ss12.pa;

import java.util.HashSet;
import java.util.TreeMap;
import java.util.Map;

/**
 * Computes the kMST of a given graph.
 *
 * For the sake of this task, running time is decidedly more important than
 * comprehensibility. This code is ugly but fast.
 *
 * @author Lorenz Leutgeb
 */
public class KMST extends AbstractKMST {
	/** Holds the edges of the graph we're looking at. */
	private final Edge[] edges;

	/**
	 * Global buffer for when we need some memory to store a solution.
	 * Normally, this should be a local variable, but we try to optimize here.
	 */
	private final Edge[] solution;

	/**
	 * Global buffer to store a selection of nodes.
	 * Normally, this should be a local variable, but we try to optimize here.
	 */
	private boolean[] taken;

	public KMST(Integer numNodes, Integer numEdges, HashSet<Edge> edges, int k) {
		this.edges = edges.toArray(new Edge[0]);
		this.solution = new Edge[k - 1];
		this.taken = new boolean[numNodes];
	}

	@Override
	public void run() {
		// TreeMap to store Prim's results ordered by their weight.
		// TODO: Try a minimum heap with min() in O(1) instead of a
		//       balanced tree (O(logn)).
		Map<Integer, Integer> roots = new TreeMap<Integer, Integer>();
		
		Edge[] edges = new Edge[this.edges.length];
		int n, weight, relevantEdges, root, lowerBound = 0;
	
		// Sort edges by weight.
		quickSort(0, this.edges.length - 1);
	
		// Compute initial lower bound (best k - 1 edges).
		// Choosing the cheapest k - 1 edges is not very intelligent. There is no guarantee
		// that this subset of edges even induces a subgraph over the initial graph.
		// TODO: Find a better initial lower bound.
		for (int i = 0; i < solution.length; i++) {
			lowerBound += this.edges[i].weight;
		}
	
		// Iterate over all nodes in the graph and run Prim's algorithm
		// until k - 1 edges are fixed.
		// As all induced subgraphs have k nodes and are connected according to Prim, they
		// are candidate solutions and thus submitted.
		for (root = 0; root < taken.length; root++) {
			taken = new boolean[taken.length];
			System.arraycopy(this.edges, 0, edges, 0, this.edges.length);

			taken[root] = true;
			n = 0;
			weight = 0;
			relevantEdges = this.edges.length;

			while (n < solution.length) { 
				for (int i = 0; i < relevantEdges; i++) {
					// XOR to check if connected and no circle.
					if (taken[edges[i].node1] ^ taken[edges[i].node2]) {
						taken[taken[edges[i].node1] ? edges[i].node2 : edges[i].node1] = true;
						solution[n++] = edges[i];
						weight += edges[i].weight;
						System.arraycopy(edges, i + 1, edges, i, --relevantEdges - i);
						break;
					}
					// Check for circle.
					else if (taken[edges[i].node1]) {
						System.arraycopy(edges, i + 1, edges, i, --relevantEdges - i);
						break;
					}
				}
			}
			// Sum up what we've just collected and submit this
			// solution to the framework.
			HashSet<Edge> set = new HashSet<Edge>(solution.length);
			for (int i = 0; i < solution.length; i++) {
				set.add(solution[i]);
			}
			setSolution(weight, set);
			roots.put(weight, root);
		}

		// Now for the real business, let's do some Branch-and-Bound. Roots of "k Prim Spanning Trees"
		// are enumerated by weight to increase our chances to obtain the kMST earlier.
		for (int item : roots.values()) {
			taken = new boolean[taken.length];
			System.arraycopy(this.edges, 0, edges, 0, this.edges.length);
			taken[item] = true;
			branchAndBound(edges, solution.length, 0, lowerBound);
		}
	}

	/**
	 * @param edges  the set of edges of the graph, ordered by weight
	 * @param left   the number of nodes that still need to be fixed	
	 * @param weight the weight of the 
	 */
	private void branchAndBound(Edge[] edges, int left, int weight, int lowerBound) {
		Edge edge = null;
		int tmp = -1, relevantEdges = edges.length;
		for (int i = 0; i < relevantEdges; i++) {
			edge = edges[i];
			if (left == 0) {
				// No nodes are left to fix, we have reached a solution.
				HashSet<Edge> set = new HashSet<Edge>(solution.length);
				for (int j = 0; j < solution.length; j++) {
					set.add(solution[j]);
				}
				setSolution(weight, set);
				return;
			}
			else if (weight + edge.weight > getSolution().getUpperBound()) {
				// The selection of edges so far plus the weight of the next edge
				// exceeds the upper bound, so kill this branch.
				return;
			}
			else if (taken[edge.node1] ^ taken[edge.node2]) {
				// Choosing this edge does not break requirements of a tree.

				// We choose this edge.
				solution[solution.length - left] = edge;

				// Find out which node was selected and indicate this in taken.
				taken[(tmp = taken[edge.node1] ? edge.node2 : edge.node1)] = true;
				
				// Remove edge for deeper recursions.
				System.arraycopy(edges, i + 1, edges, i, relevantEdges-- - i-- - 1);
				Edge[] copy = new Edge[relevantEdges];
				System.arraycopy(edges, 0, copy, 0, relevantEdges);
				
				// Recursive call.
				branchAndBound(copy, left - 1, weight + edge.weight, lowerBound);

				// Now unselect the node again.
				taken[tmp] = false;

				if (i + left > relevantEdges) {
					// Not enough edges left.
					return;
				}
				else if ((lowerBound += (i < left - 1 ? edges[left - 1].weight : edges[i + 1].weight) - edge.weight) >= getSolution().getUpperBound()) {
					// This branch will never reach a better solution than we've already found, because
					// the lower bound minus the weight of the edge we selected plus the weight of the
					// next best edge exceeds the upper bound.
					return;
				}
			}
			else if (taken[edge.node1]) {
				System.arraycopy(edges, i + 1, edges, i, relevantEdges-- - i-- - 1);
				
				if (i + left > relevantEdges) {
					return;
				}
				else if ((lowerBound += (i < left - 1 ? edges[left - 1].weight : edges[i + 1].weight) - edge.weight) >= getSolution().getUpperBound()) {
					return;
				}
			}
			else {
				if (i > left && i + 1 < relevantEdges) {
					// This edge is skipped.
					lowerBound -= edge.weight - edges[i + 1].weight;
				}
				if (lowerBound >= getSolution().getUpperBound()) {
					return;
				}
			}
		}
	}

	private void quickSort(int left, int right) {
		Edge tmp;

		if (right - left < 5) {
			for (int p = left + 1; p <= right; p++) {
				tmp = edges[p];
				int j;

				for (j = p; j > left && tmp.compareTo(edges[j - 1]) < 0; j--)
					edges[j] = edges[j - 1];

				edges[j] = tmp;
			}
			return;
		}

		int middle = (left + right) / 2;
		
		if (edges[middle].compareTo(edges[left]) < 0) {
			tmp = edges[middle];
			edges[middle] = edges[left];
			edges[left] = tmp;
		}
		if (edges[right].compareTo(edges[left]) < 0) {
			tmp = edges[right];
			edges[right] = edges[left];
			edges[left] = tmp;
		}
		if (edges[right].compareTo(edges[middle]) < 0) {
			tmp = edges[right];
			edges[right] = edges[middle];
			edges[middle] = tmp;
		}

		tmp = edges[middle];
		edges[middle] = edges[right - 1];
		edges[right - 1] = tmp;
		
		Edge p = edges[right - 1];

		int i, j;
		for (i = left, j = right - 1;;) {
			while (edges[++i].compareTo(p) < 0);
			while (p.compareTo(edges[--j]) < 0);
			if (i >= j) break;

			tmp = edges[i];
			edges[i] = edges[j];
			edges[j] = tmp;
		}
		
		tmp = edges[left];
		edges[left] = edges[i - 1];
		edges[i - 1] = tmp;
		
		quickSort(left, i - 1);
		quickSort(j + 1, right);
	}
}
