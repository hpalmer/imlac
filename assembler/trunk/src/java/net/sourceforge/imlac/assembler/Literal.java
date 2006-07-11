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

/**
 * This class represents a literal in Imlac assembly language.  Literal
 * text is stored literally as the symbol name.  When it is time for
 * literal to be generated, the CharSource interface provides access to
 * the literal text. 
 * 
 * @author Howard Palmer
 * @version $Id$
 * @see Symbol
 * @see CharSource
 */
public class Literal extends CharSource implements Symbol {
	
	private final String text;
	private int iValue;
	private boolean defined;
	private boolean reloc;

	public Literal(String text) {
		super();
		this.text = text;
		iValue = 0;
		reloc = false;
		defined = false;
	}
	
	// CharSource interface
	public int length() {
		return text.length();
	}

	public char charAt(int index) {
		return text.charAt(index);
	}

	public CharSequence subSequence(int start, int end) {
		return text.subSequence(start, end);
	}

	public String toString() {
		return text;
	}

	// Symbol interface
	public boolean isDefined() {
		return defined;
	}

	public boolean isRelocatable() {
		return reloc;
	}

	public boolean isConstant() {
		return false;
	}
	
	public int intValue() {
		return iValue;
	}

	public void define(int value, boolean isRelocatable, boolean isConstant) {
		iValue = value;
		reloc = isRelocatable;
		defined = true;
		if (isConstant) {
			throw new RuntimeException("Literal: attempt to define constant literal");
		}
	}
}
