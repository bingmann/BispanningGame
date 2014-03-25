/*******************************************************************************
 * src/net/panthema/BispanningGame/Applet.java
 *
 * Main Window of the Java Applet.
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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.RadiusGraphElementAccessor;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.AbstractGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.LayoutScalingControl;
import edu.uci.ics.jung.visualization.control.PickingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.PluggableGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.TranslatingGraphMousePlugin;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.DefaultEdgeLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.DefaultVertexLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;

public class Applet extends javax.swing.JPanel {

	private static final long serialVersionUID = 7526217664458188502L;

	private MyGraph mGraph;
	
	protected VisualizationViewer<Number,MyEdge> mVV;
	
	protected RadiusGraphElementAccessor<Number,MyEdge> mPickSupport;
	
	MyEdge mHoverEdge;
	
	public Applet() {
	
		mGraph = MyGraph.getRandomGraph(8);
        mGraph.calcUniqueExchanges();

		setBackground(Color.WHITE);

        final Layout<Number,MyEdge> layout = new KKLayout<Number,MyEdge>(mGraph);
        final VisualizationViewer<Number,MyEdge> vv = mVV = new VisualizationViewer<Number,MyEdge>(layout);
        vv.setBackground(Color.WHITE);

        PluggableGraphMouse gm = new PluggableGraphMouse();
        gm.add(new TranslatingGraphMousePlugin(MouseEvent.BUTTON3_MASK));
        gm.add(new MyGraphMousePlugin(MouseEvent.BUTTON1_MASK | MouseEvent.BUTTON3_MASK));
        gm.add(new PickingGraphMousePlugin<Number,MyEdge>());
        gm.add(new ScalingGraphMousePlugin(new LayoutScalingControl(), 0, 1.1f, 0.9f));
        vv.setGraphMouse(gm);

        vv.getRenderContext().setVertexLabelRenderer(new DefaultVertexLabelRenderer(Color.black));
        vv.getRenderContext().setVertexLabelTransformer(new Transformer<Number,String>(){
        	public String transform(Number v) {
        		return "v" + v;
        	}});
        vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<Number>());
        vv.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
        
        vv.getRenderContext().setVertexDrawPaintTransformer(new MyVertexDrawPaintFunction<Number>());
        vv.getRenderContext().setVertexFillPaintTransformer(new MyVertexFillPaintFunction());

        vv.getRenderContext().setEdgeStrokeTransformer(new MyEdgeStrokeFunction());
        vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line<Number,MyEdge>());
        vv.getRenderContext().setEdgeDrawPaintTransformer(new MyEdgePaintFunction());
        
        vv.getRenderContext().setEdgeLabelRenderer(new DefaultEdgeLabelRenderer(Color.black));
        vv.getRenderContext().setEdgeLabelTransformer(new Transformer<MyEdge,String>() {
            public String transform(MyEdge e) {
                return e.toString();
            }});
        vv.getRenderContext().setLabelOffset(6);
        
        mPickSupport = new RadiusGraphElementAccessor<Number,MyEdge>();
        vv.setPickSupport(mPickSupport);
        
        setLayout(new BorderLayout());
        add(vv, BorderLayout.CENTER);
	}

	public class MyVertexDrawPaintFunction<V> implements Transformer<V,Paint> {
		public Paint transform(V v) {
			return Color.black;
		}
	}
	
	public class MyVertexFillPaintFunction implements Transformer<Number,Paint> {
		public Paint transform(Number v) {
			
			int count1 = 0, count2 = 0;
			
			for (MyEdge e : mGraph.getIncidentEdges(v)) {
				if (e.color == 1) count1++;
				if (e.color == 2) count2++;
			}
			
			if (count1 == 1 && count2 == 1)
				return Color.MAGENTA;
			if (count1 == 1)
				return Color.RED;
			if (count2 == 1)
				return new Color(0,192,255);
			
			return Color.LIGHT_GRAY;
		}
	}

	public class MyEdgeStrokeFunction implements Transformer<MyEdge,Stroke> {
        protected final int THIN = 3;
        protected final int THICK = 5;

        public Stroke transform(MyEdge e) {
        	int size = 0;
        	
        	if (e.inCircle)
        		size = (e.isFix ? THICK : THIN);
        	else
        		size = (e.isUE ? THICK : THIN);

        	if (e == mHoverEdge)
        		size += 2;

        	return new BasicStroke(size);
        }
	}
 
	public class MyEdgePaintFunction implements Transformer<MyEdge,Paint> {
		public Paint transform(MyEdge e) {
			if (e.color == 1 && !e.inCircle)
				return Color.RED;
			if (e.color == 1 && e.inCircle && e.isFix)
				return Color.ORANGE;
			if (e.color == 1 && e.inCircle && !e.isFix)
				return new Color(255,240,187);
			if (e.color == 2 && !e.inCircle)
				return Color.BLUE;
			if (e.color == 2 && e.inCircle && e.isFix)
				return new Color(0,192,255);
			if (e.color == 2 && e.inCircle && !e.isFix)
				return new Color(192,255,255);
			return Color.BLACK;
		}
	}

	class MyGraphMousePlugin extends AbstractGraphMousePlugin
							 implements MouseListener, MouseMotionListener
	{
		MyEdge mMarkedge = null;
		boolean mHaveCycle = false;
		
		public MyGraphMousePlugin(int modifiers) {
			super(modifiers);
		}

		public void mouseClicked(MouseEvent e) {
			
			if ( (e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0 ) {
				showPopup(e);
				return;
			}
			
			Point2D p = e.getPoint();
			
			p = mVV.getRenderContext().getMultiLayerTransformer().inverseTransform(Layer.LAYOUT, p);
			
	        final MyEdge edge = mPickSupport.getEdge(mVV.getGraphLayout(), p.getX(), p.getY());
	         
	        if (edge == null) return;
	        
	        System.err.println("toggle "+edge);
	        
	        if (!mHaveCycle)
	        {
	        	edge.flipColor();
	        	
	        	mMarkedge = edge;
	        	mHaveCycle = mGraph.markCycleFixes(edge);
	        }
	        else
	        {
	        	if (!edge.inCircle) {
	        		System.out.println("Edge not in Cycle! Ignoring");
	        	}
	        	else {
	        		edge.flipColor();
	        		
	        		if (mGraph.markCycle(edge)) {
	        			System.out.println("Edge does not solve cycle! Ignoring");
	        			edge.flipColor();
	        			mGraph.markCycle(mMarkedge);
	        		}
	        		else {
	        			mHaveCycle = false;
			        	mGraph.calcUniqueExchanges();
	        		}
	        	}
	        }
	        
	        mVV.repaint();
		}

		public void showPopup(MouseEvent e) {
			JMenu newGraph = new JMenu("New Random Graph");
			newGraph.add(new AbstractAction("4 Vertices") {
				private static final long serialVersionUID = 571719411573657774L;
				public void actionPerformed(ActionEvent e) { makeNewRandomGraph(4); }
	        });
			newGraph.add(new AbstractAction("5 Vertices") {
				private static final long serialVersionUID = 571719411573657775L;
	            public void actionPerformed(ActionEvent e) { makeNewRandomGraph(5); }
	        });
			newGraph.add(new AbstractAction("6 Vertices") {
				private static final long serialVersionUID = 571719411573657776L;
	            public void actionPerformed(ActionEvent e) { makeNewRandomGraph(6); }
	        });
			newGraph.add(new AbstractAction("7 Vertices") {
				private static final long serialVersionUID = 571719411573657777L;
	            public void actionPerformed(ActionEvent e) { makeNewRandomGraph(7); }
	        });
			newGraph.add(new AbstractAction("8 Vertices") {
				private static final long serialVersionUID = 571719411573657778L;
	            public void actionPerformed(ActionEvent e) { makeNewRandomGraph(8); }
	        });
			newGraph.add(new AbstractAction("9 Vertices") {
				private static final long serialVersionUID = 571719411573657779L;
	            public void actionPerformed(ActionEvent e) { makeNewRandomGraph(9); }
	        });
			newGraph.add(new AbstractAction("10 Vertices") {
				private static final long serialVersionUID = 571719411573657780L;
	            public void actionPerformed(ActionEvent e) { makeNewRandomGraph(10); }
	        });
			newGraph.add(new AbstractAction("11 Vertices") {
				private static final long serialVersionUID = 571719411573657781L;
	            public void actionPerformed(ActionEvent e) { makeNewRandomGraph(11); }
	        });
			newGraph.add(new AbstractAction("12 Vertices") {
				private static final long serialVersionUID = 571719411573657782L;
	            public void actionPerformed(ActionEvent e) { makeNewRandomGraph(12); }
	        });
			newGraph.add(new AbstractAction("13 Vertices") {
				private static final long serialVersionUID = 571719411573657783L;
	            public void actionPerformed(ActionEvent e) { makeNewRandomGraph(13); }
	        });
			newGraph.add(new AbstractAction("14 Vertices") {
				private static final long serialVersionUID = 571719411573657784L;
	            public void actionPerformed(ActionEvent e) { makeNewRandomGraph(14); }
	        });
			newGraph.add(new AbstractAction("15 Vertices") {
				private static final long serialVersionUID = 571719411573657785L;
	            public void actionPerformed(ActionEvent e) { makeNewRandomGraph(15); }
	        });
			newGraph.add(new AbstractAction("16 Vertices") {
				private static final long serialVersionUID = 571719411573657786L;
	            public void actionPerformed(ActionEvent e) { makeNewRandomGraph(16); }
	        });
			newGraph.add(new AbstractAction("17 Vertices") {
				private static final long serialVersionUID = 571719411573657787L;
	            public void actionPerformed(ActionEvent e) { makeNewRandomGraph(17); }
	        });
			newGraph.add(new AbstractAction("18 Vertices") {
				private static final long serialVersionUID = 571719411573657788L;
	            public void actionPerformed(ActionEvent e) { makeNewRandomGraph(18); }
	        });
			newGraph.add(new AbstractAction("19 Vertices") {
				private static final long serialVersionUID = 571719411573657789L;
	            public void actionPerformed(ActionEvent e) { makeNewRandomGraph(19); }
	        });
			newGraph.add(new AbstractAction("20 Vertices") {
				private static final long serialVersionUID = 571719411573657790L;
	            public void actionPerformed(ActionEvent e) { makeNewRandomGraph(20); }
	        });
			
			JPopupMenu popup = new JPopupMenu();

			popup.add(new AbstractAction("Relayout Graph") {
				private static final long serialVersionUID = 571719411573657791L;
	            public void actionPerformed(ActionEvent e) {
	                final Layout<Number,MyEdge> layout = new KKLayout<Number,MyEdge>(mGraph);
	                mVV.setGraphLayout(layout);
	            }
	        });

			popup.add(new AbstractAction("Load graph6/sparse6") {
				private static final long serialVersionUID = 571719411573657792L;
	            public void actionPerformed(ActionEvent e) {
	            	String input = JOptionPane.showInputDialog(null, "Enter graph6/sparse6 string:", "");
	            	MyGraph g = Graph6.read_graph6(input);
	            	setNewGraph(g);
	            }
	        });

			popup.add(newGraph);           
	        
	        popup.show(mVV, e.getX(), e.getY());
		}
				
		public void mouseEntered(MouseEvent e) {
		}
		public void mouseExited(MouseEvent e) {
		}
		public void mousePressed(MouseEvent e) {
		}
		public void mouseReleased(MouseEvent e) {
		}
		public void mouseDragged(MouseEvent e) {
		}

		public void mouseMoved(MouseEvent e) {
			Point2D p = e.getPoint();	 

			p = mVV.getRenderContext().getMultiLayerTransformer().inverseTransform(Layer.LAYOUT, p);

			MyEdge edge = mPickSupport.getEdge(mVV.getGraphLayout(), p.getX(), p.getY());
			
			if (edge != mHoverEdge)
			{
				mHoverEdge = edge;
				mVV.repaint();
			}
		}
	}
		
	public static void main(String[] s) {
		JFrame jf = new JFrame("Bispanning Graph Game");
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jf.getContentPane().add(new Applet());
		jf.pack();
		jf.setVisible(true);
	}

	void makeNewRandomGraph(int numVertex)
	{
		setNewGraph( MyGraph.getRandomGraph(numVertex) );
	}
	
	void setNewGraph(MyGraph g)
	{
		AlgBispanning ab = new AlgBispanning(g);
		if (!ab.isOkay()) {
			System.out.println("Graph is not bispanning!");
			return;
		}
		
		this.mGraph = g;
        final Layout<Number,MyEdge> layout = new KKLayout<Number,MyEdge>(mGraph);
        mVV.setGraphLayout(layout);
        mGraph.calcUniqueExchanges();
	}	
}
