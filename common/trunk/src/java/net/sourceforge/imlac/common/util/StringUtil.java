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

/**
 * This class contains static member functions which provide various
 * operations on <code>String</code>s.
 * 
 * @author Howard Palmer
 * @version $Id: StringUtil.java 145 2006-07-05 09:20:28Z Howard $
 */
public class StringUtil {

	/**
	 * Count the number of occurrences of a specified character in a given
	 * <code>String</code>.
	 * 
	 * @param s		the string
	 * @param c		the character
	 * @return		the number of occurrences of the character in the string
	 */
	public static int countOf(String s, char c) {
		int count = 0;
		for (int i = 0; true; ++i) {
			i = s.indexOf(i, c);
			if (i < 0) break;
			++count;
		}
		return count;
	}
	
	/**
	 * Expands tab characters in a specified <code>String</code>, using a
	 * given tab width.  For example, a tab width of 8 would put tabs in
	 * columns 9, 17, 25, 33, 41, 49, etc., where column 1 is the first
	 * character of the string.
	 * 
	 * @param s			the <code>String</code> that possibly contains
	 * 					tab characters
	 * @param tabWidth	the tab width
	 * @return			a <code>String</code> with any tabs expanded
	 */
	public static String expandTabs(String s, int tabWidth) {
		StringBuffer sb = new StringBuffer(s);
		int tabTo = tabWidth;
		for (int i = 0; i < sb.length(); ++i) {
			if (sb.charAt(i) == '\t') {

				// Get next tab position
				while (tabTo <= i)
					tabTo += tabWidth;

				// Replace the tab with space
				sb.setCharAt(i, ' ');
				++i;

				// Insert more spaces out to next tab position
				for (int j = (tabTo - i); j > 0; --j) {
					sb.insert(i - 1, ' ');
				}
				i = tabTo - 1;
			}
		}
		return sb.toString();
	}
	
	/**
	 * Expands tab characters in a specified <code>String</code>, using a
	 * given array of tab positions.  Tab positions are numbered starting
	 * from one, not zero.
	 * 
	 * @param s			the string
	 * @param tabList	the array of tab positions, in ascending order
	 * @return			the tab-expanded string
	 */
	public static String expandTabs(String s, int[] tabList) {
		StringBuffer sb = new StringBuffer(s);
		return expandTabs(sb, tabList);
	}
	
	/**
	 * Expands tab characters in a specified <code>StringBuffer</code>, using a
	 * given array of tab positions.  Tab positions are numbered starting
	 * from one, not zero.
	 * 
	 * @param sb		the input <code>StringBuffer</code>
	 * @param tabList	the array of tab positions, in ascending order
	 * @return			the tab-expanded <code>String</code>
	 */
	public static String expandTabs(StringBuffer sb, int[] tabList) {
		int tabIndex = 0;
		for (int i = 0; i < sb.length(); ++i) {
			if (sb.charAt(i) == '\t') {
				int tabTo = -1;
				
				// Find the first tab stop beyond the current position
				for (; (tabIndex < tabList.length); ++tabIndex) {
					int ntab = tabList[tabIndex] - 1;
					if (ntab > i) {
						tabTo = ntab;
						break;
					}
				}
				
				// Replace the tab with space
				sb.setCharAt(i, ' ');
				++i;

				// Insert more spaces out to next tab position, if any
				for (int j = (tabTo - i); j > 0; --j) {
					sb.insert(i - 1, ' ');
				}
				i = tabTo - 1;
			}
		}
		return sb.toString();
	}
	
	/**
	 * Not used.
	 */
	private StringUtil() {
		super();
	}

}
