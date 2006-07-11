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
 * This class represents an Imlac processor order instruction.  These
 * instructions, with the exception of LAW and LWC, take a single 11-bit
 * address as an operand, and may specify indirect addressing.  For LAW
 * and LWC, the 11-bit value is simply a constant, and indirect addressing
 * is not allowed.
 * 
 * @author Howard Palmer
 * @version $Id$
 * @see Mnemonic
 */
public class ProcessorOrder extends Mnemonic {

	/**
	 * While <code>orders</code> itself is never used, its elements are added to
	 * a hashtable in <code>Mnemonic</code> as they are created.
	 */
	@SuppressWarnings("unused")
	private static final ProcessorOrder[] orders = {
		new ProcessorOrder("LAW", 0004000), // N=>C(AC)
		new ProcessorOrder("JMP", 0010000), // Q=>C(PC)
		new ProcessorOrder("DAC", 0020000), // C(AC)=>C(Q)
		new ProcessorOrder("XAM", 0024000), // C(AC)=>C(Q); C(Q) => C(AC)
		new ProcessorOrder("ISZ", 0030000), // C(Q)+1=>C(Q), if C(Q)=0, C(PC)+1=>C(PC)
		new ProcessorOrder("JMS", 0034000), // C(PC)+1=>C(Q), Q+1=>C(PC)
		new ProcessorOrder("AND", 0044000), // Logical AND AC with C(Q)
		new ProcessorOrder("IOR", 0050000), // Inclusive OR AC with C(Q)
		new ProcessorOrder("XOR", 0054000), // Exclusive OR AC with C(Q)
		new ProcessorOrder("LAC", 0060000), // C(Q)=>C(AC)
		new ProcessorOrder("ADD", 0064000), // C(AC)+C(Q)=>C(AC) (2's Complement)
		new ProcessorOrder("SUB", 0070000), // C(AC)-C(Q)=>C(AC) (2's Complement)
		new ProcessorOrder("SAM", 0074000), // If C(AC)=C(Q), then C(PC)+1=>C(PC)
		new ProcessorOrder("LWC", 0104000), // -N=>C(AC)
		
	};
	
	private final int instruction;
	private int combined;
	
	/**
	 * @param name
	 */
	protected ProcessorOrder(String name, int instruction) {
		super(name);
		this.instruction = instruction;
	}

	private boolean isLawOrLwc() {
		String s = this.toString();
		return ("LAW".equals(s) || "LWC".equals(s));
	}
	
	public boolean parse(Parser parser, CharSource cs) {
		boolean ok = false;
		String s = parser.getLeadingIndirectTag();
		boolean indirect = false;
		combined = instruction;
		
		if (s != null) {
			if (s.compareToIgnoreCase("D") == 0) {
				parser.error("Assembler logic error 1 in ProcessorOrder");
			} else if (s.compareToIgnoreCase("I") == 0) {
				indirect = true;
			}
		}
		
		cs.skipBlanks();
		
		if (parser.parseOperandIndirect(cs)) {
			indirect = true;
		}
		
		if (indirect) {
			if (isLawOrLwc()) {
				parser.error("Indirect mode is not compatible with " + this);
			} else {
				// Turn on the indirect bit
				combined |= 0100000;
			}
		}
		
		Expression exp = Expression.parse(parser, cs);
		if (isLawOrLwc()) {
			parser.generateWord(
				Expression.makeMaskedInstruction(combined, 03777, exp));
		} else {
			parser.generateWord(
				Expression.makeAddr11Instruction(
					parser.getCurrentLocation(),
					combined,
					exp));
		}
		ok = true;
		
		return ok;
	}
	
	public int getInstruction() {
		return combined;
	}
}
