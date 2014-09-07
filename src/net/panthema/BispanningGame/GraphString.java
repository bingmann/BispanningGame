/*******************************************************************************
 * src/net/panthema/BispanningGame/GraphString.java
 *
 * Serialize and deserialize a MyGraph object from a string.
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.StaticLayout;

class GraphString
{
    public static String write_graph(MyGraph g, Transformer<Integer, Point2D> gl) {
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(ba);

        // print the vertex list

        pw.print('V');
        pw.print(g.getVertexCount());
        pw.print(':');

        Collection<Integer> vcoll = g.getVertices();
        ArrayList<Integer> vlist = new ArrayList<Integer>(vcoll);
        Collections.sort(vlist, new Comparator<Integer>() {
            public int compare(Integer arg0, Integer arg1) {
                return arg0 - arg1;
            }
        });

        for (Integer v : vlist) {
            pw.print('i');
            pw.print(v);

            Point2D pos = gl.transform(v);
            pw.print('x');
            pw.print((int) pos.getX());
            pw.print('y');
            pw.print((int) pos.getY());

            pw.print('/');
        }
        pw.print(';');

        // print the edge list

        pw.print('E');
        pw.print(g.getEdgeCount());
        pw.print(':');

        Collection<MyEdge> ecoll = g.getEdges();
        ArrayList<MyEdge> elist = new ArrayList<MyEdge>(ecoll);
        Collections.sort(elist, new Comparator<MyEdge>() {
            public int compare(MyEdge arg0, MyEdge arg1) {
                return arg0.id - arg1.id;
            }
        });

        for (MyEdge e : elist) {
            Integer e_x = g.getEndpoints(e).getFirst();
            Integer e_y = g.getEndpoints(e).getSecond();

            pw.print('i');
            pw.print(e.id);

            pw.print('t');
            pw.print(e_x);
            pw.print('h');
            pw.print(e_y);

            pw.print('c');
            pw.print(e.color);

            pw.print('/');
        }
        pw.print(';');

        pw.flush();

        return ba.toString();
    }

    static int readInt(PushbackReader pr) throws IOException {
        int c;
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        while (Character.isDigit(c = pr.read())) {
            ba.write(c);
        }
        pr.unread(c);
        try {
            return Integer.parseInt(ba.toString());
        }
        catch (NumberFormatException e) {
            throw (new IOException("Error in Graph String: integer format error"));
        }
    }

    public static MyGraph read_graph(String str) throws IOException {

        StringReader sr = new StringReader(str);
        PushbackReader pr = new PushbackReader(sr);

        MyGraph g = new MyGraph();

        if (pr.read() != 'V')
            throw (new IOException("Error in Graph String: format error"));

        int Vnum = readInt(pr);

        int Vdesc = pr.read();

        if (Vdesc == ':') {
            // read vertex list with positions
            for (int i = 0; i < Vnum; ++i) {

                if (pr.read() != 'i')
                    throw (new IOException("Error in Graph String: format error"));

                Integer v = readInt(pr);
                g.addVertex(v);

                Double posx = null, posy = null;

                for (int p = pr.read(); p != '/'; p = pr.read()) {

                    if (p == 'x') {
                        posx = (double) readInt(pr);
                    }
                    else if (p == 'y') {
                        posy = (double) readInt(pr);
                    }
                    else {
                        throw (new IOException("Error in Graph String: unknown attribute"));
                    }
                }

                if (posx != null && posy != null) {
                    if (g.mInitialLayout == null)
                        g.mInitialLayout = new StaticLayout<Integer, MyEdge>(g);

                    g.mInitialLayout.setLocation(v, posx, posy);
                }
            }

            // read terminator of vertex list
            if (pr.read() != ';')
                throw (new IOException("Error in Graph String: format error"));
        }
        else if (Vdesc == ';') {
            // read anonymous vertex list
            for (int i = 0; i < Vnum; ++i) {
                g.addVertex(i);
            }
        }
        else {
            throw (new IOException("Error in Graph String: format error"));
        }

        // read edge list

        if (pr.read() != 'E')
            throw (new IOException("Error in Graph String: format error"));

        int Enum = readInt(pr);

        if (pr.read() != ':')
            throw (new IOException("Error in Graph String: format error"));

        for (int i = 0; i < Enum; ++i) {

            if (pr.read() != 'i')
                throw (new IOException("Error in Graph String: format error"));

            Integer ei = readInt(pr);
            MyEdge e = new MyEdge(ei);
            Integer tail = null, head = null;

            for (int p = pr.read(); p != '/'; p = pr.read()) {

                if (p == 't') {
                    tail = readInt(pr);
                }
                else if (p == 'h') {
                    head = readInt(pr);
                }
                else if (p == 'c') {
                    e.color = readInt(pr);
                }
                else {
                    throw (new IOException("Error in Graph String: unknown attribute"));
                }
            }

            if (tail == null || head == null)
                throw (new IOException("Error in Graph String: missing tail/head attributes"));

            g.addEdge(e, tail, head);
        }

        if (pr.read() != ';')
            throw (new IOException("Error in Graph String: format error"));

        return g;
    }
}
