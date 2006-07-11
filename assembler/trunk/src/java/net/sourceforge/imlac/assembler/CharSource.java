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
package net.sourceforge.imlac.assembler;


import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class adds a current position and related functions to the
 * CharSequence interface.  Subclasses must implement CharSequence.
 * 
 * @author Howard Palmer
 * @version $Id$
 * @see java.lang.CharSequence
 */
public abstract class CharSource implements CharSequence {
	// Pattern for skipping blanks
	private static final Pattern patBlank = Pattern.compile("([\\p{Blank}]+)");
	

	private int pos;
	private final Stack<Integer> pStack = new Stack<Integer>();
	
	protected CharSource() {
		super();
	}
	
	// Get the next character without advancing the position
	// Return 0 if there is no next character
	public char peekChar() {
		char ch = 0;
		if (this.length() > 0) {
			ch = this.charAt(0);
		}
		return ch;
	}
	
	// Get the next character and advance the position past it
	// Return 0 if there is no next character
	public char nextChar() {
		char ch = 0;
		if (this.length() > 0) {
			ch = this.charAt(0);
			++pos;
		}
		return ch;
	}
	
	// Return true if a character is available at the current position
	public boolean isMore() {
		return (this.length() > 0);
	}
	
	// Get the current position (0..length-1)
	public int getPosition() {
		return pos;
	}
	
	// Set the current position
//	protected int setPosition(int pos) {
//		int oldpos = this.pos;
//		this.pos = pos;
//		return oldpos;
//	}

	// Reset current position
	public int reset() {
		int oldpos = pos;
		pos = 0;
		pStack.clear();
		return oldpos;
	}
	
	// Advance the current position
	public int advance(int len) {
		int oldpos = pos;
		pos += len;
		return oldpos;
	}
	
	public void push() {
		pStack.push(pos);
	}
	
	public void pop(boolean discard) {
		Integer p = pStack.pop();
		if (!discard) {
			if (p != null) {
				pos = p.intValue();
			} else {
				reset();
			}
		}
	}
	
	public boolean skipBlanks() {
		boolean ok = false;
		Matcher mBlank = patBlank.matcher(this);
		if (mBlank.lookingAt()) {
			advance(mBlank.end());
			ok = true;
		}
		
		return ok;
	}
	
	// CharSequence interface
	public abstract int length();
	public abstract char charAt(int index);
	public abstract CharSequence subSequence(int start, int end);
	public abstract String toString();
}
