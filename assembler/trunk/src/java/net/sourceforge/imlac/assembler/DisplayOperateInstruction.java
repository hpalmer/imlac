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
 * This class represents an Imlac display processor operate instruction.
 * With the exceptions of DOPR, DHVS, DSTS, and DSTB, these instructions have
 * no operand.  DHVS, DSTS and DSTB take a constant operand of 0-3.  DOPR takes
 * a 4-bit constant operand, which typically is 014 or 015.
 * 
 * @author Howard Palmer
 * @version $Id$
 * @see Mnemonic
 */
public class DisplayOperateInstruction extends Mnemonic {

	/**
	 * While <code>dops</code> itself is never used, its elements are added to
	 * a hashtable in <code>Mnemonic</code> as they are created.
	 */
	@SuppressWarnings("unused")
	private static final DisplayOperateInstruction[] dops = {
		new DisplayOperateInstruction("DHLT", 0000000),	// Halt Display Processor
		new DisplayOperateInstruction("DHVH", 0002000),	// High Voltage Sync and Halt
		new DisplayOperateInstruction("DNOP", 0004000),	// No Operation
		new DisplayOperateInstruction("DOPR", 0004000),	// Generic display operation
		new DisplayOperateInstruction("DSTS", 0004004),	// Set Scale
		new DisplayOperateInstruction("DSTB", 0004010),	// Set Block 0 (8K machine only)
		
//		new DisplayOperateInstruction("????", 0004014),	// Desensitize Light Pen
//		new DisplayOperateInstruction("????", 0004015),	// Sensitize Light Pen

		new DisplayOperateInstruction("DDSP", 0004020),	// 2 usec intensification.
		new DisplayOperateInstruction("DRJM", 0004040),	// Return Jump, C(DT)=>C(DPC)
		new DisplayOperateInstruction("DDYM", 0004100),	// Decrement YAC msb
		new DisplayOperateInstruction("DDXM", 0004200),	// Decrement XAC msb
		new DisplayOperateInstruction("DIYM", 0004400),	// Increment YAC msb
		new DisplayOperateInstruction("DIXM", 0005000),	// Increment XAC msb
		new DisplayOperateInstruction("DHVC", 0006000),	// High Voltage Sync. and Continue.
		new DisplayOperateInstruction("DHVS", 0006004),	// DHVC|DSTS
		
	};
	
	private final int instruction;
	
	/**
	 * @param name
	 */
	protected DisplayOperateInstruction(String name, int instruction) {
		super(name);
		this.instruction = instruction;
	}

	public boolean parse(Parser parser, CharSource cs) {
		String name = toString();
		int mask = 0;
		
		// Set mask for operand, if any
		if (name.equals("DOPR")) {
			mask = 017;
		} else if (name.equals("DSTB") || name.equals("DSTS") || name.equals("DHVS")) {
			mask = 03;
		}
		
		if (mask != 0) {
			if (cs.skipBlanks()) {
				Expression exp = Expression.parse(parser, cs);
				if (exp != null) {
					parser.generateWord(
						Expression.makeMaskedInstruction(
							instruction,
							mask,
							exp));
				} else {
					parser.error("Missing operand for " + this);
				}
			} else {
				parser.error("Missing operand for " + this);
			}
		} else {
			parser.generateWord(instruction);
		}

		return true;
	}
	
	public int getInstruction() {
		return instruction;
	}
}
