/*******************************************************************************
 * src/net/panthema/BispanningGame/MyGraph.java
 *
 * Derive from SparseGraph a MyGraph class with additional algorithms needed
 * for calculating unique exchanges.
 *
 *******************************************************************************
 * Copyright (C) 2014 Timo Bingmann <tb@panthema.net>
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package net.panthema.BispanningGame;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.graph.SparseMultigraph;

/**
 * Edge Data Content
 *
 * @author Timo Bingmann
 */
class MyEdge implements Comparable<MyEdge>
{
    /** id of the edge, mainly for labeling */
    int id;

    /** color: 0=black, 1=red, 2=blue */
    int color;

    /** original color: 0=black, 1=red, 2=blue */
    int origColor;

    /** boolean if edge leads to a unique exchange */
    boolean isUE;

    /** if a cycle exists, then cycle edges are marked */
    boolean inCycle;

    /** if a cut exists, then cut edges are marked */
    boolean inCut;

    /** if a cycle exists, then cycle breaker edges are marked */
    boolean isFix;

    /** require an id to construct */
    public MyEdge(int id) {
        this.id = id;
        this.color = 0;
        this.origColor = 0;
    }

    /** Construct edge label */
    public String toString() {
        return "e" + id;
    }

    /** Flip the color, if edge is colored */
    public void flipColor() {
        if (color == 1)
            color = 2;
        else if (color == 2)
            color = 1;
    }

    /** Required to be Comparable */
    public int compareTo(MyEdge o) {
        return id - o.id;
    }
}

/**
 * Graph class with custom edge data and various other graph algorithms needed
 * for graph exchange calculations.
 *
 * @author Timo Bingmann
 */
class MyGraph extends SparseMultigraph<Integer, MyEdge>
{
    private static final long serialVersionUID = -6036820402858303673L;

    /** Message to post render onto graph */
    public String message;

    /** Initial layout (loaded from GraphString) */
    public StaticLayout<Integer, MyEdge> mInitialLayout;

    /** Generate a simple random bispanning graph with numVertex nodes */
    static MyGraph getRandomGraph(int numVertex) {
        while (true) {
            // generate a random graph with numVertex vertexes
            MyGraph g = new MyGraph();

            if (numVertex == 0)
                return g;

            for (int i = 0; i < numVertex; ++i)
                g.addVertex(i);

            for (int i = 0; i < 2 * numVertex - 2; ++i) {
                int x = (int) (Math.random() * numVertex);
                int y = (int) (Math.random() * numVertex);

                if (g.findEdge(x, y) != null)
                    --i;
                else
                    g.addEdge(new MyEdge(i), x, y);
            }

            // test if generated graph is a bispanning graph
            AlgBispanning ab = new AlgBispanning(g);
            if (ab.isOkay())
                return g;
        }
    }

    /**
     * Calculate an edge path from one edge of e0 to the other. If sameColor =
     * false, then the a path (cycle minus e0) of the other color is calculated.
     * If sameColor = true, then a true cycle of the color of e0 is calculated.
     */
    List<MyEdge> calcCycle(MyEdge e0, boolean sameColor) {
        Integer e0_x = getEndpoints(e0).getFirst();
        Integer e0_y = getEndpoints(e0).getSecond();

        Queue<Integer> queue = new ArrayDeque<Integer>();

        // initialize queue with node e0_x
        queue.add(e0_x);

        // predecessor map to calculate path
        Map<Integer, MyEdge> pred = new HashMap<Integer, MyEdge>();

        // Breadth first search
        while (!queue.isEmpty()) {
            Integer v = queue.poll();

            for (MyEdge ei : getIncidentEdges(v)) {
                // skip edges with same color?
                if (!sameColor && ei.color == e0.color)
                    continue;
                // skip edges with other color? or == e0
                if (sameColor && (ei.color != e0.color || ei == e0))
                    continue;

                Integer w = getOpposite(v, ei);

                if (pred.get(w) != null) // vertex already seen
                    continue;

                queue.add(w);
                pred.put(w, ei);
            }
        }

        if (pred.get(e0_y) == null) // BFS did not reach other end of e0
            return null;

        // follow predecessor links back to other end
        List<MyEdge> path = new ArrayList<MyEdge>();

        Integer v = e0_y;
        while (v != e0_x) {
            MyEdge p = pred.get(v);
            path.add(p);
            v = getOpposite(v, p);
        }

        if (sameColor)
            path.add(e0);

        return path;
    }

    /** Calculate the edge cut of e0 which has the other color. */
    Set<MyEdge> calcCut(MyEdge e0, boolean sameColor) {
        Integer e0_x = getEndpoints(e0).getFirst();
        Integer e0_y = getEndpoints(e0).getSecond();

        // map containing mark of side of the cut for each vertex
        Map<Integer, Integer> mark = new HashMap<Integer, Integer>();

        // initialize BFS queue with first end of e0
        Queue<Integer> queue = new ArrayDeque<Integer>();
        queue.add(e0_x);
        mark.put(e0_x, 1);

        // First breadth search on e0_x's side
        while (!queue.isEmpty()) {
            Integer v = queue.poll();

            for (MyEdge ei : getIncidentEdges(v)) {
                // skip edges with e0's color?
                if (sameColor && ei.color == e0.color)
                    continue;
                // skip edges with other color? or == e0
                if (!sameColor && (ei.color != e0.color || ei == e0))
                    continue;

                Integer w = getOpposite(v, ei);

                if (mark.containsKey(w)) // vertex already seen
                    continue;

                queue.add(w);
                mark.put(w, 1);
            }
        }

        // if other end was marked: there is a path from e0_x to e0_y -> no cut.
        if (mark.containsKey(e0_y))
            return null;

        // initialize BFS queue with other end of e0
        queue.add(e0_y);
        mark.put(e0_y, 2);

        Set<MyEdge> cut = new HashSet<MyEdge>();

        // Second breadth search on e0_y's side
        while (!queue.isEmpty()) {
            Integer v = queue.poll();

            for (MyEdge ei : getIncidentEdges(v)) {
                Integer w = getOpposite(v, ei);

                // skip edges with e0's color or other color?
                if (sameColor == (ei.color == e0.color)) {
                    // check if cut edge
                    if (mark.containsKey(w) && mark.get(w) == 1)
                        cut.add(ei);
                }
                else { // other color
                    if (!mark.containsKey(w)) {
                        // vertex not seen
                        queue.add(w);
                        mark.put(w, 2);
                    }
                }
            }
        }

        return cut;
    }

    /** Test if e0 is contained in a cycle of the same color in the graph */
    boolean testCycle(MyEdge e0) {
        Integer e0_x = getEndpoints(e0).getFirst();
        Integer e0_y = getEndpoints(e0).getSecond();

        Queue<Integer> queue = new ArrayDeque<Integer>();

        // initialize queue with node e0
        queue.add(e0_x);

        Set<Integer> seen = new HashSet<Integer>();

        // Breadth first search
        while (!queue.isEmpty()) {
            Integer v = queue.poll();

            for (MyEdge ei : getIncidentEdges(v)) {
                if (ei.color != e0.color) // skip other colors
                    continue;
                if (ei == e0) // skip the seed edge
                    continue;

                Integer w = getOpposite(v, ei);

                if (seen.contains(w)) // vertex already seen
                    continue;

                queue.add(w);
                seen.add(w);
            }
        }

        return seen.contains(e0_y);
    }

    /**
     * Test if e0 closes a cycle of the same color in the graph. If a cycle is
     * found, then mark edges of cycle with inCircle.
     */
    boolean markCycle(MyEdge e0) {
        for (MyEdge ei : getEdges())
            ei.inCycle = false;

        // find cycle in graph with color of e0
        List<MyEdge> cycle = calcCycle(e0, true);

        if (cycle == null)
            return false;

        // mark cycle edges
        for (MyEdge ei : cycle)
            ei.inCycle = true;

        return true;
    }

    /**
     * Test if e0 closes a cycle of the same color, if it does, mark the cycle
     * and calculate which of the cycle edges break the cycle correctly.
     */
    boolean markCycleFixes(MyEdge e0) {
        for (MyEdge ei : getEdges()) {
            ei.isFix = false;
            ei.inCycle = false;
            ei.inCut = false;
        }

        // find cycle in graph with color of e0
        List<MyEdge> cycle = calcCycle(e0, true);
        // System.out.println("cycle[" + e0 + "]: " + cycle);

        if (cycle == null)
            return false;

        // mark cycle edges
        for (MyEdge ei : cycle)
            ei.inCycle = true;

        // find cut in graph with color of e0
        Set<MyEdge> cut = calcCut(e0, true);
        // System.out.println("cut  [" + e0 + "]: " + cut);

        if (cut == null)
            return false;

        // mark cut edges and intersection
        for (MyEdge ei : cut) {
            ei.inCut = true;
            ei.isFix = ei.inCycle;
        }

        return true;
    }

    /** Return true if the vertex v is a leaf in the color tree */
    boolean isTreeLeaf(Integer v, int color) {
        int count = 0;

        for (MyEdge e : getIncidentEdges(v)) {
            if (e.color == color)
                count++;
        }

        return (count == 1);
    }

    /** Test if the edge e0 leads to a unique exchange */
    @SuppressWarnings("unused")
    boolean testUniqueExchange(MyEdge e0) {
        if (testCycle(e0))
            return false; // Bad throw new RuntimeException("Bad!");

        List<MyEdge> cycle = calcCycle(e0, false);
        if (cycle == null)
            throw new RuntimeException("UEtest: edge is in a cycle!");

        Set<MyEdge> cut = calcCut(e0, false);
        if (cut == null)
            throw new RuntimeException("UEtest: edge induces no cut!");

        Set<MyEdge> intersection = new HashSet<MyEdge>(cycle);
        intersection.retainAll(cut);

        if (false) {
            System.out.println("cycle[" + e0 + "]: " + cycle);
            System.out.println("cut  [" + e0 + "]: " + cut);
            System.out.println("inter[" + e0 + "]: " + intersection);
        }

        return intersection.size() == 1;
    }

    /** Calculate edges of graph which lead to unique exchanges. */
    @SuppressWarnings("unused")
    void calcUniqueExchanges() {

        for (MyEdge ei : getEdges()) {
            ei.isUE = testUniqueExchange(ei);

            if (false) {
                Integer x = getEndpoints(ei).getFirst();
                Integer y = getEndpoints(ei).getSecond();

                if (isTreeLeaf(x, 1) || isTreeLeaf(y, 1) || isTreeLeaf(x, 2) || isTreeLeaf(y, 2))
                    ei.isUE = false;
            }
        }

        for (MyEdge ei : getEdges()) {
            ei.inCycle = false;
            ei.inCut = false;
            ei.isFix = false;
        }
    }

    /** Update original color fields from current color */
    void updateOriginalColor() {
        for (MyEdge ei : getEdges()) {
            ei.origColor = ei.color;
        }
    }

    /** Called by EditingGraphMousePlugin when the graph changed. */
    void graphChanged() {
        System.out.println("Graph changed!");

        AlgBispanning ab = new AlgBispanning(this);
        if (ab.isOkay()) {
            message = "";
            calcUniqueExchanges();
            System.out.println("Graph is bispanning!");

            if (getVertexCount() <= 14) {
                if (isAtomicBispanner()) {
                    message = "atomic";
                }
                else {
                    message = "composite";
                }
            }
        }
        else {
            message = "Graph is not bispanning!";
            System.out.println(message);
        }
    }

    /** Return maximum vertex id */
    int getMaxVertexId() {
        int vertexMax = 0;

        for (Integer v : getVertices()) {
            if (vertexMax < v)
                vertexMax = v;
        }
        return vertexMax;
    }

    /** Return maximum edge.id */
    int getMaxEdgeId() {
        int edgeMax = 0;

        for (MyEdge e : getEdges()) {
            if (edgeMax < e.id)
                edgeMax = e.id;
        }
        return edgeMax;
    }

    /** Increment binary number stored in the vector v */
    static boolean increment_int_vector(int[] v) {
        for (int i = 0; i < v.length; ++i) {
            if (v[i] == -1)
                continue;

            if (v[i] == 0) {
                v[i] = 1;
                return true;
            }
            else {
                v[i] = 0;
            }
        }
        return false;
    }

    /**
     * Tests if the graph is an atomic bispanner, after Nash-Williams' criterion
     */
    boolean isAtomicBispanner() {

        final int maxVertex = getMaxVertexId();
        final int numVertex = getVertexCount();

        // make a mapping V -> {0,...,n-1}
        final int[] vmap = new int[maxVertex + 1];

        int k = 0;
        for (Integer v : getVertices())
            vmap[v] = k++;
        assert (k == numVertex);

        // iterate over all possible vertex set partitions
        EnumerateSetPartitions en = new EnumerateSetPartitions(numVertex);

        return en.enumerate(new SetPartitionFunctor() {
            public boolean partition(final int[] setp) {
                // calculate number of set partitions
                int npart = 0;
                for (int i = 0; i < setp.length; ++i) {
                    if (npart < setp[i] + 1)
                        npart = setp[i] + 1;
                }

                // skip trivial partitions
                if (npart == 1 || npart == numVertex)
                    return true;

                // count number of edges crossing partition members
                int cross = 0;

                for (MyEdge e : getEdges()) {
                    Integer v = getEndpoints(e).getFirst();
                    Integer w = getEndpoints(e).getSecond();

                    if (setp[vmap[v]] != setp[vmap[w]])
                        ++cross;
                }

                if (cross == 2 * (npart - 1)) // composite
                    return false;

                return true;
            }
        });
    }

    /** Tests if the graph is an atomic bispanner. */
    boolean isAtomicBispannerTutte() {

        int maxEdge = getMaxEdgeId();

        int[] subset = new int[maxEdge + 1];

        // mark all existing edges as 0
        for (int i = 0; i < subset.length; ++i)
            subset[i] = -1;

        for (MyEdge e : getEdges())
            subset[e.id] = 0;

        // iterate over all possible cut sets
        while (increment_int_vector(subset)) {

            // copy graph, excluding all edges in cut set
            MyGraph g = new MyGraph();

            for (Integer v : getVertices()) {
                g.addVertex(v);
            }

            int cutsize = 0;

            for (MyEdge e : getEdges()) {

                if (subset[e.id] == -1)
                    continue;

                if (subset[e.id] != 0) {
                    cutsize++;
                    continue;
                }

                Integer x = getEndpoints(e).getFirst();
                Integer y = getEndpoints(e).getSecond();

                g.addEdge(e, x, y);
            }

            if (cutsize == getEdgeCount())
                continue;

            int comp = g.countComponents();

            // System.out.println(Arrays.toString(subset) + " - " + (2 * (comp -
            // 1)) + " - " + cutsize);

            if (2 * (comp - 1) == cutsize)
                return false;
        }

        return true;
    }

    /** Count the number of components in the graph */
    int countComponents() {

        // predecessor vertex in BFS tree
        int[] pred = new int[getMaxVertexId() + 1];
        for (int i = 0; i < pred.length; ++i)
            pred[i] = i;

        int numComponents = 0;

        // BFS queue
        ArrayDeque<Integer> queue = new ArrayDeque<Integer>();

        // iterate over all vertices as roots
        for (Integer r : getVertices()) {

            if (pred[r] != r)
                continue;

            ++numComponents; // new root, new component

            queue.addLast(r);

            while (!queue.isEmpty()) {

                Integer v = queue.pollFirst();

                // visit all neighbors of v
                for (MyEdge ev : getIncidentEdges(v)) {

                    Integer w = getOpposite(v, ev);

                    if (pred[w] != w || w == r) // already seen
                        continue;

                    queue.addLast(w);
                    pred[w] = v;
                }
            }
        }

        return numComponents;
    }
}
