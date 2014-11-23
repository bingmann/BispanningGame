/*******************************************************************************
 * src/net/panthema/BispanningGame/MyQuadCurbe.java
 *
 * An AbstractEdgeShapeTransformer which starts single edges straight and
 * multiple parallel edges as arcs.  Loosely based on parts of EdgeShape.java
 * from jung2.
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

import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.QuadCurve2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.graph.util.EdgeIndexFunction;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.decorators.AbstractEdgeShapeTransformer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape.IndexedRendering;

public class MyQuadCurve<V, E> extends AbstractEdgeShapeTransformer<V, E> implements IndexedRendering<V, E>, EdgeIndexFunction<V, E>
{
    // singleton straight line
    private static Line2D lineInstance = new Line2D.Float(0.0f, 0.0f, 1.0f, 0.0f);

    // singleton curved line
    private static QuadCurve2D curveInstance = new QuadCurve2D.Float();

    MyQuadCurve() {
        setControlOffsetIncrement(40);
    }

    /**
     * Get the shape for this edge, returning either a straight or a curve.
     */
    public Shape transform(Context<Graph<V, E>, E> context) {
        Graph<V, E> graph = context.graph;
        E e = context.element;

        Pair<V> endpoints = graph.getEndpoints(e);
        if (endpoints != null) {
            boolean isLoop = endpoints.getFirst().equals(endpoints.getSecond());
            assert (!isLoop);
        }

        IndexCount ic = getIndexCount(graph, e);

        if (ic.count == 1) {
            return lineInstance;
        }
        else {
            float controlY = -(control_offset_increment * (ic.count - 1)) / 2.0f + (control_offset_increment * ic.index);
            if (ic.opposite)
                controlY *= -1;
            // System.out.println("ic: " + ic.count + " - " + ic.index + " - " +
            // ic.opposite + " : " + controlY);
            curveInstance.setCurve(0.0f, 0.0f, 0.5f, controlY, 1.0f, 0.0f);
            return curveInstance;
        }
    }

    /**
     * Class containing the index and total count of parallel edges for each
     * edge.
     */
    class IndexCount
    {
        int index, count;
        boolean opposite;

        IndexCount(int i, int c) {
            index = i;
            count = c;
            opposite = false;
        }
    };

    protected Map<Context<Graph<V, E>, E>, IndexCount> edge_index = new HashMap<Context<Graph<V, E>, E>, IndexCount>();

    /**
     * Returns the IndexCount object for e.
     */
    public IndexCount getIndexCount(Graph<V, E> graph, E e) {

        IndexCount indexcount = edge_index.get(Context.<Graph<V, E>, E> getInstance(graph, e));

        if (indexcount == null) {
            Pair<V> endpoints = graph.getEndpoints(e);
            V u = endpoints.getFirst();
            V v = endpoints.getSecond();
            assert (!u.equals(v));
            indexcount = getIndexCount(graph, e, u, v);
        }
        return indexcount;
    }

    public int getIndex(Graph<V, E> graph, E e) {
        return getIndexCount(graph, e).index;
    }

    protected IndexCount getIndexCount(Graph<V, E> graph, E e, V v, V u) {
        Collection<E> commonEdgeSet = new HashSet<E>(graph.getIncidentEdges(u));
        commonEdgeSet.retainAll(graph.getIncidentEdges(v));
        for (Iterator<E> iterator = commonEdgeSet.iterator(); iterator.hasNext();) {
            E edge = iterator.next();
            Pair<V> ep = graph.getEndpoints(edge);
            V first = ep.getFirst(), second = ep.getSecond();
            // no loops
            assert (!first.equals(second));
        }
        int index = 0;
        for (E edge : commonEdgeSet) {
            IndexCount indexcount = new IndexCount(index, commonEdgeSet.size());
            // mark edges in opposite direction
            Pair<V> ep = graph.getEndpoints(edge);
            if (ep.getFirst().equals(v) == false) {
                indexcount.opposite = true;
            }
            edge_index.put(Context.<Graph<V, E>, E> getInstance(graph, edge), indexcount);
            index++;
        }
        return edge_index.get(Context.<Graph<V, E>, E> getInstance(graph, e));
    }

    public EdgeIndexFunction<V, E> getEdgeIndexFunction() {
        return this;
    }

    public void setEdgeIndexFunction(EdgeIndexFunction<V, E> peif) {
        reset();
    }

    public void reset(Graph<V, E> graph, E e) {
        reset();
    }

    /**
     * Clears all edge indices for all edges in all graphs.
     */
    public void reset() {
        // System.out.println("MyQuadCurve::reset()");
        edge_index.clear();
    }
}
