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
 * This class represents Imlac display processor orders.  All of them
 * take a single operand, though the nature of the operand differs among
 * them.  DLXA and DLYA take a 10-bit coordinate value.  DEIM is used to
 * enter increment mode, and takes the first 8-bit increment byte as its
 * operand.  DJMS and DJMP take a 12-bit memory address as their operand.
 * 
 * @author Howard Palmer
 * @version $Id$
 * @see Mnemonic
 */
public class DisplayOrder extends Mnemonic {

	/**
	 * While <code>orders</code> itself is never used, its elements are added to
	 * a hashtable in <code>Mnemonic</code> as they are created.
	 */
	@SuppressWarnings("unused")
	private static final DisplayOrder[] orders = {
		new DisplayOrder("DLXA", 0010000), // N=>C(XAC)
		new DisplayOrder("DLYA", 0020000), // N=>C(YAC)
		new DisplayOrder("DEIM", 0030000), // Increment Mode; N is first byte.
		new DisplayOrder("DJMS", 0050000), // C(DPC)+1=>C(DT), C(Q)=>C(DPC)
		new DisplayOrder("DJMP", 0060000), // Q=>C(DPC)
		
	};
	
	private final int instruction;
	
	/**
	 * @param name
	 */
	protected DisplayOrder(String name, int instruction) {
		super(name);
		this.instruction = instruction;
	}

	public boolean parse(Parser parser, CharSource cs) {
		String s = parser.getLeadingIndirectTag();
		boolean indirect = false;
		
		if (s != null) {
			if (s.compareToIgnoreCase("I") == 0) {
				indirect = true;
			} else if (s.compareToIgnoreCase("D") == 0) {
				parser.error("Assembler logic error 1 in DisplayOrder");
			}
		}
		
		// TODO:
		//		Handle "I DJMS foo" format
		//		Handle "D JMP bar" format
		
		if (cs.skipBlanks()) {
			if (instruction != 030000 /* DEIM */
				) {
				Expression exp = Expression.parse(parser, cs);
				if (exp != null) {
					if (instruction < 030000 /* DLXA, DLYA */
						) {
						parser.generateWord(
							Expression.makeMaskedInstruction(
								instruction,
								01777,
								exp));
					} else {
						parser.generateWord(
							Expression.makeAddr12Instruction(
								parser.getCurrentLocation(),
								instruction,
								exp));
					}
				} else {
					parser.error("Missing operand for " + this);
				}
			} else {
				// TODO:
				// Support DEIM
			}
		} else {
			parser.error("Missing operand for " + this);
		}

		return true;
	}
}
