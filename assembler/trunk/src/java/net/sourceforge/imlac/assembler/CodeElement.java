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
 * This class represents an Imlac code element.  Each element has an
 * associated load address, which may be absolute or relative.  Each
 * also has a value, part of which may be stored as a reference to an
 * unevaluated expression during the first assembly phase.  The value
 * is finalized as an integer during the second phase.  Each element
 * also contains the line number of the source line that generated it.
 * 
 * @author Howard Palmer
 * @version $Id$
 */
public class CodeElement {

	private Expression expValue;
	private int intValue;
	private int lineNumber;
	private int location;
	
	/**
	 * 
	 */
	public CodeElement() {
		super();
		expValue = null;
		intValue = 0;
		lineNumber = 0;
		location = 0;
	}
	
	public CodeElement(int line, int location, int value) {
		super();
		expValue = null;
		intValue = value;
		lineNumber = line;
		this.location = location;
	}
	
	public CodeElement(int line, int location, Expression exp) {
		super();
		expValue = exp;
		lineNumber = line;
		this.location = location;
	}

	public int getLocation() {
		return location;
	}
	
	public int getLineNumber() {
		return lineNumber;
	}
	
	public Integer getValue(Parser parser) {
		Integer result = null;
		if (expValue == null) {
			result = new Integer(intValue);
		} else {
			result = expValue.evaluate(parser);
		}
		
		return result;
	}
}
