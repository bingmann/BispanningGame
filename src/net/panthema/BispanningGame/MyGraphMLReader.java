/*******************************************************************************
 * src/net/panthema/BispanningGame/MyGraphMLReader.java
 *
 * Read a graphML file written by this program.
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

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.io.GraphIOException;
import edu.uci.ics.jung.io.graphml.EdgeMetadata;
import edu.uci.ics.jung.io.graphml.GraphMLReader2;
import edu.uci.ics.jung.io.graphml.GraphMetadata;
import edu.uci.ics.jung.io.graphml.GraphMetadata.EdgeDefault;
import edu.uci.ics.jung.io.graphml.HyperEdgeMetadata;
import edu.uci.ics.jung.io.graphml.NodeMetadata;

class MyGraphMLReader implements Transformer<Integer, Point2D>
{
    MyGraph newGraph;

    Map<Integer, Point2D> posMap;

    MyGraphMLReader(javax.swing.JPanel panel) throws FileNotFoundException, GraphIOException {

        posMap = new TreeMap<Integer, Point2D>();

        // Query user for filename
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Specify GraphML file to read");
        chooser.setCurrentDirectory(new File("."));
        FileNameExtensionFilter filter = new FileNameExtensionFilter("GraphML File", "graphml");
        chooser.setFileFilter(filter);

        if (chooser.showOpenDialog(panel) != JFileChooser.APPROVE_OPTION)
            return;

        File infile = chooser.getSelectedFile();

        BufferedReader fileReader = new BufferedReader(new FileReader(infile));

        newGraph = new MyGraph();

        // create the graph transformer
        Transformer<GraphMetadata, MyGraph> graphTransformer = new Transformer<GraphMetadata, MyGraph>() {
            public MyGraph transform(GraphMetadata metadata) {
                assert (metadata.getEdgeDefault().equals(EdgeDefault.UNDIRECTED));
                return newGraph;
            }
        };

        // create the vertex transformer
        Transformer<NodeMetadata, Integer> vertexTransformer = new Transformer<NodeMetadata, Integer>() {
            public Integer transform(NodeMetadata metadata) {
                // create a new vertex
                Integer v = newGraph.getVertexCount();

                // save layout information
                if (metadata.getProperty("x") != null && metadata.getProperty("y") != null) {
                    double x = Double.parseDouble(metadata.getProperty("x"));
                    double y = Double.parseDouble(metadata.getProperty("y"));
                    posMap.put(v, new Point2D.Double(x, y));
                }

                newGraph.addVertex(v);
                return v;
            }
        };

        // create the edge transformer
        Transformer<EdgeMetadata, MyEdge> edgeTransformer = new Transformer<EdgeMetadata, MyEdge>() {
            public MyEdge transform(EdgeMetadata metadata) {
                MyEdge e = new MyEdge(newGraph.getEdgeCount());
                if (metadata.getProperty("color") != null)
                    e.color = Integer.parseInt(metadata.getProperty("color"));
                return e;
            }
        };

        // create the useless hyperedge transformer
        Transformer<HyperEdgeMetadata, MyEdge> hyperEdgeTransformer = new Transformer<HyperEdgeMetadata, MyEdge>() {
            public MyEdge transform(HyperEdgeMetadata metadata) {
                return null;
            }
        };

        // create the graphMLReader2
        GraphMLReader2<MyGraph, Integer, MyEdge> graphReader = new GraphMLReader2<MyGraph, Integer, MyEdge>(fileReader, graphTransformer, vertexTransformer, edgeTransformer, hyperEdgeTransformer);

        // Get the new graph object from the GraphML file
        graphReader.readGraph();
    }

    @Override
    public Point2D transform(Integer v) {
        return posMap.get(v);
    }
}