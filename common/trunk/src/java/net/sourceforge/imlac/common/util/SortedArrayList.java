/*
 * Copyright © 2004, 2005, 2006 by Howard Palmer.  All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.sourceforge.imlac.common.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * This class provides a sorted <code>ArrayList</code>, with the order
 * determined by a <code>Comparator</code> that is specified when the
 * <code>SortedArrayList</code> is created.
 * 
 * @author Howard Palmer
 * @version $Id: SortedArrayList.java 145 2006-07-05 09:20:28Z Howard $
 * @see java.util.ArrayList#ArrayList
 */
public class SortedArrayList<E> extends ArrayList<E> {

	private static final long serialVersionUID = -4227524469047861415L;
	private final Comparator<E> cmp;
	private final Comparator<E> rcmp;

	/**
	 * Creates a <code>SortedArrayList</code>.  The order of elements
	 * subsequently added to the <code>SortedArrayList</code> is determined
	 * by the <code>Comparator</code> argument to this constructor.
	 * 
	 * @param c		a <code>Comparator</code> for the type of elements that
	 * 				the list will contain
	 * @see java.util.ArrayList#ArrayList()
	 * @see java.util.Comparator
	 */
	public SortedArrayList(Comparator<E> c) {
		super();
		this.cmp = c;
		this.rcmp = new Comparator<E>() {
			public int compare(E o1, E o2) {
				return cmp.compare(o2, o1);
			}
		};
	}

	/**
	 * Creates a <code>SortedArrayList</code>.  The order of elements
	 * subsequently added to the <code>SortedArrayList</code> is determined
	 * by the <code>Comparator</code> argument to this constructor.
	 * 
	 * @param c		a <code>Comparator</code> for the type of elements that
	 * 				the list will contain
	 * @param size	the initial size of the list
	 * @see java.util.ArrayList ArrayList#ArrayList(int)
	 * @see java.util.Comparator
	 */
	public SortedArrayList(Comparator<E> c, int size) {
		super(size);
		this.cmp = c;
		this.rcmp = new Comparator<E>() {
			public int compare(E o1, E o2) {
				return cmp.compare(o2, o1);
			}
		};
	}

	/**
	 * Adds an element to this list, maintaining the list in descending
	 * order.  All elements must be added using <code>addDescending</code>
	 * if the descending order is to be maintained.
	 * 
	 * @param o		the element to be added to the list
	 * @return		the position at which the element was added
	 * @see java.util.ArrayList#add(int, Object)
	 */
	public int addDescending(E o) {
		int i = Collections.binarySearch(this, o, rcmp);
		if (i < 0) {
			i = -1 - i;
		}
		add(i, o);
		return i;
	}

	/**
	 * Adds an element to this list, maintaining the list in ascending
	 * order.  All elements must be added using <code>addAscending</code>
	 * if the ascending order is to be maintained.
	 * 
	 * @param o		the element to be added to the list
	 * @return		the position at which the element was added
	 * @see java.util.ArrayList#add(int, Object)
	 */
	public int addAscending(E o) {
		int i = Collections.binarySearch(this, o, cmp);
		if (i < 0) {
			i = -1 - i;
		}
		add(i, o);
		return i;
	}
}
