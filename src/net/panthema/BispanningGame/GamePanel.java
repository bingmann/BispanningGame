/*******************************************************************************
 * src/net/panthema/BispanningGame/GamePanel.java
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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.AbstractAction;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.collections15.Transformer;

import com.itextpdf.awt.DefaultFontMapper;
import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.io.GraphIOException;
import edu.uci.ics.jung.io.GraphMLWriter;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.AbstractGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.LayoutScalingControl;
import edu.uci.ics.jung.visualization.control.PickingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.PluggableGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.TranslatingGraphMousePlugin;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.picking.ShapePickSupport;
import edu.uci.ics.jung.visualization.renderers.DefaultEdgeLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.DefaultVertexLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;

public class GamePanel extends javax.swing.JPanel
{
    private static final long serialVersionUID = 7526217664458188502L;

    private MyGraph mGraph;

    /** Jung2 visualization object */
    protected VisualizationViewer<Integer, MyEdge> mVV;

    /** Jung2 object for getting nearest vertex or edge */
    protected ShapePickSupport<Integer, MyEdge> mPickSupport;

    /** distance of picking support */
    protected final static float mPickDistance = 32;

    /** Jung2 layouting object */
    protected AbstractLayout<Integer, MyEdge> mLayout;

    /** Vertex Counter **/
    protected int mNextVertex;

    /** Edge over which the mouse hovers */
    protected MyEdge mHoverEdge;

    /** Edge marked by user */
    protected MyEdge mMarkedge = null;

    /** Flag if a cycle/cut exists in the graph */
    protected boolean mHaveCycle = false;

    /** Scale the edge stroke thickness using mouse wheel */
    double edgeScale = 2.0;

    /** Generate only random atomic bispannings graphs */
    protected boolean generateOnlyAtomic = false;

    /** Allow freer non-unique edge exchanges */
    protected boolean allowFreeExchange = false;

    public GamePanel() {

        setBackground(Color.WHITE);

        makeNewRandomGraph(8);
        mLayout = MyGraphLayoutFactory(mGraph);

        mVV = new VisualizationViewer<Integer, MyEdge>(mLayout);
        mVV.setBackground(Color.WHITE);

        // set up mouse handling
        PluggableGraphMouse gm = new PluggableGraphMouse();
        gm.add(new MyEditingGraphMousePlugin<Integer, MyEdge>(MouseEvent.CTRL_MASK, new MyVertexFactory(), new MyEdgeFactory()));
        gm.add(new TranslatingGraphMousePlugin(MouseEvent.BUTTON3_MASK));
        gm.add(new MyGraphMousePlugin(MouseEvent.BUTTON1_MASK | MouseEvent.BUTTON3_MASK));
        gm.add(new PickingGraphMousePlugin<Integer, MyEdge>());
        gm.add(new ScalingGraphMousePlugin(new LayoutScalingControl(), 0, 1.1f, 0.9f));
        mVV.setGraphMouse(gm);

        // set vertex and label drawing
        mVV.getRenderContext().setVertexLabelRenderer(new DefaultVertexLabelRenderer(Color.black));
        mVV.getRenderContext().setVertexLabelTransformer(new Transformer<Integer, String>() {
            public String transform(Integer v) {
                return "v" + v;
            }
        });
        mVV.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<Integer>());
        mVV.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);

        mVV.getRenderer().setEdgeRenderer(new MyEdgeRenderer());
        mVV.getRenderContext().setVertexDrawPaintTransformer(new MyVertexDrawPaintTransformer<Integer>());
        mVV.getRenderContext().setVertexFillPaintTransformer(new MyVertexFillPaintTransformer());

        mVV.getRenderContext().setEdgeStrokeTransformer(new MyEdgeStrokeTransformer());
        MyQuadCurve<Integer, MyEdge> quadcurve = new MyQuadCurve<Integer, MyEdge>();
        mVV.getRenderContext().setEdgeShapeTransformer(quadcurve);
        mVV.getRenderContext().setParallelEdgeIndexFunction(quadcurve);

        mVV.getRenderContext().setEdgeDrawPaintTransformer(new MyEdgeDrawPaintTransformer());
        mVV.getRenderContext().setEdgeFillPaintTransformer(new MyEdgeFillPaintTransformer());
        mVV.getRenderContext().setEdgeArrowStrokeTransformer(new MyEdgeInnerStrokeTransformer());

        mVV.getRenderContext().setEdgeLabelRenderer(new DefaultEdgeLabelRenderer(Color.black));
        mVV.getRenderContext().setEdgeLabelTransformer(new Transformer<MyEdge, String>() {
            public String transform(MyEdge e) {
                return e.toString();
            }
        });
        mVV.getRenderContext().setLabelOffset(6);

        // create pick support to select closest nodes and edges
        mPickSupport = new ShapePickSupport<Integer, MyEdge>(mVV, mPickDistance);

        // add post renderer to show error messages in background
        mVV.addPostRenderPaintable(new MyGraphPostRenderer());

        setLayout(new BorderLayout());
        add(mVV, BorderLayout.CENTER);
    }

    static void showStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter w = new PrintWriter(sw);
        e.printStackTrace(w);
        String st = sw.toString();
        JOptionPane.showMessageDialog(null, "Exception: " + st);
    }

    static AbstractLayout<Integer, MyEdge> MyGraphLayoutFactory(MyGraph g) {
        return new FRLayout<Integer, MyEdge>(g);
    }

    public class MyVertexDrawPaintTransformer<V> implements Transformer<V, Paint>
    {
        public Paint transform(V v) {
            return Color.black;
        }
    }

    public class MyVertexFillPaintTransformer implements Transformer<Integer, Paint>
    {
        public Paint transform(Integer v) {

            int count1 = 0, count2 = 0;

            for (MyEdge e : mGraph.getIncidentEdges(v)) {
                if (e.color == 1)
                    count1++;
                if (e.color == 2)
                    count2++;
            }

            if (count1 == 1 && count2 == 1)
                return Color.MAGENTA;
            if (count1 == 1)
                return Color.RED;
            if (count2 == 1)
                return new Color(0, 192, 255);

            return Color.LIGHT_GRAY;
        }
    }

    public class MyVertexFactory implements org.apache.commons.collections15.Factory<Integer>
    {
        public Integer create() {
            int i = 0;
            while (mGraph.containsVertex(i))
                ++i;
            return i;
        }
    }

    public class MyEdgeStrokeTransformer implements Transformer<MyEdge, Stroke>
    {
        protected final int THIN = 3;
        protected final int THICK = 6;

        public Stroke transform(MyEdge e) {
            int size = 0;

            if (e.inCircle)
                size = (e.isFix ? THICK : THIN);
            else
                size = (e.isUE ? THICK : THIN);

            if (e == mHoverEdge && (e.isUE || allowFreeExchange))
                size += 2;

            return new BasicStroke((int) (size * edgeScale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        }
    }

    public class MyEdgeDrawPaintTransformer implements Transformer<MyEdge, Paint>
    {
        public Paint transform(MyEdge e) {
            if (e.color == 1 && !e.inCircle)
                return Color.RED;
            if (e.color == 1 && e.inCircle && e.isFix)
                return Color.ORANGE;
            if (e.color == 1 && e.inCircle && !e.isFix)
                return new Color(255, 240, 187);
            if (e.color == 2 && !e.inCircle)
                return Color.BLUE;
            if (e.color == 2 && e.inCircle && e.isFix)
                return new Color(0, 192, 255);
            if (e.color == 2 && e.inCircle && !e.isFix)
                return new Color(192, 255, 255);
            return Color.BLACK;
        }
    }

    // misuse EdgeArrowStrokeTransformer interface for stroke of inner line
    public class MyEdgeInnerStrokeTransformer implements Transformer<MyEdge, Stroke>
    {
        public Stroke transform(MyEdge e) {
            return new BasicStroke((int) (2 * edgeScale));
        }
    }

    public class MyEdgeFillPaintTransformer implements Transformer<MyEdge, Paint>
    {
        public Paint transform(MyEdge e) {
            if (e.origColor == 1)
                return Color.RED;
            if (e.origColor == 2)
                return Color.BLUE;
            return Color.BLACK;
        }
    }

    public class MyEdgeFactory implements org.apache.commons.collections15.Factory<MyEdge>
    {
        public MyEdge create() {
            for (int i = 0;; ++i) {
                boolean contains = false;
                for (MyEdge e : mGraph.getEdges()) {
                    if (e.id == i) {
                        contains = true;
                        break;
                    }
                }
                if (!contains)
                    return new MyEdge(i);
            }
        }
    }

    class MyGraphPostRenderer implements VisualizationViewer.Paintable
    {
        Font font;
        FontMetrics metrics;
        int swidth, sheight;
        String str;

        public void paint(Graphics g) {
            Dimension d = mVV.getSize();
            if (font == null) {
                font = new Font(g.getFont().getName(), Font.BOLD, 20);
            }
            if (str != mGraph.message) {
                str = mGraph.message;
                if (str == null)
                    return;

                metrics = g.getFontMetrics(font);
                swidth = metrics.stringWidth(str);
                sheight = metrics.getMaxAscent() + metrics.getMaxDescent();
            }
            if (str == null)
                return;

            int x = (d.width - swidth) / 2;
            int y = (int) (d.height - sheight * 1.5);

            g.setFont(font);
            Color oldColor = g.getColor();
            g.setColor(Color.red);
            g.drawString(str, x, y);
            g.setColor(oldColor);
        }

        public boolean useTransform() {
            return false;
        }
    }

    class MyGraphMousePlugin extends AbstractGraphMousePlugin implements MouseListener, MouseMotionListener, MouseWheelListener
    {
        public MyGraphMousePlugin(int modifiers) {
            super(modifiers);
        }

        Point2D mClickPoint;

        public void mouseClicked(MouseEvent e) {

            if ((e.getModifiers() & MouseEvent.CTRL_MASK) != 0) {
                return;
            }

            if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0) {
                showPopup(e);
                return;
            }

            Point2D p = e.getPoint();

            p = mVV.getRenderContext().getMultiLayerTransformer().inverseTransform(Layer.VIEW, p);

            final MyEdge edge = mPickSupport.getEdge(mVV.getGraphLayout(), p.getX(), p.getY());

            if (edge == null)
                return;

            System.err.println("toggle " + edge);

            if (!mHaveCycle) {
                if (edge.isUE || allowFreeExchange) {
                    edge.flipColor();

                    mMarkedge = edge;
                    mHaveCycle = mGraph.markCycleFixes(edge);
                }
            }
            else {
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
            newGraph.add(new AbstractAction("Empty Graph") {
                private static final long serialVersionUID = 571719411573657773L;

                public void actionPerformed(ActionEvent e) {
                    makeNewRandomGraph(0);
                }
            });
            newGraph.add(new AbstractAction("4 Vertices") {
                private static final long serialVersionUID = 571719411573657774L;

                public void actionPerformed(ActionEvent e) {
                    makeNewRandomGraph(4);
                }
            });
            newGraph.add(new AbstractAction("5 Vertices") {
                private static final long serialVersionUID = 571719411573657775L;

                public void actionPerformed(ActionEvent e) {
                    makeNewRandomGraph(5);
                }
            });
            newGraph.add(new AbstractAction("6 Vertices") {
                private static final long serialVersionUID = 571719411573657776L;

                public void actionPerformed(ActionEvent e) {
                    makeNewRandomGraph(6);
                }
            });
            newGraph.add(new AbstractAction("7 Vertices") {
                private static final long serialVersionUID = 571719411573657777L;

                public void actionPerformed(ActionEvent e) {
                    makeNewRandomGraph(7);
                }
            });
            newGraph.add(new AbstractAction("8 Vertices") {
                private static final long serialVersionUID = 571719411573657778L;

                public void actionPerformed(ActionEvent e) {
                    makeNewRandomGraph(8);
                }
            });
            newGraph.add(new AbstractAction("9 Vertices") {
                private static final long serialVersionUID = 571719411573657779L;

                public void actionPerformed(ActionEvent e) {
                    makeNewRandomGraph(9);
                }
            });
            newGraph.add(new AbstractAction("10 Vertices") {
                private static final long serialVersionUID = 571719411573657780L;

                public void actionPerformed(ActionEvent e) {
                    makeNewRandomGraph(10);
                }
            });
            newGraph.add(new AbstractAction("11 Vertices") {
                private static final long serialVersionUID = 571719411573657781L;

                public void actionPerformed(ActionEvent e) {
                    makeNewRandomGraph(11);
                }
            });
            newGraph.add(new AbstractAction("12 Vertices") {
                private static final long serialVersionUID = 571719411573657782L;

                public void actionPerformed(ActionEvent e) {
                    makeNewRandomGraph(12);
                }
            });
            newGraph.add(new AbstractAction("13 Vertices") {
                private static final long serialVersionUID = 571719411573657783L;

                public void actionPerformed(ActionEvent e) {
                    makeNewRandomGraph(13);
                }
            });
            newGraph.add(new AbstractAction("14 Vertices") {
                private static final long serialVersionUID = 571719411573657784L;

                public void actionPerformed(ActionEvent e) {
                    makeNewRandomGraph(14);
                }
            });
            newGraph.add(new AbstractAction("15 Vertices") {
                private static final long serialVersionUID = 571719411573657785L;

                public void actionPerformed(ActionEvent e) {
                    makeNewRandomGraph(15);
                }
            });
            newGraph.add(new AbstractAction("16 Vertices") {
                private static final long serialVersionUID = 571719411573657786L;

                public void actionPerformed(ActionEvent e) {
                    makeNewRandomGraph(16);
                }
            });
            newGraph.add(new AbstractAction("17 Vertices") {
                private static final long serialVersionUID = 571719411573657787L;

                public void actionPerformed(ActionEvent e) {
                    makeNewRandomGraph(17);
                }
            });
            newGraph.add(new AbstractAction("18 Vertices") {
                private static final long serialVersionUID = 571719411573657788L;

                public void actionPerformed(ActionEvent e) {
                    makeNewRandomGraph(18);
                }
            });
            newGraph.add(new AbstractAction("19 Vertices") {
                private static final long serialVersionUID = 571719411573657789L;

                public void actionPerformed(ActionEvent e) {
                    makeNewRandomGraph(19);
                }
            });
            newGraph.add(new AbstractAction("20 Vertices") {
                private static final long serialVersionUID = 571719411573657790L;

                public void actionPerformed(ActionEvent e) {
                    makeNewRandomGraph(20);
                }
            });

            newGraph.add(new AbstractAction(generateOnlyAtomic ? "Generate Composite or Atomic" : "Generate Only Atomic") {
                private static final long serialVersionUID = 571719711573657790L;

                public void actionPerformed(ActionEvent e) {
                    generateOnlyAtomic = !generateOnlyAtomic;
                }
            });

            JPopupMenu popup = new JPopupMenu();

            popup.add(new AbstractAction("Update Original Colors") {
                private static final long serialVersionUID = 571719411573657796L;

                public void actionPerformed(ActionEvent e) {
                    mGraph.updateOriginalColor();
                    mVV.repaint();
                }
            });

            popup.add(new AbstractAction(allowFreeExchange ? "Restrict to Unique Exchanges" : "Allow Free Edge Exchanges") {
                private static final long serialVersionUID = 571719411573657798L;

                public void actionPerformed(ActionEvent e) {
                    allowFreeExchange = !allowFreeExchange;
                }
            });

            popup.add(new AbstractAction("Load GraphString") {
                private static final long serialVersionUID = 8636579131902717983L;

                public void actionPerformed(ActionEvent e) {
                    String input = JOptionPane.showInputDialog(null, "Enter GraphString:", "");
                    if (input == null)
                        return;
                    try {
                        MyGraph g = GraphString.read_graph(input);
                        setNewGraph(g);
                    }
                    catch (IOException e1) {
                        JOptionPane.showMessageDialog(null, "Error in GraphString: " + e1, "GraphString", JOptionPane.INFORMATION_MESSAGE);

                    }
                }
            });

            popup.add(new AbstractAction("Load graph6/sparse6") {
                private static final long serialVersionUID = 571719411573657792L;

                public void actionPerformed(ActionEvent e) {
                    String input = JOptionPane.showInputDialog(null, "Enter graph6/sparse6 string:", "");
                    if (input == null)
                        return;
                    MyGraph g = Graph6.read_graph6(input);
                    setNewGraph(g);
                }
            });

            popup.add(newGraph);

            popup.add(new AbstractAction("Delete Vertex") {
                private static final long serialVersionUID = 571719411573657791L;

                public void actionPerformed(ActionEvent e) {
                    Point2D p = mVV.getRenderContext().getMultiLayerTransformer().inverseTransform(Layer.VIEW, mClickPoint);

                    Integer v = mPickSupport.getVertex(mVV.getGraphLayout(), p.getX(), p.getY());
                    if (v == null)
                        return;

                    mGraph.removeVertex(v);
                    mVV.getRenderContext().getParallelEdgeIndexFunction().reset();
                    mGraph.graphChanged();
                    mVV.repaint();
                }
            });

            popup.add(new AbstractAction("Delete Edge") {
                private static final long serialVersionUID = 571719411573657794L;

                public void actionPerformed(ActionEvent e) {
                    Point2D p = mVV.getRenderContext().getMultiLayerTransformer().inverseTransform(Layer.VIEW, mClickPoint);

                    MyEdge edge = mPickSupport.getEdge(mVV.getGraphLayout(), p.getX(), p.getY());
                    if (edge == null)
                        return;

                    mGraph.removeEdge(edge);
                    mVV.getRenderContext().getParallelEdgeIndexFunction().reset();
                    mGraph.graphChanged();
                    mVV.repaint();
                }
            });

            popup.add(new AbstractAction("Relayout Graph") {
                private static final long serialVersionUID = 571719411573657791L;

                public void actionPerformed(ActionEvent e) {
                    final AbstractLayout<Integer, MyEdge> layout = MyGraphLayoutFactory(mGraph);
                    mVV.setGraphLayout(layout);
                }
            });

            popup.add(new AbstractAction("Print GraphString") {
                private static final long serialVersionUID = 545719411573657792L;

                public void actionPerformed(ActionEvent e) {
                    JEditorPane text = new JEditorPane("text/plain", GraphString.write_graph(mGraph, mLayout));
                    text.setEditable(false);
                    text.setPreferredSize(new Dimension(300, 125));
                    JOptionPane.showMessageDialog(null, text, "GraphString Serialization", JOptionPane.INFORMATION_MESSAGE);
                }
            });

            popup.add(new AbstractAction("Print graph6") {
                private static final long serialVersionUID = 571719411573657792L;

                public void actionPerformed(ActionEvent e) {
                    JTextArea text = new JTextArea(Graph6.write_graph6(mGraph));
                    JOptionPane.showMessageDialog(null, text, "graph6 Serialization", JOptionPane.INFORMATION_MESSAGE);
                }
            });

            popup.add(new AbstractAction("Write PDF") {
                private static final long serialVersionUID = 571719411573657793L;

                public void actionPerformed(ActionEvent e) {
                    try {
                        writePdf();
                    }
                    catch (FileNotFoundException e1) {
                        showStackTrace(e1);
                    }
                    catch (DocumentException de) {
                        System.err.println(de.getMessage());
                    }
                }
            });

            popup.add(new AbstractAction("Read GraphML") {
                private static final long serialVersionUID = 571719411573657794L;

                public void actionPerformed(ActionEvent e) {
                    try {
                        readGraphML();
                    }
                    catch (IOException e1) {
                        showStackTrace(e1);
                    }
                    catch (GraphIOException e1) {
                        showStackTrace(e1);
                    }
                }
            });

            popup.add(new AbstractAction("Write GraphML") {
                private static final long serialVersionUID = 571719411573657795L;

                public void actionPerformed(ActionEvent e) {
                    try {
                        writeGraphML();
                    }
                    catch (IOException e1) {
                        showStackTrace(e1);
                    }
                }
            });

            mClickPoint = e.getPoint();
            popup.show(mVV, e.getX(), e.getY());
        }

        public void mouseEntered(MouseEvent e) {
            mouseMoved(e);
        }

        public void mouseMoved(MouseEvent e) {
            Point2D p = e.getPoint();

            p = mVV.getRenderContext().getMultiLayerTransformer().inverseTransform(Layer.VIEW, p);

            MyEdge edge = mPickSupport.getEdge(mVV.getGraphLayout(), p.getX(), p.getY());

            if (edge != mHoverEdge) {
                mHoverEdge = edge;
                mVV.repaint();
            }
        }

        public void mouseExited(MouseEvent e) {
            mHoverEdge = null;
            mVV.repaint();
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseDragged(MouseEvent e) {
        }

        public void mouseWheelMoved(MouseWheelEvent e) {

            // act only when CTRL is pressed
            if ((e.getModifiers() & MouseEvent.CTRL_MASK) == 0) {
                return;
            }

            int notches = e.getWheelRotation();

            if (notches < 0) { // mouse wheel moved UP
                edgeScale *= 0.9;
            }
            else { // mouse wheel moved DOWN
                edgeScale /= 0.9;
            }

            mVV.repaint();
        }
    }

    void makeNewRandomGraph(int numVertex) {
        MyGraph g;
        do {
            g = MyGraph.getRandomGraph(numVertex);
        } while (numVertex != 0 && generateOnlyAtomic && !g.isAtomicBispanner());
        setNewGraph(g);
    }

    void setNewGraph(MyGraph g) {

        mGraph = g;
        mGraph.graphChanged();
        mHoverEdge = null;
        mMarkedge = null;
        mHaveCycle = false;
        mNextVertex = g.getVertexCount();

        mGraph.calcUniqueExchanges();
        mGraph.updateOriginalColor();

        if (mVV != null) {

            if (g.mInitialLayout != null)
                mLayout = g.mInitialLayout;
            else
                mLayout = MyGraphLayoutFactory(mGraph);

            mVV.setGraphLayout(mLayout);
        }
    }

    public void writePdf() throws FileNotFoundException, DocumentException {

        // Query user for filename
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Specify PDF file to save");
        chooser.setCurrentDirectory(new File("."));
        FileNameExtensionFilter filter = new FileNameExtensionFilter("PDF Documents", "pdf");
        chooser.setFileFilter(filter);

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        File outfile = chooser.getSelectedFile();
        if (!outfile.getAbsolutePath().endsWith(".pdf")) {
            outfile = new File(outfile.getAbsolutePath() + ".pdf");
        }

        // Calculate page size rectangle
        Dimension size = mVV.getSize();
        Rectangle rsize = new Rectangle(size.width, size.height);

        // Open the PDF file for writing - and create a Graphics2D object
        Document document = new Document(rsize);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outfile));
        document.open();

        PdfContentByte contentByte = writer.getDirectContent();
        PdfGraphics2D graphics2d = new PdfGraphics2D(contentByte, size.width, size.height, new DefaultFontMapper());

        // Create a container to hold the visualisation
        Container container = new Container();
        container.addNotify();
        container.add(mVV);
        container.setVisible(true);
        container.paintComponents(graphics2d);

        // Dispose of the graphics and close the document
        graphics2d.dispose();
        document.close();

        // Put mVV pack onto visible plane
        setLayout(new BorderLayout());
        add(mVV, BorderLayout.CENTER);
    }

    public void writeGraphML() throws IOException {

        // Query user for filename
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Specify GraphML file to save");
        chooser.setCurrentDirectory(new File("."));
        FileNameExtensionFilter filter = new FileNameExtensionFilter("GraphML File", "graphml");
        chooser.setFileFilter(filter);

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        File outfile = chooser.getSelectedFile();
        if (!outfile.getAbsolutePath().endsWith(".graphml")) {
            outfile = new File(outfile.getAbsolutePath() + ".graphml");
        }

        // construct graphml writer
        GraphMLWriter<Integer, MyEdge> graphWriter = new GraphMLWriter<Integer, MyEdge>();

        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outfile)));

        graphWriter.addVertexData("x", null, "0", new Transformer<Integer, String>() {
            public String transform(Integer v) {
                return Double.toString(mLayout.getX(v));
            }
        });

        graphWriter.addVertexData("y", null, "0", new Transformer<Integer, String>() {
            public String transform(Integer v) {
                return Double.toString(mLayout.getY(v));
            }
        });

        graphWriter.addEdgeData("color", null, "0", new Transformer<MyEdge, String>() {
            public String transform(MyEdge e) {
                return Integer.toString(e.color);
            }
        });

        graphWriter.save(mGraph, out);
    }

    public void readGraphML() throws IOException, GraphIOException {

        MyGraphMLReader gml = new MyGraphMLReader(this);
        setNewGraph(gml.newGraph);

        mLayout = new StaticLayout<Integer, MyEdge>(mGraph, gml);
        mVV.setGraphLayout(mLayout);
    }
}
