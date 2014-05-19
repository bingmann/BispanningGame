/*******************************************************************************
 * src/net/panthema/BispanningGame/Graph6.java
 *
 * A plugin that can create vertices, undirected edges, and directed edges
 * using mouse gestures.
 *
 *******************************************************************************
 * Copyright (C) Tom Nelson
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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Point2D;

import javax.swing.JComponent;

import org.apache.commons.collections15.Factory;

import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.VisualizationServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.AbstractGraphMousePlugin;
import edu.uci.ics.jung.visualization.util.ArrowFactory;

/**
 * A plugin that can create vertices, undirected edges, and directed edges using
 * mouse gestures.
 * 
 * @author Tom Nelson
 */
public class MyEditingGraphMousePlugin<V, E> extends AbstractGraphMousePlugin implements MouseListener, MouseMotionListener
{
    protected V startVertex;
    protected Point2D down;

    protected CubicCurve2D rawEdge = new CubicCurve2D.Float();
    protected Shape edgeShape;
    protected Shape rawArrowShape;
    protected Shape arrowShape;
    protected VisualizationServer.Paintable edgePaintable;
    protected Factory<V> vertexFactory;
    protected Factory<E> edgeFactory;

    public MyEditingGraphMousePlugin(Factory<V> vertexFactory, Factory<E> edgeFactory) {
        this(MouseEvent.BUTTON1_MASK, vertexFactory, edgeFactory);
    }

    /**
     * create instance and prepare shapes for visual effects
     * 
     * @param modifiers
     */
    public MyEditingGraphMousePlugin(int modifiers, Factory<V> vertexFactory, Factory<E> edgeFactory) {
        super(modifiers);
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        rawEdge.setCurve(0.0f, 0.0f, 0.33f, 100, .66f, -50, 1.0f, 0.0f);
        rawArrowShape = ArrowFactory.getNotchedArrow(20, 16, 8);
        edgePaintable = new EdgePaintable();
        this.cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    /**
     * Overridden to be more flexible, and pass events with key combinations.
     * The default responds to both ButtonOne and ButtonOne+Shift
     */
    @Override
    public boolean checkModifiers(MouseEvent e) {
        return (e.getModifiers() & modifiers) != 0;
    }

    /**
     * If the mouse is pressed in an empty area, create a new vertex there. If
     * the mouse is pressed on an existing vertex, prepare to create an edge
     * from that vertex to another
     */
    @SuppressWarnings("unchecked")
    public void mousePressed(MouseEvent e) {
        if (checkModifiers(e)) {
            final VisualizationViewer<V, E> vv = (VisualizationViewer<V, E>) e.getSource();
            final Point2D p = e.getPoint();
            GraphElementAccessor<V, E> pickSupport = vv.getPickSupport();
            if (pickSupport != null) {
                Graph<V, E> graph = vv.getModel().getGraphLayout().getGraph();

                final V vertex = pickSupport.getVertex(vv.getModel().getGraphLayout(), p.getX(), p.getY());
                if (vertex != null) { // get ready to make an edge
                    startVertex = vertex;
                    down = e.getPoint();
                    transformEdgeShape(down, down);
                    vv.addPostRenderPaintable(edgePaintable);
                }
                else { // make a new vertex
                    V newVertex = vertexFactory.create();
                    Layout<V, E> layout = vv.getModel().getGraphLayout();
                    graph.addVertex(newVertex);
                    layout.setLocation(newVertex, vv.getRenderContext().getMultiLayerTransformer().inverseTransform(e.getPoint()));

                    if (graph instanceof MyGraph)
                        ((MyGraph) graph).graphChanged();
                }
            }
            vv.repaint();
        }
    }

    /**
     * If startVertex is non-null, and the mouse is released over an existing
     * vertex, create an undirected edge from startVertex to the vertex under
     * the mouse pointer. If shift was also pressed, create a directed edge
     * instead.
     */
    @SuppressWarnings("unchecked")
    public void mouseReleased(MouseEvent e) {
        if (checkModifiers(e)) {
            final VisualizationViewer<V, E> vv = (VisualizationViewer<V, E>) e.getSource();
            final Point2D p = e.getPoint();
            Layout<V, E> layout = vv.getModel().getGraphLayout();
            GraphElementAccessor<V, E> pickSupport = vv.getPickSupport();
            if (pickSupport != null) {
                final V vertex = pickSupport.getVertex(layout, p.getX(), p.getY());
                if (vertex != null && startVertex != null && startVertex != vertex) {
                    Graph<V, E> graph = vv.getGraphLayout().getGraph();
                    graph.addEdge(edgeFactory.create(), startVertex, vertex, EdgeType.UNDIRECTED);

                    if (graph instanceof MyGraph)
                        ((MyGraph) graph).graphChanged();

                    vv.repaint();
                }
            }
            startVertex = null;
            down = null;
            vv.removePostRenderPaintable(edgePaintable);
        }
    }

    /**
     * If startVertex is non-null, stretch an edge shape between startVertex and
     * the mouse pointer to simulate edge creation
     */
    @SuppressWarnings("unchecked")
    public void mouseDragged(MouseEvent e) {
        if (checkModifiers(e) || startVertex != null) {
            if (startVertex != null) {
                transformEdgeShape(down, e.getPoint());
            }
            VisualizationViewer<V, E> vv = (VisualizationViewer<V, E>) e.getSource();
            vv.repaint();
        }
    }

    /**
     * code lifted from PluggableRenderer to move an edge shape into an
     * arbitrary position
     */
    private void transformEdgeShape(Point2D down, Point2D out) {
        float x1 = (float) down.getX();
        float y1 = (float) down.getY();
        float x2 = (float) out.getX();
        float y2 = (float) out.getY();

        AffineTransform xform = AffineTransform.getTranslateInstance(x1, y1);

        float dx = x2 - x1;
        float dy = y2 - y1;
        float thetaRadians = (float) Math.atan2(dy, dx);
        xform.rotate(thetaRadians);
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        xform.scale(dist / rawEdge.getBounds().getWidth(), 1.0);
        edgeShape = xform.createTransformedShape(rawEdge);
    }

    /**
     * Used for the edge creation visual effect during mouse drag
     */
    class EdgePaintable implements VisualizationServer.Paintable
    {

        public void paint(Graphics g) {
            if (edgeShape != null) {
                Color oldColor = g.getColor();
                g.setColor(Color.black);
                ((Graphics2D) g).draw(edgeShape);
                g.setColor(oldColor);
            }
        }

        public boolean useTransform() {
            return false;
        }
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
        JComponent c = (JComponent) e.getSource();
        c.setCursor(cursor);
    }

    public void mouseExited(MouseEvent e) {
        JComponent c = (JComponent) e.getSource();
        c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    public void mouseMoved(MouseEvent e) {
    }
}
