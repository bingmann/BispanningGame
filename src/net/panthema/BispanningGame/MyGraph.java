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
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

import edu.uci.ics.jung.graph.SparseGraph;

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
    boolean inCircle;

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
class MyGraph extends SparseGraph<Number, MyEdge>
{
    private static final long serialVersionUID = -6036820402858303673L;

    /** Message to post render onto graph */
    public String message;

    /** Generate a simple random bispanning graph with numVertex nodes */
    static MyGraph getRandomGraph(int numVertex) {
        while (true) {
            // generate a random graph with numVertex vertexes
            MyGraph g = new MyGraph();

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

    /** Test if e0 closes a cycle of the same color in the graph */
    boolean testCycle(MyEdge e0) {
        Number e0_x = getEndpoints(e0).getFirst();
        Number e0_y = getEndpoints(e0).getSecond();

        Queue<Number> queue = new ArrayDeque<Number>();

        // initialize queue with node e0
        queue.add(e0_x);

        Map<Number, Number> pred = new TreeMap<Number, Number>();
        pred.put(e0_x, e0_x);

        // Breadth first search
        while (!queue.isEmpty()) {
            Number v = queue.poll();

            for (MyEdge ei : getIncidentEdges(v)) {
                if (ei.color != e0.color)
                    continue;
                if (ei == e0)
                    continue;

                Number w = getOpposite(v, ei);

                if (pred.get(w) != null) // vertex already seen
                    continue;

                queue.add(w);
                pred.put(w, v);
            }
        }

        return (pred.get(e0_y) != null);
    }

    /**
     * Test if e0 closes a cycle of the same color in the graph. If a cycle is
     * found, then mark edges of cycle with inCircle.
     */
    boolean markCycle(MyEdge e0) {
        for (MyEdge ei : getEdges())
            ei.inCircle = false;

        Number e0_x = getEndpoints(e0).getFirst();
        Number e0_y = getEndpoints(e0).getSecond();

        Queue<Number> queue = new ArrayDeque<Number>();

        // initialize queue with node e0
        queue.add(e0_x);

        Map<Number, Number> pred = new TreeMap<Number, Number>();
        pred.put(e0_x, e0_x);

        // Breadth first search
        while (!queue.isEmpty()) {
            Number v = queue.poll();

            for (MyEdge ei : getIncidentEdges(v)) {
                if (ei.color != e0.color)
                    continue;
                if (ei == e0)
                    continue;

                Number w = getOpposite(v, ei);

                if (pred.get(w) != null) // vertex already seen
                    continue;

                queue.add(w);
                pred.put(w, v);
            }
        }

        if (pred.get(e0_y) != null) {
            // System.out.println("Found cycle");
            Number y = e0_y;
            while (pred.get(y) != y) {
                MyEdge e = findEdge(y, pred.get(y));
                assert (e != null);
                e.inCircle = true;
                y = getOpposite(y, e);
            }
            e0.inCircle = true;
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Test if e0 closes a cycle of the same color, if it does, mark the cycle
     * and calculate which of the cycle edges break the cycle correctly.
     */
    boolean markCycleFixes(MyEdge e0) {
        for (MyEdge ei : getEdges())
            ei.isFix = false;

        if (!markCycle(e0))
            return false;

        for (MyEdge ei : getEdges()) {
            if (!ei.inCircle)
                continue;

            ei.flipColor();
            ei.isFix = !testCycle(ei);
            ei.flipColor();
        }

        return markCycle(e0);
    }

    /** Return true if the vertex v is a leaf in the color tree */
    boolean isTreeLeaf(Number v, int color) {
        int count = 0;

        for (MyEdge e : getIncidentEdges(v)) {
            if (e.color == color)
                count++;
        }

        return (count == 1);
    }

    /** Test if the edge e0 leads to a unique exchange */
    boolean testUniqueExchange(MyEdge e0) {
        if (markCycle(e0))
            throw new RuntimeException("Bad!");

        e0.flipColor();
        if (!markCycle(e0))
            throw new RuntimeException("Bad!");

        int excount = 0;

        for (MyEdge ei : getEdges()) {
            if (!ei.inCircle)
                continue;
            if (ei == e0)
                continue;

            ei.flipColor();
            if (!testCycle(ei)) {
                excount++;
            }
            ei.flipColor();
        }

        e0.flipColor();

        return (excount == 1);
    }

    /** Calculate edges of graph which lead to unique exchanges. */
    @SuppressWarnings("unused")
    void calcUniqueExchanges() {
        for (MyEdge ei : getEdges()) {
            ei.isUE = testUniqueExchange(ei);

            if (false) {
                Number x = getEndpoints(ei).getFirst();
                Number y = getEndpoints(ei).getSecond();

                if (isTreeLeaf(x, 1) || isTreeLeaf(y, 1) || isTreeLeaf(x, 2) || isTreeLeaf(y, 2))
                    ei.isUE = false;
            }
        }

        for (MyEdge ei : getEdges()) {
            ei.inCircle = false;
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
        }
        else {
            message = "Graph is not bispanning!";
        }
    }
}
