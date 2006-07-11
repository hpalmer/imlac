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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents a Label in Imlac assembly language.  A Label
 * has an integer value, may be defined or undefined, and relocatable
 * or not.  Labels encountered in the label position of a source statement
 * are defined at once.  Labels in operands that are forward references
 * are created when first encountered, but not defined until later.
 * 
 * @author Howard Palmer
 * @version $Id$
 * @see Symbol
 */
public class Label implements Symbol {

	// Imlac HS Assembler label, e.g. X or X2, limit 2 characters.
	// Definition must start in first column, terminated by space.
	private static final Pattern patImlacLabel =
		Pattern.compile("([A-Z][0-7]?)");
	
	// NASA Ames Relasm label (also covers older Absasm).
	// Definition must start in first column, terminated by space.
	private static final Pattern patAmesLabel =
		Pattern.compile("([\\p{Alpha}$_]+[\\p{Alnum}[$_]]*)");
	
	// Midas label: first form must contain at least one of [A-Z], '$', or '%'.
	// Need not start in first column, as it is colon-terminated.
	private static final Pattern patColonLabel1 =
		Pattern.compile(
			"([\\p{Blank}]*)([\\p{Alnum}$%.]*[\\p{Alpha}$%]+[\\p{Alnum}$%.]*)");

	// Midas label: second form must contain at least two '.'
	private static final Pattern patColonLabel2 =
		Pattern.compile(
			"([\\p{Blank}]*)([\\p{Alnum}$%.]*[.]+[\\p{Alnum}$%.]*[.]+[\\p{Alnum}$%.]*)");

	// Label state information
	private static boolean seenImlacLabel = false;
	private static boolean seenAmesLabel = false;
	private static boolean seenColonLabel = false;
	private static String lastLabel = null;
	
	/**
	 * This is used to parse a label definition or reference, as it does not
	 * care what terminates the label.  If a label is found, it returns true,
	 * and the label can be retrieved by a call to getLastLabel().  In some
	 * styles the label must start at the beginning of the character source.
	 *
	 * @param cs character source to be parsed
	 * @return true if label found
	 */
	public static boolean parse(int style, CharSource cs, boolean inOperand) {
		boolean ok = false;
		int len = 0;
		
		// Check for a possible label in several different styles
		String label = null;
		
		switch (style) {
			case Asm.STYLE_IMLACHS:
				// Imlac HS Assembler
				if (inOperand || (cs.getPosition() == 0)) {
					Matcher matcher = patImlacLabel.matcher(cs);
					if (matcher.lookingAt()) {
						label = matcher.group(1);
						len = matcher.end(1);
						seenImlacLabel = true;
						ok = true;
					}
				}
				break;
			case Asm.STYLE_OLDAMES:
			case Asm.STYLE_NEWAMES:
				// NASA Ames Absasm or Relasm
				if (inOperand || (cs.getPosition() == 0)) {
					Matcher matcher = patAmesLabel.matcher(cs);
					if (matcher.lookingAt()) {
						label = matcher.group(1);
						len = matcher.end(1);
						seenAmesLabel = true;
						ok = true;
					}
				}
				break;
			case Asm.STYLE_MIDAS:
				// MIT Midas
				boolean gotit = false;
				Matcher matcher = patColonLabel1.matcher(cs);
				if (matcher.lookingAt()) {
					gotit = true;
				} else {
					matcher = patColonLabel2.matcher(cs);
					if (matcher.lookingAt()) {
						gotit = true;
					}
				}
				if (gotit) {
					label = matcher.group(2);
					len = matcher.end(2);
					ok = true;
					if (!inOperand) {
						cs.advance(len);

						// Midas labels need at least one colon to terminate
						// a definition.
						if (cs.peekChar() == ':') {
							cs.advance(1);
							len = 0;
							if (cs.peekChar() == ':') {
								len = 1;
							}
							seenColonLabel = true;
						} else {
							// This will also catch label=value or label==value,
							// so those will need to be parsed separately, with
							// inOperand = false.
							len = -len;
							label = null;
							ok = false;
						}
					}
				}
				break;
			default:
				System.out.println("Unsupported style in parseLabel");
				System.exit(-1);
		}
		
		if (ok) {
			lastLabel = label;
		}
		
		cs.advance(len);
		return ok;
	}
	
	/**
	 * This returns the last label parsed by parse().
	 * 
	 * @return label string
	 */
	public static String getLastLabel() {
		return lastLabel;
	}
	
	public static boolean isSeenImlacLabel() {
		return seenImlacLabel;
	}
	
	public static boolean isSeenAmesLabel() {
		return seenAmesLabel;
	}
	
	public static boolean isSeenColonLabel() {
		return seenColonLabel;
	}
	
	final String name;
	int iValue;
	boolean defined;
	boolean reloc;
	boolean constant;

	public Label(String name) {
		super();
		this.name = name;
		iValue = 0;
		defined = false;
		reloc = false;
		constant = false;
	}
	
	public Label(String name, int value, boolean isRelocatable) {
		super();
		this.name = name;
		iValue = value;
		defined = true;
		reloc = isRelocatable;
		constant = false;
	}
	
	// Override Object functions
	public boolean equals(Object obj) {
		boolean result = false;
		if (obj instanceof Label) {
			Label s = (Label) obj;
			result = (name.equals(s.name));
		}
		return result;
	}
	
	public int hashCode() {
		return name.hashCode();
	}

	public String toString() {
		return name;
	}
	
	// Symbol interface
	public int intValue() {
		return iValue;
	}
	
	public boolean isRelocatable() {
		return reloc;
	}
	
	public boolean isConstant() {
		return constant;
	}
	
	public boolean isDefined() {
		return defined;
	}
	
	public void define(int value, boolean isRelocatable, boolean isConstant) {
		iValue = value;
		reloc = isRelocatable;
		constant = isConstant;
		defined = true;
	}	
}
