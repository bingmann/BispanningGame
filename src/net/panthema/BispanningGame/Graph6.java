/*******************************************************************************
 * src/net/panthema/BispanningGame/Graph6.java
 *
 * Construct a MyGraph object from a string in graph6/sparse6 graph format.
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

class ByteReader6 {

	private byte[] mBytes;
	private int mSize, mPos, mBit;

	public ByteReader6(String s6) {
		mBytes = s6.getBytes();
		mSize = s6.length();
		mPos = mBit = 0;
	}

	// ! whether k bits are available
	boolean have_bits(int k) {
		return (mPos + (mBit + k - 1) / 6) < mSize;
	}

	// ! return the next integer encoded in graph6
	int get_number() {
		assert(mPos < mSize);

		byte c = mBytes[mPos];
		assert(c >= 63);
		c -= 63;
		++mPos;

		if (c < 126)
			return c;

		assert(false);
		return 0;
	}

	// ! return the next bit encoded in graph6
	int get_bit() {
		assert(mPos < mSize);

		byte c = mBytes[mPos];
		assert(c >= 63);
		c -= 63;
		c >>= (5 - mBit);

		mBit++;
		if (mBit == 6) {
			mPos++;
			mBit = 0;
		}

		return (c & 0x01);
	}

	// ! return the next bits as an integer
	int get_bits(int k) {
		int v = 0;

		for (int i = 0; i < k; ++i) {
			v *= 2;
			v += get_bit();
		}

		return v;
	}
}

public class Graph6 {
	
	public static MyGraph read_sparse6(String str) {
		
		ByteReader6 br6 = new ByteReader6(str);

		int numVertex = br6.get_number();
		int k = (int) Math.ceil(Math.log(numVertex) / Math.log(2));

		MyGraph g = new MyGraph();

		for (int i = 0; i < numVertex; ++i)
			g.addVertex(i);

		int v = 0, numEdge = 0;

		while (br6.have_bits(1 + k)) {
			int b = br6.get_bit();
			int x = br6.get_bits(k);

			if (x >= numVertex)
				break;

			if (b != 0)
				v = v + 1;
			if (v >= numVertex)
				break;

			if (x > v)
				v = x;
			else {
				//System.out.println("add edge " + x + " - " + v + "!");
				g.addEdge(new MyEdge(numEdge++), x, v);
			}
		}

		return g;
	}

	public static MyGraph read_graph6(String str) {
		if (str.charAt(0) == ':')
			return read_sparse6(str.substring(1));

		ByteReader6 br6 = new ByteReader6(str);
		int n = br6.get_number();

		MyGraph g = new MyGraph();

		int numEdge = 0;

		for (int j = 1; j < n; ++j) {
			for (int i = 0; i < j; ++i) {
				int e = br6.get_bit();
				if (e != 0) {
					g.addEdge(new MyEdge(numEdge++), i, j);
				}
			}
		}
		return g;
	}
}
