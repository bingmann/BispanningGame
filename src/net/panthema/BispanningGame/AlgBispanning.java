/*******************************************************************************
 * src/net/panthema/BispanningGame/AlgBispanning.java
 *
 * Algorithm to construct two spanning trees in a bispanning graph.
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
import java.util.Stack;
import java.util.TreeMap;

/**
 * Algorithm to construct two spanning trees in a bispanning graph.
 * 
 * @author Timo Bingmann
 */
public class AlgBispanning
{
    /** Simple debug output */
    @SuppressWarnings("unused")
    private void debug(String str) {
        if (false)
            System.out.println(str);
    }

    /** The processed graph */
    private MyGraph mGraph;

    /** Number of edges in trees */
    private int mCount0, mCount1, mCount2;

    /** Three union-find data structures */
    private UnionFind mUnion1, mUnion2;

    /** Predecessor vertex in BFS tree of both colors */
    private Map<Integer, MyEdge> mPred1, mPred2;

    /** Calculate BFS tree from root with given color */
    private void bfs_tree(int color, Integer root) {

        Map<Integer, MyEdge> pred = (color == 1) ? mPred1 : mPred2;
        pred.clear();

        // BFS queue
        Queue<Integer> queue = new ArrayDeque<Integer>();

        // Initialize queue with node root
        queue.add(root);
        pred.put(root, new MyEdge(0));

        // Breadth first search
        while (!queue.isEmpty()) {

            Integer v = queue.poll();

            for (MyEdge ei : mGraph.getIncidentEdges(v)) {
                if (ei.color != color)
                    continue;

                Integer w = mGraph.getOpposite(v, ei);

                if (pred.get(w) != null) // vertex already seen
                    continue;

                queue.add(w);
                pred.put(w, ei);
            }
        }
    }

    private boolean bfs_augmenting_path(MyEdge e0) {

        // BFS queue for edges
        Queue<MyEdge> queue = new ArrayDeque<MyEdge>();

        // initialize queue with node e0
        queue.add(e0);

        Integer e0_x = mGraph.getEndpoints(e0).getFirst();

        // erase labels
        Map<MyEdge, MyEdge> label = new TreeMap<MyEdge, MyEdge>();

        while (!queue.isEmpty()) {
            MyEdge e = queue.poll();

            int ti = (e.color % 2) + 1; // other tree

            Map<Integer, MyEdge> pred = (ti == 1) ? mPred1 : mPred2;
            UnionFind myunion = (ti == 1) ? mUnion1 : mUnion2;

            Integer e_v = mGraph.getEndpoints(e).getFirst();
            Integer e_w = mGraph.getEndpoints(e).getSecond();

            debug("Visiting " + e + " with color " + ti + "!");
            debug("Ends of " + e + ": " + myunion.find(e_v) + " - " + myunion.find(e_w));

            if (myunion.find(e_v) != myunion.find(e_w)) {
                debug("Augmenting sequence!");

                myunion.union(e_v, e_w);

                if (ti == 1)
                    mCount1++;
                if (ti == 2)
                    mCount2++;

                while (label.get(e) != null) {
                    int tmp = e.color;
                    e.color = ti;
                    ti = tmp;

                    debug("colored " + e + " with " + e.color);
                    e = label.get(e);
                }

                e.color = ti;
                debug("colored final " + e + " with " + e.color);

                mCount0--;
                return true;
            }

            // pick the vertex u which is not the BFS root, and walk upwards to
            // find a part of the cycle
            Integer e_u;
            if (e_v != e0_x && label.get(pred.get(e_v)) == null)
                e_u = e_v;
            else if (e_w != e0_x && label.get(pred.get(e_w)) == null)
                e_u = e_w;
            else {
                debug("Both ends of edge already in label tree.");
                continue;
            }

            Stack<MyEdge> predpath = new Stack<MyEdge>();

            while (e_u != e0_x && label.get(pred.get(e_u)) == null) {
                MyEdge en = pred.get(e_u);

                debug("push (e_u,pred[e_u]) = (" + e_u + "," + pred.get(e_u) + ") = " + en + " onto stack.");
                predpath.add(en);

                e_u = mGraph.getOpposite(e_u, pred.get(e_u));
            }

            while (!predpath.empty()) {
                MyEdge e_prime = predpath.pop();
                label.put(e_prime, e);
                queue.add(e_prime);
            }
        }

        return false;
    }

    public AlgBispanning(MyGraph aGraph) {
        mGraph = aGraph;

        mCount0 = mCount1 = mCount2 = 0;

        int vertexMax = mGraph.getMaxVertexId();

        mUnion1 = new UnionFind(vertexMax + 1);
        mUnion2 = new UnionFind(vertexMax + 1);

        mPred1 = new TreeMap<Integer, MyEdge>();
        mPred2 = new TreeMap<Integer, MyEdge>();

        // iterate over edge and try to keep preinitialized colors
        for (MyEdge e0 : mGraph.getEdges()) {
            Integer e0_x = mGraph.getEndpoints(e0).getFirst();
            Integer e0_y = mGraph.getEndpoints(e0).getSecond();

            if (e0.color == 1 && mUnion1.find(e0_x) != mUnion1.find(e0_y)) {
                mUnion1.union(e0_x, e0_y);
                mCount1++;
            }
            else if (e0.color == 2 && mUnion2.find(e0_x) != mUnion2.find(e0_y)) {
                mUnion2.union(e0_x, e0_y);
                mCount2++;
            }
            else {
                e0.color = 0;
                mCount0++;
            }
        }

        // iterate over all edges and try to put them into a tree.
        for (MyEdge e0 : mGraph.getEdges()) {
            Integer e0_x = mGraph.getEndpoints(e0).getFirst();
            Integer e0_y = mGraph.getEndpoints(e0).getSecond();

            if (e0.color != 0) {
                debug("Edge already added to a tree");
            }
            // check two simple cases
            else if (mUnion1.find(e0_x) != mUnion1.find(e0_y)) {
                debug("Edge added directly to tree 1");

                e0.color = 1;
                mUnion1.union(e0_x, e0_y);
                mCount1++;
                mCount0--;
            }
            else if (mUnion2.find(e0_x) != mUnion2.find(e0_y)) {
                debug("Edge added directly to tree 2");

                e0.color = 2;
                mUnion2.union(e0_x, e0_y);
                mCount2++;
                mCount0--;
            }
            // apply labeling algorithm
            else {
                debug("BFS root node x = " + e0_x);

                bfs_tree(1, e0_x);
                bfs_tree(2, e0_x);

                // augment unfinished tree, or abort if no augmentation found.
                if (!bfs_augmenting_path(e0))
                    return;
            }

            int[] count = new int[3];

            for (MyEdge e : mGraph.getEdges())
                count[e.color]++;

            debug("number of colored edges: " + count[0] + " / " + count[1] + " / " + count[2]);

            assert (mCount0 == count[0]);
            assert (mCount1 == count[1]);
            assert (mCount2 == count[2]);

            if (mCount1 == mGraph.getVertexCount() - 1 && mCount2 == mGraph.getVertexCount() - 1)
                break;
        }
    }

    public boolean isOkay() {
        if (mCount1 + mCount2 != mGraph.getEdgeCount())
            return false;

        return (mCount1 == mGraph.getVertexCount() - 1 && mCount2 == mGraph.getVertexCount() - 1);
    }
}
