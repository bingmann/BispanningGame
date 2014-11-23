/*
 * Copyright (c) 2005, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 *
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 *
 * Created on Aug 23, 2005
 *
 * Modified 2014 by Timo Bingmann for double stroking edges: misusing FillPaint
 * for inner stroke color and DrawPaint for outside.
 */

package net.panthema.BispanningGame;

import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.renderers.BasicEdgeArrowRenderingSupport;
import edu.uci.ics.jung.visualization.renderers.EdgeArrowRenderingSupport;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;
import edu.uci.ics.jung.visualization.transform.shape.GraphicsDecorator;

public class MyEdgeRenderer implements Renderer.Edge<Integer, MyEdge>
{
    public void paintEdge(RenderContext<Integer, MyEdge> rc, Layout<Integer, MyEdge> layout, MyEdge e) {
        GraphicsDecorator g2d = rc.getGraphicsContext();
        Graph<Integer, MyEdge> graph = layout.getGraph();
        if (!rc.getEdgeIncludePredicate().evaluate(Context.<Graph<Integer, MyEdge>, MyEdge> getInstance(graph, e)))
            return;

        // don't draw edge if either incident vertex is not drawn
        Pair<Integer> endpoints = graph.getEndpoints(e);
        Integer v1 = endpoints.getFirst();
        Integer v2 = endpoints.getSecond();
        if (!rc.getVertexIncludePredicate().evaluate(Context.<Graph<Integer, MyEdge>, Integer> getInstance(graph, v1)) || !rc.getVertexIncludePredicate().evaluate(Context.<Graph<Integer, MyEdge>, Integer> getInstance(graph, v2)))
            return;

        Stroke new_stroke = rc.getEdgeStrokeTransformer().transform(e);
        Stroke old_stroke = g2d.getStroke();
        if (new_stroke != null)
            g2d.setStroke(new_stroke);

        drawSimpleEdge(rc, layout, e);

        // restore paint and stroke
        if (new_stroke != null)
            g2d.setStroke(old_stroke);
    }

    /**
     * Draws the edge <code>e</code>, whose endpoints are at
     * <code>(x1,y1)</code> and <code>(x2,y2)</code>, on the graphics context
     * <code>g</code>. The <code>Shape</code> provided by the
     * <code>EdgeShapeFunction</code> instance is scaled in the x-direction so
     * that its width is equal to the distance between <code>(x1,y1)</code> and
     * <code>(x2,y2)</code>.
     */
    protected void drawSimpleEdge(RenderContext<Integer, MyEdge> rc, Layout<Integer, MyEdge> layout, MyEdge e) {

        GraphicsDecorator g = rc.getGraphicsContext();
        Graph<Integer, MyEdge> graph = layout.getGraph();
        Pair<Integer> endpoints = graph.getEndpoints(e);
        Integer v1 = endpoints.getFirst();
        Integer v2 = endpoints.getSecond();

        Point2D p1 = layout.transform(v1);
        Point2D p2 = layout.transform(v2);
        p1 = rc.getMultiLayerTransformer().transform(Layer.VIEW, p1);
        p2 = rc.getMultiLayerTransformer().transform(Layer.VIEW, p2);
        float x1 = (float) p1.getX();
        float y1 = (float) p1.getY();
        float x2 = (float) p2.getX();
        float y2 = (float) p2.getY();

        boolean isLoop = v1.equals(v2);
        Shape s2 = rc.getVertexShapeTransformer().transform(v2);
        Shape edgeShape = rc.getEdgeShapeTransformer().transform(Context.<Graph<Integer, MyEdge>, MyEdge> getInstance(graph, e));

        Rectangle deviceRectangle = null;
        JComponent vv = rc.getScreenDevice();
        if (vv != null) {
            Dimension d = vv.getSize();
            deviceRectangle = new Rectangle(0, 0, d.width, d.height);
        }

        AffineTransform xform = AffineTransform.getTranslateInstance(x1, y1);

        if (isLoop) {
            // this is a self-loop. scale it is larger than the vertex
            // it decorates and translate it so that its nadir is
            // at the center of the vertex.
            Rectangle2D s2Bounds = s2.getBounds2D();
            xform.scale(s2Bounds.getWidth(), s2Bounds.getHeight());
            xform.translate(0, -edgeShape.getBounds2D().getWidth() / 2);
        }
        else {
            // this is a normal edge. Rotate it to the angle between
            // vertex endpoints, then scale it to the distance between
            // the vertices
            float dx = x2 - x1;
            float dy = y2 - y1;
            float thetaRadians = (float) Math.atan2(dy, dx);
            xform.rotate(thetaRadians);
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            xform.scale(dist, 1.0);
        }

        edgeShape = xform.createTransformedShape(edgeShape);

        MutableTransformer vt = rc.getMultiLayerTransformer().getTransformer(Layer.LAYOUT);
        edgeShape = vt.transform(edgeShape);

        if (edgeShape.intersects(deviceRectangle)) {
            Paint oldPaint = g.getPaint();

            // get Paints for filling and drawing
            // (filling is done first so that drawing and label use same Paint)

            Paint draw_paint = rc.getEdgeDrawPaintTransformer().transform(e);
            if (draw_paint != null) {
                g.setPaint(draw_paint);
                g.draw(edgeShape);
            }

            Paint fill_paint = rc.getEdgeFillPaintTransformer().transform(e);
            if (fill_paint != null) {
                // misuse getEdgeArrowStrokeTransformer() interface
                Stroke old_stroke = g.getStroke();
                g.setStroke(rc.getEdgeArrowStrokeTransformer().transform(e));

                g.setPaint(fill_paint);
                g.draw(edgeShape);

                g.setStroke(old_stroke);
            }

            g.setPaint(oldPaint); // restore old paint
        }
    }

    protected EdgeArrowRenderingSupport<Integer, MyEdge> edgeArrowRenderingSupport = new BasicEdgeArrowRenderingSupport<Integer, MyEdge>();

    public EdgeArrowRenderingSupport<Integer, MyEdge> getEdgeArrowRenderingSupport() {
        return edgeArrowRenderingSupport;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void setEdgeArrowRenderingSupport(EdgeArrowRenderingSupport edgeArrowRenderingSupport) {
        this.edgeArrowRenderingSupport = edgeArrowRenderingSupport;
    }
}
