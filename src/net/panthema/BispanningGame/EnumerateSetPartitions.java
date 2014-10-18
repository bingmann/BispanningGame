/*******************************************************************************
 * src/net/panthema/BispanningGame/EnumerateSetPartitions.java
 *
 * Call a functional object for all set partitions of {0,...,n-1}, providing
 * identification via a std::vector<size_t>. The enumeration is ended if the
 * functional returns false.
 * 
 * Mostly borrowed from
 * http://compprog.wordpress.com/2007/10/15/generating-the-partitions-of-a-set/
 * but massively fixed and changed to 0..n-1.
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

interface SetPartitionFunctor
{
    // called with const array of partition numbers for each item of the n-set
    boolean partition(final int[] s);
}

class EnumerateSetPartitions
{
    // the size of the base set
    protected int n;

    // s[i] is the number of the set in which the ith element should go
    protected int[] s;
    // m[i] is the largest of the first i elements in s
    protected int[] m;

    EnumerateSetPartitions(int _n) {
        n = _n;

        s = new int[n];
        m = new int[n];
    }

    boolean enumerate(SetPartitionFunctor functor) {
        // 0 0 0 0 is the first way to partition a set is to put all the
        // elements in the same subset.

        // Output the first partitioning.
        if (!functor.partition(s))
            return false;

        // Print the other partitioning schemes.
        while (true) {
            // Update s: 0 0 0 0 -> 1 0 0 0 -> 0 1 0 0 -> 1 1 0 0 -> 2 1 0 0 ->
            // 0 0 1 0 ...

            int i = 0;
            ++s[i];
            while ((i < n - 1) && (s[i] > m[i + 1] + 1)) {
                s[i] = 0;
                ++i;
                ++s[i];
            }

            // If i is has reached n-1 th element, then the last unique
            // partitiong has been found
            if (i == n - 1)
                break;

            // Because all the first i elements are now 1, s[i] (i + 1 th
            // element) is the largest. So we update max by copying it to all
            // the first i positions in m.
            if (s[i] > m[i])
                m[i] = s[i];

            int j = i;
            while (j > 0)
                m[--j] = m[i];

            if (!functor.partition(s))
                return false;
        }
        return true;
    }
}
