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
 * This class represent Imlac main processor shift instructions.  These
 * instructions take a single operand, which is a shift count of 0-3.
 * 
 * @author Howard Palmer
 * @version $Id$
 * @see Mnemonic
 */
public class ShiftInstruction extends Mnemonic {

	/**
	 * While <code>shifts</code> itself is never used, its elements are added to
	 * a hashtable in <code>Mnemonic</code> as they are created.
	 */
	@SuppressWarnings("unused")
	private static final ShiftInstruction[] shifts = {
		new ShiftInstruction("RAL", 0003000), // ROTATE AC left, N positions
		new ShiftInstruction("RAR", 0003020), // ROTATE AC right, N positions
		new ShiftInstruction("SAL", 0003040), // Shift AC left, N positions
		new ShiftInstruction("SAR", 0003060), // Shift AC right, N positions
		
	};
	
	private final int instruction;
	
	/**
	 * @param name
	 */
	protected ShiftInstruction(String name, int instruction) {
		super(name);
		this.instruction = instruction;
	}

	public boolean parse(Parser parser, CharSource cs) {
		boolean ok = false;
		String s = parser.getLeadingIndirectTag();
		boolean indirect = false;
		
		if (s != null) {
			if (s.compareToIgnoreCase("D") == 0) {
				parser.error("Assembler logic error 1 in SkipInstruction");
			} else if (s.compareToIgnoreCase("I") == 0) {
				indirect = true;
			}
		}
		
		cs.skipBlanks();
		
		if (parser.parseOperandIndirect(cs)) {
			indirect = true;
		}
		
		if (indirect) {
			parser.error("Indirect mode is not compatible with " + this);
		}
		
		Expression exp = Expression.parse(parser, cs);
		parser.generateWord(Expression.makeMaskedInstruction(instruction, 3, exp));
		ok = true;
		
		return ok;
	}
}
