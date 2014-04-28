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
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import javax.swing.AbstractAction;
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

public class GamePanel extends javax.swing.JPanel
{
    private static final long serialVersionUID = 7526217664458188502L;

    private MyGraph mGraph;

    /** Jung2 visualization object */
    protected VisualizationViewer<Number, MyEdge> mVV;

    /** Jung2 object for getting nearest vertex or edge */
    protected RadiusGraphElementAccessor<Number, MyEdge> mPickSupport;

    /** distance of picking support */
    protected final static double mPickDistance = 32;

    /** Jung2 layouting object */
    protected Layout<Number, MyEdge> mLayout;

    /** Vertex Counter **/
    protected int mNextVertex;

    /** Edge over which the mouse hovers */
    protected MyEdge mHoverEdge;

    /** Edge marked by user */
    protected MyEdge mMarkedge = null;

    /** Flag if a cycle/cut exists in the graph */
    protected boolean mHaveCycle = false;

    public GamePanel() {

        setBackground(Color.WHITE);

        makeNewRandomGraph(8);
        mLayout = new KKLayout<Number, MyEdge>(mGraph);

        mVV = new VisualizationViewer<Number, MyEdge>(mLayout);
        mVV.setBackground(Color.WHITE);

        // set up mouse handling
        PluggableGraphMouse gm = new PluggableGraphMouse();
        gm.add(new MyEditingGraphMousePlugin<Number, MyEdge>(MouseEvent.CTRL_MASK, new MyVertexFactory(), new MyEdgeFactory()));
        gm.add(new TranslatingGraphMousePlugin(MouseEvent.BUTTON3_MASK));
        gm.add(new MyGraphMousePlugin(MouseEvent.BUTTON1_MASK | MouseEvent.BUTTON3_MASK));
        gm.add(new PickingGraphMousePlugin<Number, MyEdge>());
        gm.add(new ScalingGraphMousePlugin(new LayoutScalingControl(), 0, 1.1f, 0.9f));
        mVV.setGraphMouse(gm);

        // set vertex and label drawing
        mVV.getRenderContext().setVertexLabelRenderer(new DefaultVertexLabelRenderer(Color.black));
        mVV.getRenderContext().setVertexLabelTransformer(new Transformer<Number, String>() {
            public String transform(Number v) {
                return "v" + v;
            }
        });
        mVV.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<Number>());
        mVV.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);

        mVV.getRenderContext().setVertexDrawPaintTransformer(new MyVertexDrawPaintFunction<Number>());
        mVV.getRenderContext().setVertexFillPaintTransformer(new MyVertexFillPaintFunction());

        mVV.getRenderContext().setEdgeStrokeTransformer(new MyEdgeStrokeFunction());
        mVV.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line<Number, MyEdge>());
        mVV.getRenderContext().setEdgeDrawPaintTransformer(new MyEdgePaintFunction());

        mVV.getRenderContext().setEdgeLabelRenderer(new DefaultEdgeLabelRenderer(Color.black));
        mVV.getRenderContext().setEdgeLabelTransformer(new Transformer<MyEdge, String>() {
            public String transform(MyEdge e) {
                return e.toString();
            }
        });
        mVV.getRenderContext().setLabelOffset(6);

        // create pick support to select closest nodes and edges
        mPickSupport = new RadiusGraphElementAccessor<Number, MyEdge>();

        // add post renderer to show error messages in background
        mVV.addPostRenderPaintable(new MyGraphPostRenderer());

        setLayout(new BorderLayout());
        add(mVV, BorderLayout.CENTER);
    }

    public class MyVertexDrawPaintFunction<V> implements Transformer<V, Paint>
    {
        public Paint transform(V v) {
            return Color.black;
        }
    }

    public class MyVertexFillPaintFunction implements Transformer<Number, Paint>
    {
        public Paint transform(Number v) {

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

    public class MyVertexFactory implements org.apache.commons.collections15.Factory<Number>
    {
        public Number create() {
            return mGraph.getVertexCount();
        }
    }

    public class MyEdgeStrokeFunction implements Transformer<MyEdge, Stroke>
    {
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

    public class MyEdgePaintFunction implements Transformer<MyEdge, Paint>
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

    public class MyEdgeFactory implements org.apache.commons.collections15.Factory<MyEdge>
    {
        public MyEdge create() {
            return new MyEdge(mGraph.getEdgeCount());
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

    class MyGraphMousePlugin extends AbstractGraphMousePlugin implements MouseListener, MouseMotionListener
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

            p = mVV.getRenderContext().getMultiLayerTransformer().inverseTransform(Layer.LAYOUT, p);

            final MyEdge edge = mPickSupport.getEdge(mVV.getGraphLayout(), p.getX(), p.getY(), mPickDistance);

            if (edge == null)
                return;

            System.err.println("toggle " + edge);

            if (!mHaveCycle) {
                edge.flipColor();

                mMarkedge = edge;
                mHaveCycle = mGraph.markCycleFixes(edge);
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
            newGraph.add(new AbstractAction("1 Vertex") {
                private static final long serialVersionUID = 571719411573657773L;

                public void actionPerformed(ActionEvent e) {
                    makeNewRandomGraph(1);
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

            JPopupMenu popup = new JPopupMenu();

            popup.add(new AbstractAction("Relayout Graph") {
                private static final long serialVersionUID = 571719411573657791L;

                public void actionPerformed(ActionEvent e) {
                    final Layout<Number, MyEdge> layout = new KKLayout<Number, MyEdge>(mGraph);
                    mVV.setGraphLayout(layout);
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
                    Point2D p = mVV.getRenderContext().getMultiLayerTransformer().inverseTransform(Layer.LAYOUT, mClickPoint);

                    Number v = mPickSupport.getVertex(mVV.getGraphLayout(), p.getX(), p.getY(), mPickDistance);
                    if (v == null)
                        return;

                    mGraph.removeVertex(v);
                    mVV.repaint();
                }
            });

            popup.add(new AbstractAction("Delete Edge") {
                private static final long serialVersionUID = 571719411573657791L;

                public void actionPerformed(ActionEvent e) {
                    Point2D p = mVV.getRenderContext().getMultiLayerTransformer().inverseTransform(Layer.LAYOUT, mClickPoint);

                    MyEdge edge = mPickSupport.getEdge(mVV.getGraphLayout(), p.getX(), p.getY(), mPickDistance);
                    if (edge == null)
                        return;

                    mGraph.removeEdge(edge);
                    mVV.repaint();
                }
            });

            popup.add(new AbstractAction("Print graph6") {
                private static final long serialVersionUID = 571719411573657792L;

                public void actionPerformed(ActionEvent e) {
                    JTextArea text = new JTextArea("graph6: " + Graph6.write_graph6(mGraph));
                    JOptionPane.showMessageDialog(null, text);
                }
            });

            popup.add(new AbstractAction("Write PDF") {
                private static final long serialVersionUID = 571719411573657793L;

                public void actionPerformed(ActionEvent e) {
                    try {
                        writePdf();
                    } catch (FileNotFoundException e1) {
                        e1.printStackTrace();
                    } catch (DocumentException de) {
                        System.err.println(de.getMessage());
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

            p = mVV.getRenderContext().getMultiLayerTransformer().inverseTransform(Layer.LAYOUT, p);

            MyEdge edge = mPickSupport.getEdge(mVV.getGraphLayout(), p.getX(), p.getY(), mPickDistance);

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
    }

    void makeNewRandomGraph(int numVertex) {
        setNewGraph(MyGraph.getRandomGraph(numVertex));
    }

    void setNewGraph(MyGraph g) {
        AlgBispanning ab = new AlgBispanning(g);
        if (!ab.isOkay()) {
            System.out.println("Graph is not bispanning!");
            return;
        }

        mGraph = g;
        mHoverEdge = null;
        mMarkedge = null;
        mHaveCycle = false;
        mNextVertex = g.getVertexCount();

        mGraph.calcUniqueExchanges();

        if (mVV != null) {
            mLayout = new KKLayout<Number, MyEdge>(mGraph);
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
}
