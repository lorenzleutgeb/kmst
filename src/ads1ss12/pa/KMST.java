package ads1ss12.pa;

import java.util.HashSet;

/** @author Lorenz Leutgeb */
public class KMST extends AbstractKMST {
	private final Edge[] edges;
	private final Edge[] solution;
	private boolean[] taken;

	public KMST(Integer numNodes, Integer numEdges, HashSet<Edge> edges, int k) {
		this.edges = edges.toArray(new Edge[0]);
		this.solution = new Edge[k - 1];
		this.taken = new boolean[numNodes];
	}

	@Override public void run() {
		// AVL-Tree to store Prim's results
		// TODO: try a minimum heap with min() in O(1) instead of a
		//       balanced tree (O(logn))
		AvlTree roots = new AvlTree();
		
		Edge[] edges = new Edge[this.edges.length];
		int n, weight, relevantEdges, root, lowerBound = 0;;
	
		// sort edges by weight
		quickSort(0, this.edges.length - 1);
	
		// compute initial lower bound (best k - 1 edges)
		for (int i = 0; i < solution.length; i++) {
			lowerBound += this.edges[i].weight;
		}
	
		// iterate over all nodes in the graph and run Prim's algorithm
		// until k - 1 edges are fixed
		for (root = 0; root < taken.length; root++) {
			taken = new boolean[taken.length];
			System.arraycopy(this.edges, 0, edges, 0, this.edges.length);

			taken[root] = true;
			n = 0;
			weight = 0;
			relevantEdges = this.edges.length;

			while (n < solution.length) { 
				for (int i = 0; i < relevantEdges; i++) {
					// XOR to check if connected & no circle
					if (taken[edges[i].node1] ^ taken[edges[i].node2]) {
						taken[taken[edges[i].node1] ? edges[i].node2 : edges[i].node1] = true;
						solution[n++] = edges[i];
						weight += edges[i].weight;
						System.arraycopy(edges, i + 1, edges, i, --relevantEdges - i);
						break;
					}
					// check for circle
					else if (taken[edges[i].node1]) {
						System.arraycopy(edges, i + 1, edges, i, --relevantEdges - i);
						break;
					}
				}
			}
			// sum up what we've just collected and submit this
			// solution to the framework
			HashSet<Edge> set = new HashSet<Edge>(solution.length);
			for (int i = 0; i < solution.length; i++) {
				set.add(solution[i]);
			}
			setSolution(weight, set);
			roots.insert(weight, root);
		}
		// now for the real business, let's do some Branch-and-Bound
		while ((root = roots.poll()) != Integer.MIN_VALUE) {
			taken = new boolean[taken.length];
			System.arraycopy(this.edges, 0, edges, 0, this.edges.length);
			taken[root] = true;
			branchAndBound(edges, solution.length, 0, lowerBound);
		}
	}

	private void branchAndBound(Edge[] edges, int left, int weight, int lowerBound) {
		Edge edge = null;
		int tmp = -1, relevantEdges = edges.length;
		for (int i = 0; i < relevantEdges; i++) {
			edge = edges[i];
			if (left == 0) { // finished!
				HashSet<Edge> set = new HashSet<Edge>(solution.length);
				for (int j = 0; j < solution.length; j++) {
					set.add(solution[j]);
				}
				setSolution(weight, set);
				return;
			}
			else if (weight + edge.weight > getSolution().getUpperBound()) {
				// solution is too expensive, kill this branch
				return;
			}
			else if (taken[edge.node1] ^ taken[edge.node2]) {
				solution[solution.length - left] = edge;
				taken[(tmp = taken[edge.node1] ? edge.node2 : edge.node1)] = true;
				
				// remove edge for deeper recursions
				System.arraycopy(edges, i + 1, edges, i, relevantEdges-- - i-- - 1);
				Edge[] copy = new Edge[relevantEdges];
				System.arraycopy(edges, 0, copy, 0, relevantEdges);
				
				// recursive call
				branchAndBound(copy, left - 1, weight + edge.weight, lowerBound);
				
				taken[tmp] = false;
				
				if (i + left > relevantEdges) {
					// not enough edges left
					return;
				}
				else if ((lowerBound += (i < left - 1 ? edges[left - 1].weight : edges[i + 1].weight) - edge.weight) >= getSolution().getUpperBound()) {
					// this branch will never reach a better solution than we've already found
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
	
	private static class AvlTree {
		private static class AvlNode {
			public AvlNode left;
			public AvlNode right;
			public AvlNode parent;

			public int key;
			public int balance;
			public int value;

			public AvlNode(int k, int v) {
				left = right = parent = null;
				balance = 0;
				key = k;
				value = v;
			}

			@Override public String toString() {
				return "[ " + key + "(" + value + ") ]";
			}

		}
		
		private AvlNode root;
		
		public void insert(int k, int v) {
			if (root == null) {
				root = new AvlNode(k, v);
				return;
			}

			AvlNode n = this.root;

			while (true) {
				if (k < n.key) {
					if (n.left == null) {
						n.left = new AvlNode(k, v);
						n.left.parent = n;
						break;
					}
					else {
						n = n.left;
					}
				}
				else if (k > n.key) {
					if (n.right == null) {
						n.right = new AvlNode(k, v);
						n.right.parent = n;
						break;
					}
					else {
						n = n.right;
					}
				}
				else {
					return;
				}
			}

			balanceToRoot(n);
		}

		public AvlNode rotateLeft(AvlNode n) {
			AvlNode m = n.right;
			n.right = m.left;

			if (n.right != null)
				n.right.parent = n;

			m.left = n;
			n.parent = m;

			n.balance = Math.max(height(n.left), height(n.right)) + 1;
			m.balance = Math.max(height(m.left), height(m.right)) + 1;

			return m;
		}

		public AvlNode rotateRight(AvlNode n) {
			AvlNode m = n.left;
			n.left = m.right;

			if (n.left != null)
				n.left.parent = n;

			m.right = n;
			n.parent = m;

			n.balance = Math.max(height(n.left), height(n.right)) + 1;
			m.balance = Math.max(height(m.left), height(m.right)) + 1;

			return m;
		}

		public AvlNode doubleLeft(AvlNode n) {
			AvlNode m = rotateRight(n.right);
			m.parent = n;
			n.right = m;
			return rotateLeft(n);
		}

		public AvlNode doubleRight(AvlNode n) {
			AvlNode m = rotateLeft(n.left);
			m.parent = n;
			n.left = m;
			return rotateRight(n);
		}

		private void balanceToRoot(AvlNode n) {
			if (n == null)
				return;

			AvlNode parent;
			while (true) {
				parent = n.parent;
				n = balance(n);
				n.parent = parent;

				if (parent == null) {
					root = n;
					break;
				}

				if (n.key > parent.key)
					parent.right = n;
				else
					parent.left = n;

				n = parent;
			}
		}

		private AvlNode balance(AvlNode n) {
			if (height(n.right) - height(n.left) == -2) {
				if (height(n.left.left) >= height(n.left.right))
					n = rotateRight(n);
				else
					n = doubleRight(n);
			}
			else if (height(n.right) - height(n.left) == 2)
				if (height(n.right.right) >= height(n.right.left))
					n = rotateLeft(n);
				else
					n = doubleLeft(n);

			n.balance = Math.max(height(n.left), height(n.right)) + 1;
			return n;
		}

		private int height(AvlNode n) {
			if (n == null)
				return -1;
			else
				return n.balance;
		}

		public boolean isEmpty() {
			return root == null;
		}
		
		public int poll() {
			if (root == null)
				return Integer.MIN_VALUE;

			AvlNode remove = root;

			while (remove.left != null) remove = remove.left;

			AvlNode m = remove.right;
			
			if (m != null) {
				m.parent = remove.parent;
			}
			if (remove.parent == null) {
				root = m;
			}
			else {
				if (remove == remove.parent.left) {
					remove.parent.left = m;
				}
				else {
					remove.parent.right = m;
				}
			}
			balanceToRoot(remove.parent);
			return remove.value;
		}
	}
}
