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
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
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
import edu.uci.ics.jung.visualization.MultiLayerTransformer;
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

    /** Text area for game log */
    protected JTextArea logTextArea;

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

    /** Number of turns played in game */
    protected int mTurnNum = 0;

    /** Edge marked by Alice */
    protected MyEdge mMarkedge = null;

    /** Flag whether to automatically play Bob's part */
    protected boolean mAutoPlayBob = true;

    /** Flag if a cycle/cut exists in the graph */
    protected boolean mHaveCycle = false;

    /** Scale the edge stroke thickness using mouse wheel */
    float edgeScale = 2.0f;

    /** Generate only random atomic bispannings graphs */
    protected boolean generateOnlyAtomic = false;

    /** Allow freer non-unique edge exchanges */
    protected boolean allowFreeExchange = true;

    /** Image of Alice and Bob */
    BufferedImage ImageAlice, ImageBob;

    public GamePanel() throws IOException {

        makeActions();

        setBackground(Color.WHITE);

        ImageAlice = ImageIO.read(getClass().getClassLoader().getResourceAsStream("net/panthema/BispanningGame/images/Alice.png"));
        ImageBob = ImageIO.read(getClass().getClassLoader().getResourceAsStream("net/panthema/BispanningGame/images/Bob.png"));

        logTextArea = new JTextArea();

        makeNewRandomGraph(8);
        mLayout = MyGraphLayoutFactory(mGraph);

        mVV = new VisualizationViewer<Integer, MyEdge>(mLayout);
        mVV.setSize(new Dimension(1000,800));
        mVV.setBackground(Color.WHITE);

        // Bob's play does not repeat.
        mPlayBob.setRepeats(false);

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

        // add pre renderer to draw Alice and Bob
        mVV.addPreRenderPaintable(new MyGraphPreRenderer());

        // add post renderer to show error messages in background
        mVV.addPostRenderPaintable(new MyGraphPostRenderer());

        setLayout(new BorderLayout());
        add(mVV, BorderLayout.CENTER);

        JPanel panelSouth = new JPanel();
        add(panelSouth, BorderLayout.SOUTH);
        panelSouth.setLayout(new GridLayout(0, 2, 0, 0));

        JPanel panelButtons = new JPanel();
        panelSouth.add(panelButtons);
        panelButtons.setLayout(new GridLayout(2, 2, 0, 0));
        panelSouth.setPreferredSize(new Dimension(800, 60));

        final JButton btnNewRandomGraph = new JButton("New Random Graph");
        btnNewRandomGraph.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JPopupMenu popup = new JPopupMenu();

                for (int i = 0; i < actionRandomGraph.length; ++i) {
                    if (actionRandomGraph[i] != null)
                        popup.add(actionRandomGraph[i]);
                }
                popup.addSeparator();
                popup.add(getActionNewGraphType());

                popup.show(btnNewRandomGraph, e.getX(), e.getY());
            }
        });
        panelButtons.add(btnNewRandomGraph);

        final JButton btnNewNamedGraph = new JButton("New Named Graph");
        btnNewNamedGraph.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JPopupMenu popup = new JPopupMenu();

                for (int i = 0; i < actionNamedGraph.size(); ++i) {
                    if (actionNamedGraph.get(i) != null)
                        popup.add(actionNamedGraph.get(i));
                }

                popup.show(btnNewNamedGraph, e.getX(), e.getY());
            }
        });
        panelButtons.add(btnNewNamedGraph);

        final JButton btnRelayout = new JButton("Relayout");
        btnRelayout.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                relayoutGraph();
            }
        });
        panelButtons.add(btnRelayout);

        final JButton btnOptions = new JButton("Options");
        btnOptions.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JPopupMenu popup = new JPopupMenu();

                addPopupActions(popup);
                popup.addSeparator();

                popup.show(btnOptions, e.getX(), e.getY());
            }
        });
        panelButtons.add(btnOptions);

        JScrollPane scrollPane = new JScrollPane(logTextArea);
        panelSouth.add(scrollPane);

        logTextArea.setEditable(false);

        setSize(new Dimension(1000, 800));
        relayoutGraph();
    }

    static void showStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter w = new PrintWriter(sw);
        e.printStackTrace(w);
        String st = sw.toString();
        JOptionPane.showMessageDialog(null, "Exception: " + st);
    }

    /**
     * Append new line to game text log.
     */
    void putLog(String text) {
        if (logTextArea.getDocument().getLength() != 0)
            text = "\n" + text;
        logTextArea.append(text);
        logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
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

            int size = (e.inCycle || e.inCut || e.isUE) ? THICK : THIN;
            if (allowFreeExchange) size = THICK;

            if (e == mHoverEdge && (e.isUE || allowFreeExchange))
                size += 2;

            float[] dash_cut = { size * edgeScale };

            if (e.inCut)
                return new BasicStroke((int) (size * edgeScale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0.0f, dash_cut, 0.0f);
            else
                return new BasicStroke((int) (size * edgeScale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        }
    }

    public class MyEdgeDrawPaintTransformer implements Transformer<MyEdge, Paint>
    {
        public Paint transform(MyEdge e) {
            if (e.color == 1 && !e.inCycle)
                return Color.BLUE;
            if (e.color == 1 && e.inCycle)
                return new Color(0, 164, 255);
            if (e.color == 2 && !e.inCycle)
                return Color.RED;
            if (e.color == 2 && e.inCycle)
                return new Color(255, 164, 0);
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
                return Color.BLUE;
            if (e.origColor == 2)
                return Color.RED;
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

    class MyGraphPreRenderer implements VisualizationViewer.Paintable
    {
        Font font;
        FontMetrics metrics;
        int swidthAlice, swidthBob, sheight;

        Color highLight = new Color(192, 255, 192);
        Color high = new Color(0, 192, 0);

        public void paint(Graphics _g) {
            Graphics2D g = (Graphics2D) _g;
            if (font == null) {
                font = new Font(g.getFont().getName(), Font.BOLD, 18);

                metrics = g.getFontMetrics(font);
                swidthAlice = metrics.stringWidth("Alice");
                swidthBob = metrics.stringWidth("Bob");
                sheight = metrics.getMaxAscent() + metrics.getMaxDescent();
            }

            Dimension d = mVV.getSize();

            int AliceHeight = 100 * ImageAlice.getHeight() / ImageAlice.getWidth();
            int BobHeight = 100 * ImageBob.getHeight() / ImageBob.getWidth();

            g.setFont(font);
            g.setStroke(new BasicStroke(2));

            if (!mHaveCycle) {
                g.setColor(highLight);
                g.fillRoundRect(0, 0, 103, AliceHeight + sheight + 6, 20, 20);
                g.setColor(high);
                g.drawRoundRect(0, 0, 103, AliceHeight + sheight + 6, 20, 20);
            }
            else {
                g.setColor(Color.BLACK);
                g.drawRoundRect(0, 0, 103, AliceHeight + sheight + 6, 20, 20);
            }

            g.setColor(Color.BLACK);
            g.drawImage(ImageAlice, 4, 6, 100, AliceHeight, 0, 0, ImageAlice.getWidth(), ImageAlice.getHeight(), null);
            g.drawString("Alice", (100 - swidthAlice) / 2, AliceHeight + sheight - 3);

            if (mHaveCycle) {
                g.setColor(highLight);
                g.fillRoundRect(d.width - 105, d.height - BobHeight - sheight - 8, 103, BobHeight + sheight + 6, 20, 20);
                g.setColor(high);
                g.drawRoundRect(d.width - 105, d.height - BobHeight - sheight - 8, 103, BobHeight + sheight + 6, 20, 20);
            }
            else {
                g.setColor(Color.BLACK);
                g.drawRoundRect(d.width - 105, d.height - BobHeight - sheight - 8, 103, BobHeight + sheight + 6, 20, 20);
            }

            g.setColor(Color.BLACK);
            g.drawImage(ImageBob, d.width - 100, d.height - BobHeight - sheight - 2, d.width - 5, d.height - sheight - 4, 0, 0, ImageBob.getWidth(), ImageBob.getHeight(), null);
            g.drawString("Bob", d.width - swidthBob - (100 - swidthBob) / 2, d.height - 8);
        }

        public boolean useTransform() {
            return false;
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

            // no mouse click when Bob is playing!
            if (mHaveCycle && mAutoPlayBob)
                return;

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
                    putLog("Turn " + (++mTurnNum) + ": Alice flips edge " + edge.id + " " + edge.colorName(false) + " -> " + edge.colorName(true) + ".");
                    edge.flipColor();

                    mMarkedge = edge;
                    mHaveCycle = mGraph.markCycleFixes(edge);

                    if (mAutoPlayBob) {
                        mPlayBob.start();
                    }

                    updateGraphMessage();
                }
            }
            else {
                if (!edge.inCycle) {
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
                        // flip edge
                        putLog("Turn " + (++mTurnNum) + ": Bob flips edge " + edge.id + " " + edge.colorName(true) + " -> " + edge.colorName(false) + ".");

                        mHaveCycle = false;
                        mGraph.calcUniqueExchanges();
                    }

                    updateGraphMessage();
                }
            }

            mVV.repaint();
        }

        public void showPopup(MouseEvent e) {

            JPopupMenu popup = new JPopupMenu();

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

            popup.addSeparator();
            addPopupActions(popup);

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

    Timer mPlayBob = new Timer(1000, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            // markCycleFixes has set isFix for all fix edges.
            ArrayList<MyEdge> fix1list = new ArrayList<MyEdge>();
            ArrayList<MyEdge> fix2list = new ArrayList<MyEdge>();
            // Bob prefers to re-color already flipped edges: fix1list

            for (MyEdge ei : mGraph.getEdges()) {
                if (ei.isFix && ei != mMarkedge) {
                    if (ei.color != ei.origColor) {
                        fix1list.add(ei);
                    }
                    else {
                        fix2list.add(ei);
                    }
                }
            }

            fix1list.addAll(fix2list);

            for (MyEdge eFix : fix1list) {
                // flip edge
                putLog("Turn " + (++mTurnNum) + ": Bob flips edge " + eFix.id + " " + eFix.colorName(false) + " -> " + eFix.colorName(true) + ".");

                eFix.flipColor();

                if (mGraph.markCycle(eFix)) {
                    System.out.println("Edge does not solve cycle! Ignoring");
                    eFix.flipColor();
                    mGraph.markCycle(mMarkedge);
                }
                else {
                    mHaveCycle = false;
                    mGraph.calcUniqueExchanges();
                    break;
                }
            }

            if (mHaveCycle) {
                System.out.println("Bob could not fix the graph?");
            }

            updateGraphMessage();
            repaint();
        }
    });

    AbstractAction[] actionRandomGraph = new AbstractAction[20 + 1];
    ArrayList<AbstractAction> actionNamedGraph = new ArrayList<AbstractAction>();

    void makeActions() {
        actionRandomGraph[0] = new AbstractAction("Empty Graph") {
            private static final long serialVersionUID = 571719411573657773L;

            public void actionPerformed(ActionEvent e) {
                makeNewRandomGraph(0);
            }
        };
        actionRandomGraph[4] = new AbstractAction("4 Vertices") {
            private static final long serialVersionUID = 571719411573657774L;

            public void actionPerformed(ActionEvent e) {
                makeNewRandomGraph(4);
            }
        };
        actionRandomGraph[5] = new AbstractAction("5 Vertices") {
            private static final long serialVersionUID = 571719411573657775L;

            public void actionPerformed(ActionEvent e) {
                makeNewRandomGraph(5);
            }
        };
        actionRandomGraph[6] = new AbstractAction("6 Vertices") {
            private static final long serialVersionUID = 571719411573657776L;

            public void actionPerformed(ActionEvent e) {
                makeNewRandomGraph(6);
            }
        };
        actionRandomGraph[7] = new AbstractAction("7 Vertices") {
            private static final long serialVersionUID = 571719411573657777L;

            public void actionPerformed(ActionEvent e) {
                makeNewRandomGraph(7);
            }
        };
        actionRandomGraph[8] = new AbstractAction("8 Vertices") {
            private static final long serialVersionUID = 571719411573657778L;

            public void actionPerformed(ActionEvent e) {
                makeNewRandomGraph(8);
            }
        };
        actionRandomGraph[9] = new AbstractAction("9 Vertices") {
            private static final long serialVersionUID = 571719411573657779L;

            public void actionPerformed(ActionEvent e) {
                makeNewRandomGraph(9);
            }
        };
        actionRandomGraph[10] = new AbstractAction("10 Vertices") {
            private static final long serialVersionUID = 571719411573657780L;

            public void actionPerformed(ActionEvent e) {
                makeNewRandomGraph(10);
            }
        };
        actionRandomGraph[11] = new AbstractAction("11 Vertices") {
            private static final long serialVersionUID = 571719411573657781L;

            public void actionPerformed(ActionEvent e) {
                makeNewRandomGraph(11);
            }
        };
        actionRandomGraph[12] = new AbstractAction("12 Vertices") {
            private static final long serialVersionUID = 571719411573657782L;

            public void actionPerformed(ActionEvent e) {
                makeNewRandomGraph(12);
            }
        };
        actionRandomGraph[13] = new AbstractAction("13 Vertices") {
            private static final long serialVersionUID = 571719411573657783L;

            public void actionPerformed(ActionEvent e) {
                makeNewRandomGraph(13);
            }
        };
        actionRandomGraph[14] = new AbstractAction("14 Vertices") {
            private static final long serialVersionUID = 571719411573657784L;

            public void actionPerformed(ActionEvent e) {
                makeNewRandomGraph(14);
            }
        };
        actionRandomGraph[15] = new AbstractAction("15 Vertices") {
            private static final long serialVersionUID = 571719411573657785L;

            public void actionPerformed(ActionEvent e) {
                makeNewRandomGraph(15);
            }
        };
        actionRandomGraph[16] = new AbstractAction("16 Vertices") {
            private static final long serialVersionUID = 571719411573657786L;

            public void actionPerformed(ActionEvent e) {
                makeNewRandomGraph(16);
            }
        };
        actionRandomGraph[17] = new AbstractAction("17 Vertices") {
            private static final long serialVersionUID = 571719411573657787L;

            public void actionPerformed(ActionEvent e) {
                makeNewRandomGraph(17);
            }
        };
        actionRandomGraph[18] = new AbstractAction("18 Vertices") {
            private static final long serialVersionUID = 571719411573657788L;

            public void actionPerformed(ActionEvent e) {
                makeNewRandomGraph(18);
            }
        };
        actionRandomGraph[19] = new AbstractAction("19 Vertices") {
            private static final long serialVersionUID = 571719411573657789L;

            public void actionPerformed(ActionEvent e) {
                makeNewRandomGraph(19);
            }
        };
        actionRandomGraph[20] = new AbstractAction("20 Vertices") {
            private static final long serialVersionUID = 571719411573657790L;

            public void actionPerformed(ActionEvent e) {
                makeNewRandomGraph(20);
            }
        };

        // Special Named Graphs

        actionNamedGraph.add(new AbstractAction("K4 (complete, 4 vertices)") {
            private static final long serialVersionUID = 571719411573657791L;

            public void actionPerformed(ActionEvent e) {
                loadGraphString("V4:i0x0y0/i1x1y0/i2x1y1/i3x0y1/;E6:i0t0h1c1/i1t0h2c1/i2t0h3c2/i3t1h2c2/i4t1h3c2/i5t2h3c1/;");
            }
        });

        actionNamedGraph.add(new AbstractAction("W5 (wheel, 5 vertices)") {
            private static final long serialVersionUID = 571719411573657792L;

            public void actionPerformed(ActionEvent e) {
                loadGraphString("V5:i0x10y10/i1x10y0/i2x0y10/i3x10y20/i4x20y10/;E8:i0t0h1c1/i1t1h2c1/i2t2h0c2/i3t2h3c1/i4t3h0c2/i5t3h4c1/i6t4h0c2/i7t4h1c2/;");
            }
        });

        actionNamedGraph.add(new AbstractAction("K4 + K4 (2-clique sum, 6 vertices)") {
            private static final long serialVersionUID = 571719411573657793L;

            public void actionPerformed(ActionEvent e) {
                loadGraphString("V6:i0x0y0/i1x1y0/i2x1y1/i3x0y1/i4x2y0/i5x2y1/;E10:i0t0h1c1/i1t0h2c2/i2t0h3c1/i4t1h3c2/i5t2h3c2/i6t1h4c2/i7t1h5c1/i9t4h5c2/i10t4h2c1/i11t2h5c1/;");
            }
        });

        actionNamedGraph.add(new AbstractAction("B 6,12 difficult (6 vertices)") {
            private static final long serialVersionUID = 571719411573457792L;

            public void actionPerformed(ActionEvent e) {
                loadGraphString("V6:i0x0y12/i1x20y15/i2x40y12/i3x10y30/i4x20y0/i5x30y30/;E10:i0t0h3c2/i1t1h3c1/i2t2h3c1/i3t0h4c1/i4t1h4c2/i5t2h4c2/i6t0h5c1/i7t1h5c2/i8t2h5c1/i9t3h5c2/;");
            }
        });

        actionNamedGraph.add(new AbstractAction("W6 (wheel, 6 vertices)") {
            private static final long serialVersionUID = 571719411573657794L;

            public void actionPerformed(ActionEvent e) {
                loadGraphString("V6:i0x0y0/i1x0y-1000/i2x-951y-309/i3x-588y809/i4x588y809/i5x951y-309/;E10:i0t0h1c1/i1t1h2c1/i2t0h2c2/i3t2h3c1/i4t0h3c2/i5t3h4c1/i6t0h4c2/i7t4h5c1/i8t0h5c2/i9t5h1c2/;");
            }
        });

        actionNamedGraph.add(new AbstractAction("B7,1 triangle free (7 vertices)") {
            private static final long serialVersionUID = 571719411573657796L;

            public void actionPerformed(ActionEvent e) {
                loadGraphString("V7:i0x2y0/i1x2y2/i2x2y4/i3x0y1/i4x0y3/i5x4y1/i6x4y3/;E12:i0t3h0c2/i1t3h1c1/i2t3h2c1/i3t4h0c2/i4t4h1c2/i5t4h2c1/i6t5h0c1/i7t5h1c2/i8t5h2c2/i9t6h0c1/i10t6h1c1/i11t6h2c2/;");
            }
        });

        actionNamedGraph.add(new AbstractAction("B8,1 triangle free (8 vertices)") {
            private static final long serialVersionUID = 571719411573657798L;

            public void actionPerformed(ActionEvent e) {
                loadGraphString("V8:i0x10y20/i1x20y10/i2x40y35/i3x0y35/i4x40y15/i5x0y15/i6x30y30/i7x20y40/;E14:i0t0h4c1/i1t1h4c2/i2t2h4c1/i3t0h5c1/i4t1h5c2/i5t3h5c2/i6t0h6c2/i7t1h6c1/i8t2h6c2/i9t3h6c1/i10t0h7c2/i11t1h7c1/i12t2h7c1/i13t3h7c2/;");
            }
        });

        actionNamedGraph.add(new AbstractAction("B9,1 difficult (9 vertices)") {
            private static final long serialVersionUID = 571719411573657796L;

            public void actionPerformed(ActionEvent e) {
                loadGraphString("V9:i0x1y0/i1x3y2/i2x1y2/i3x1y1/i4x3y1/i5x0y1/i6x0y2/i7x2y2/i8x2y1/;E16:i0t0h4c1/i1t1h4c1/i2t0h5c2/i3t2h5c2/i4t3h5c1/i5t0h6c2/i6t2h6c1/i7t3h6c2/i8t1h7c1/i9t2h7c1/i10t3h7c2/i11t4h7c2/i12t0h8c1/i13t1h8c2/i14t2h8c2/i15t3h8c1/;");
            }
        });

        actionNamedGraph.add(new AbstractAction("2x2 K4 grid (9 vertices)") {
            private static final long serialVersionUID = 571719411573657796L;

            public void actionPerformed(ActionEvent e) {
                loadGraphString("V9:i0x0y0/i1x1y0/i2x2y0/i3x0y1/i4x1y1/i5x2y1/i6x0y2/i7x1y2/i8x2y2/;E16:i0t0h1c1/i1t0h4c2/i2t0h3c2/i3t1h2c2/i4t1h3c2/i5t1h5c1/i6t2h4c1/i7t2h5c1/i8t3h6c1/i9t3h7c1/i10t4h6c1/i11t4h8c2/i12t5h7c2/i13t5h8c2/i14t6h7c2/i15t7h8c1/;");
            }
        });

        actionNamedGraph.add(new AbstractAction("B10,1 difficult (10 vertices)") {
            private static final long serialVersionUID = 571419411573657796L;

            public void actionPerformed(ActionEvent e) {
                loadGraphString("V10:i0x20y6/i1x0y-25/i2x0y25/i3x-20y6/i4x30y-12/i5x-30y13/i6x30y13/i7x-30y-12/i8x-10y-10/i9x10y-10/;E18:i0t0h4c1/i1t1h4c2/i2t0h5c2/i3t2h5c1/i4t2h6c2/i5t3h6c1/i6t4h6c2/i7t1h7c1/i8t3h7c2/i9t5h7c1/i10t0h8c1/i11t1h8c2/i12t2h8c1/i13t3h8c2/i14t0h9c2/i15t1h9c1/i16t2h9c2/i17t3h9c1/;");
            }
        });

        actionNamedGraph.add(new AbstractAction("B10,2 difficult (10 vertices)") {
            private static final long serialVersionUID = 571419411573657796L;

            public void actionPerformed(ActionEvent e) {
                loadGraphString("V10:i0x10y10/i1x10y20/i2x30y20/i3x30y10/i4x0y25/i5x40y5/i6x40y25/i7x0y5/i8x20y10/i9x20y20/;E18:i0t0h4c2/i1t1h4c1/i2t2h5c2/i3t3h5c1/i4t2h6c1/i5t3h6c2/i6t4h6c1/i7t0h7c1/i8t1h7c2/i9t5h7c2/i10t0h8c1/i11t1h8c1/i12t2h8c2/i13t3h8c2/i14t0h9c2/i15t1h9c2/i16t2h9c1/i17t3h9c1/;");
            }
        });

        actionNamedGraph.add(new AbstractAction("B11,1 difficult (11 vertices)") {
            private static final long serialVersionUID = 571419411573657796L;

            public void actionPerformed(ActionEvent e) {
                loadGraphString("V11:i0x30y24/i1x0y24/i2x15y0/i3x25y17/i4x5y17/i5x15y20/i6x30y8/i7x0y8/i8x20y8/i9x15y30/i10x10y8/;E20:i0t0h5c1/i1t1h5c2/i2t0h6c2/i3t2h6c2/i4t3h6c1/i5t1h7c1/i6t2h7c1/i7t4h7c2/i8t2h8c2/i9t3h8c1/i10t4h8c1/i11t5h8c2/i12t0h9c2/i13t1h9c1/i14t3h9c2/i15t4h9c1/i16t2h10c1/i17t3h10c2/i18t4h10c2/i19t5h10c1/;");
            }
        });

        actionNamedGraph.add(new AbstractAction("B11,2 difficult (11 vertices)") {
            private static final long serialVersionUID = 571419411573657797L;

            public void actionPerformed(ActionEvent e) {
                loadGraphString("V11:i0x3y2/i1x1y2/i2x3y1/i3x1y1/i4x4y1/i5x0y1/i6x4y2/i7x0y2/i8x2y2/i9x2y0/i10x2y1/;E20:i0t0h4c1/i1t1h5c2/i2t0h6c2/i3t2h6c1/i4t4h6c2/i5t1h7c1/i6t3h7c2/i7t5h7c1/i8t0h8c2/i9t1h8c1/i10t2h8c1/i11t3h8c2/i12t2h9c2/i13t3h9c1/i14t4h9c2/i15t5h9c1/i16t0h10c1/i17t1h10c2/i18t2h10c2/i19t3h10c1/;");
            }
        });

        actionNamedGraph.add(new AbstractAction("B12,1 difficult (12 vertices)") {
            private static final long serialVersionUID = 571419411573657796L;

            public void actionPerformed(ActionEvent e) {
                loadGraphString("V12:i0x30y30/i1x10y30/i2x30y0/i3x10y0/i4x25y10/i5x20y20/i6x40y20/i7x0y20/i8x30y20/i9x10y20/i10x15y10/i11x20y0/;E22:i0t0h5c1/i1t1h5c2/i2t0h6c2/i3t2h6c1/i4t1h7c1/i5t3h7c2/i6t1h8c1/i7t2h8c2/i8t4h8c1/i9t6h8c2/i10t0h9c2/i11t3h9c1/i12t4h9c2/i13t7h9c1/i14t2h10c1/i15t3h10c1/i16t4h10c2/i17t5h10c2/i18t2h11c2/i19t3h11c2/i20t4h11c1/i21t5h11c1/;");
            }
        });

        actionNamedGraph.add(new AbstractAction("B12,2 difficult (12 vertices)") {
            private static final long serialVersionUID = 571419411573657796L;

            public void actionPerformed(ActionEvent e) {
                loadGraphString("V12:i0x10y0/i1x30y0/i2x30y20/i3x15y30/i4x12y40/i5x40y30/i6x0y10/i7x40y10/i8x25y30/i9x10y20/i10x0y30/i11x28y40/;E22:i0t0h5c1/i1t1h5c2/i2t0h6c1/i3t2h6c1/i4t1h7c2/i5t2h7c2/i6t2h8c1/i7t3h8c1/i8t4h8c2/i9t5h8c2/i10t1h9c1/i11t3h9c1/i12t4h9c2/i13t6h9c2/i14t0h10c2/i15t3h10c2/i16t4h10c1/i17t7h10c1/i18t2h11c2/i19t3h11c2/i20t4h11c1/i21t5h11c1/;");
            }
        });

        actionNamedGraph.add(new AbstractAction("B12,3 difficult (12 vertices)") {
            private static final long serialVersionUID = 571419411573657796L;

            public void actionPerformed(ActionEvent e) {
                loadGraphString("V12:i0x10y5/i1x10y25/i2x30y10/i3x30y20/i4x10y10/i5x10y20/i6x30y5/i7x30y25/i8x20y10/i9x3y15/i10x37y15/i11x20y20/;E22:i0t0h4c2/i1t1h5c1/i2t0h6c1/i3t2h6c2/i4t1h7c2/i5t3h7c1/i6t2h8c1/i7t3h8c2/i8t4h8c2/i9t5h8c1/i10t0h9c1/i11t1h9c2/i12t4h9c1/i13t5h9c2/i14t2h10c1/i15t3h10c2/i16t6h10c1/i17t7h10c2/i18t2h11c2/i19t3h11c1/i20t4h11c1/i21t5h11c2/;");
            }
        });

        actionNamedGraph.add(new AbstractAction("B12,4 difficult (12 vertices)") {
            private static final long serialVersionUID = 571419411573657796L;

            public void actionPerformed(ActionEvent e) {
                loadGraphString("V12:i0x10y10/i1x25y20/i2x40y10/i3x40y0/i4x10y0/i5x10y20/i6x50y10/i7x0y10/i8x40y20/i9x20y10/i10x30y10/i11x25y0/;E22:i0t0h5c1/i1t1h5c2/i2t2h6c1/i3t3h6c2/i4t0h7c2/i5t4h7c1/i6t5h7c2/i7t1h8c1/i8t2h8c2/i9t6h8c1/i10t0h9c1/i11t1h9c2/i12t3h9c1/i13t4h9c2/i14t1h10c1/i15t2h10c2/i16t3h10c1/i17t4h10c2/i18t0h11c2/i19t2h11c1/i20t3h11c2/i21t4h11c1/;");
            }
        });

        actionNamedGraph.add(new AbstractAction("3x3 K4 grid (16 vertices)") {
            private static final long serialVersionUID = 571719411573657796L;

            public void actionPerformed(ActionEvent e) {
                loadGraphString("V16:i0x0y0/i1x1y0/i2x2y0/i3x3y0/i4x0y1/i5x1y1/i6x2y1/i7x3y1/i8x0y2/i9x1y2/i10x2y2/i11x3y2/i12x0y3/i13x1y3/i14x2y3/i15x3y3/;E30:i0t0h1c1/i1t0h5c2/i2t0h4c2/i3t1h2c1/i4t1h4c2/i5t1h6c2/i6t2h3c2/i7t2h5c2/i8t2h7c1/i9t3h6c1/i10t3h7c1/i11t4h8c1/i12t4h9c1/i13t5h8c1/i14t5h10c1/i15t6h9c1/i16t6h11c2/i17t7h10c2/i18t7h11c2/i19t8h12c2/i20t8h13c2/i21t9h12c2/i22t9h14c2/i23t10h13c2/i24t10h15c1/i25t11h14c1/i26t11h15c1/i27t12h13c1/i28t13h14c1/i29t14h15c2/;");
            }
        });

        actionNamedGraph.add(new AbstractAction("B18,1 square-free (18 vertices)") {
            private static final long serialVersionUID = 571719411573657796L;

            public void actionPerformed(ActionEvent e) {
                loadGraphString("V18:i0x0y0/i1x100y0/i2x200y0/i3x300y0/i4x300y100/i5x300y200/i6x200y200/i7x100y200/i8x0y200/i9x0y100/i10x40y40/i11x150y25/i12x260y40/i13x260y160/i14x150y175/i15x40y160/i16x175y70/i17x125y130/;E34:i0t0h1c1/i1t1h2c2/i2t2h3c2/i3t3h4c1/i4t4h5c2/i5t5h6c1/i6t6h7c2/i7t7h8c1/i8t8h9c1/i9t9h0c2/i10t0h10c1/i11t3h12c1/i12t5h13c1/i13t8h15c2/i14t9h16c1/i15t4h17c2/i16t4h11c2/i17t9h14c2/i18t1h15c1/i19t1h17c1/i20t2h16c1/i21t2h13c2/i22t6h12c1/i23t6h16c2/i24t7h17c1/i25t7h10c2/i26t10h11c2/i27t10h13c2/i28t11h16c1/i29t11h15c2/i30t12h15c2/i31t12h14c2/i32t13h14c1/i33t14h17c1/;");
            }
        });
    }

    AbstractAction getActionNewGraphType() {
        return new AbstractAction(generateOnlyAtomic ? "Generate Composite or Atomic" : "Generate Only Atomic") {
            private static final long serialVersionUID = 571719711573657790L;

            public void actionPerformed(ActionEvent e) {
                generateOnlyAtomic = !generateOnlyAtomic;
            }
        };
    }

    public void addPopupActions(JPopupMenu popup) {

        popup.add(new AbstractAction("Center Graph") {
            private static final long serialVersionUID = 571719411574657791L;

            public void actionPerformed(ActionEvent e) {
                centerAndScaleGraph();
            }
        });

        popup.add(new AbstractAction("Relayout Graph") {
            private static final long serialVersionUID = 571719411573657791L;

            public void actionPerformed(ActionEvent e) {
                relayoutGraph();
            }
        });

        popup.add(new AbstractAction("Reset Board Colors") {
            private static final long serialVersionUID = 571719411573657796L;

            public void actionPerformed(ActionEvent e) {
                mGraph.updateOriginalColor();
                mTurnNum = 0;
                putLog("Resetting game graph's colors.");
                updateGraphMessage();
                mVV.repaint();
            }
        });

        popup.add(new AbstractAction(allowFreeExchange ? "Restrict to Unique Exchanges" : "Allow Free Edge Exchanges") {
            private static final long serialVersionUID = 571719411573657798L;

            public void actionPerformed(ActionEvent e) {
                allowFreeExchange = !allowFreeExchange;
                mVV.repaint();
            }
        });

        popup.add(new AbstractAction((mAutoPlayBob ? "Disable" : "Enable") + " Autoplay of Bob's Moves") {
            private static final long serialVersionUID = 571719413573657798L;

            public void actionPerformed(ActionEvent e) {
                mAutoPlayBob = !mAutoPlayBob;
            }
        });

        popup.addSeparator();

        JMenu newGraph = new JMenu("New Random Graph");
        for (int i = 0; i < actionRandomGraph.length; ++i) {
            if (actionRandomGraph[i] != null)
                newGraph.add(actionRandomGraph[i]);
        }
        newGraph.addSeparator();
        newGraph.add(getActionNewGraphType());
        popup.add(newGraph);

        JMenu newNamedGraph = new JMenu("New Named Graph");
        for (int i = 0; i < actionNamedGraph.size(); ++i) {
            if (actionNamedGraph.get(i) != null)
                newNamedGraph.add(actionNamedGraph.get(i));
        }
        popup.add(newNamedGraph);

        popup.add(new AbstractAction("Show GraphString") {
            private static final long serialVersionUID = 545719411573657792L;

            public void actionPerformed(ActionEvent e) {
                JEditorPane text = new JEditorPane("text/plain", GraphString.write_graph(mGraph, mVV.getModel().getGraphLayout()));
                text.setEditable(false);
                text.setPreferredSize(new Dimension(300, 125));
                JOptionPane.showMessageDialog(null, text, "GraphString Serialization", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        popup.add(new AbstractAction("Load GraphString") {
            private static final long serialVersionUID = 8636579131902717983L;

            public void actionPerformed(ActionEvent e) {
                String input = JOptionPane.showInputDialog(null, "Enter GraphString:", "");
                if (input == null)
                    return;
                loadGraphString(input);
            }
        });

        popup.add(new AbstractAction("Show graph6") {
            private static final long serialVersionUID = 571719411573657792L;

            public void actionPerformed(ActionEvent e) {
                JTextArea text = new JTextArea(Graph6.write_graph6(mGraph));
                JOptionPane.showMessageDialog(null, text, "graph6 Serialization", JOptionPane.INFORMATION_MESSAGE);
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
    }

    void makeNewRandomGraph(int numVertex) {
        MyGraph g;
        do {
            g = MyGraph.getRandomGraph(numVertex);
        } while (numVertex != 0 && generateOnlyAtomic && !g.isAtomicBispanner());
        setNewGraph(g);
    }

    void relayoutGraph() {
        final AbstractLayout<Integer, MyEdge> layout = MyGraphLayoutFactory(mGraph);
        mLayout = layout;
        mVV.setGraphLayout(layout);
        centerAndScaleGraph();
    }

    void updateGraphMessage() {
        String msg = "";

        int round = mTurnNum / 2 + 1;
        int min_rounds = mGraph.getEdgeCount() / 2;

        if (mGraph.getVertexCount() == 0) {
        }
        else if (mGraph.finishedEdges() != mGraph.getEdgeCount()) {
            msg = "Round " + round;
            msg += " of minimum " + min_rounds;
            msg += ", remaining edges: " + (mGraph.getEdgeCount() - mGraph.finishedEdges()) + ".";
        }
        else {
            if (round == min_rounds)
                msg = "Congratulations! You won in the minimum number of rounds! Terrific!";
            else {
                msg = "Congratulations! You won after " + round + " of minimum " + min_rounds + " rounds!";
                msg += " You needed " + (round - min_rounds) + " extra rounds.";
            }
        }

        mGraph.message = msg;
    }

    void centerAndScaleGraph() {

        // clear layout
        MultiLayerTransformer mlTransformer = mVV.getRenderContext().getMultiLayerTransformer();
        mlTransformer.setToIdentity();

        if (mGraph.getVertexCount() == 0)
            return;

        // calculate bounding box of layout
        double xMin = Double.POSITIVE_INFINITY;
        double yMin = Double.POSITIVE_INFINITY;
        double xMax = Double.NEGATIVE_INFINITY;
        double yMax = Double.NEGATIVE_INFINITY;

        for (Integer v : mGraph.getVertices()) {
            Point2D p = mLayout.transform(v);
            if (p.getX() < xMin)
                xMin = p.getX();
            if (p.getX() > xMax)
                xMax = p.getX();
            if (p.getY() < yMin)
                yMin = p.getY();
            if (p.getY() > yMax)
                yMax = p.getY();
        }

        System.err.println("xMin: " + xMin + " xMax: " + xMax + " yMin: " + yMin + " yMax: " + yMax);

        // shift and scale layout
        Dimension vv_size = mVV.getSize();
        System.err.println("vv_size: " + vv_size);

        double xSize = xMax - xMin;
        double ySize = yMax - yMin;

        double xRatio = vv_size.getWidth() / xSize;
        double yRatio = vv_size.getHeight() / ySize;
        double ratio = 0.75 * Math.min(xRatio, yRatio);

        System.err.println("ratio: " + ratio);

        mlTransformer.getTransformer(Layer.LAYOUT).scale(ratio, ratio, new Point2D.Double(0, 0));

        double xShift = -xMin + (vv_size.getWidth() / ratio - xSize) / 2.0;
        double yShift = -yMin + (vv_size.getHeight() / ratio - ySize) / 2.0;
        mlTransformer.getTransformer(Layer.LAYOUT).translate(xShift, yShift);
    }

    void setNewGraph(MyGraph g) {

        mGraph = g;
        mGraph.graphChanged();
        mHoverEdge = null;
        mMarkedge = null;
        mHaveCycle = false;
        mTurnNum = 0;
        mNextVertex = g.getVertexCount();

        putLog("Starting new game with " + mGraph.getEdgeCount() + " edges.");

        if (mGraph.getVertexCount() > 0 && mGraph.getVertexCount() <= 14) {
            if (mGraph.isAtomicBispanner()) {
                putLog("New graph is an atomic bispanning graph.");
            }
            else {
                putLog("New graph is a composite bispanning graph.");
            }
        }

        mGraph.calcUniqueExchanges();
        mGraph.updateOriginalColor();

        updateGraphMessage();

        if (mVV != null) {

            if (g.mInitialLayout != null)
                mLayout = g.mInitialLayout;
            else
                mLayout = MyGraphLayoutFactory(mGraph);

            mVV.setGraphLayout(mLayout);
            centerAndScaleGraph();
        }
    }

    public void loadGraphString(String input) {
        try {
            MyGraph g = GraphString.read_graph(input);
            setNewGraph(g);
        }
        catch (IOException e1) {
            JOptionPane.showMessageDialog(null, "Error in GraphString: " + e1, "GraphString", JOptionPane.INFORMATION_MESSAGE);
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

        // Create a container to hold the visualization
        Container container = new Container();
        container.addNotify();
        container.add(mVV);
        container.setVisible(true);
        container.paintComponents(graphics2d);

        // Dispose of the graphics and close the document
        graphics2d.dispose();
        document.close();

        // Put mVV back onto visible plane
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
        centerAndScaleGraph();
    }
}
