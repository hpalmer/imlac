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
 * This class is used to represent Imlac main processor instructions in
 * the operate class.  These instructions do not take any operands, and
 * may be combined to do several operations with one instruction.
 * 
 * @author Howard Palmer
 * @version $Id$
 * @see Mnemonic
 */
public class OperateInstruction extends Mnemonic {

	/**
	 * While <code>ops</code> itself is never used, its elements are added to
	 * a hashtable in <code>Mnemonic</code> as they are created.
	 */
	@SuppressWarnings("unused")
	private static final OperateInstruction[] ops = {
		new OperateInstruction("HLT", 0000000),		// HALT
		new OperateInstruction("NOP", 0100000),		// NO OPERATION
		new OperateInstruction("CLA", 0100001),		// CLEAR AC
		new OperateInstruction("CMA", 0100002),		// 1'S COMP AC
		new OperateInstruction("STA", 0100003),		// CLA & COMP
		new OperateInstruction("IAC", 0100004),		// INC. AC
		new OperateInstruction("COA", 0100005),		// +1=>C(AC)
		new OperateInstruction("CIA", 0100006),		// 2'S COMP AC
		new OperateInstruction("CLL", 0100010),		// CLEAR LINK
		new OperateInstruction("CAL", 0100011),		// CLA & CLL
		new OperateInstruction("CML", 0100020),		// COMPLEMENT LINK
		new OperateInstruction("STL", 0100030),		// SET LINK
		new OperateInstruction("ODA", 0100040),		// IOR AC with DS
		new OperateInstruction("LDA", 0100041),		// C(DS)=>C(AC)
	};
	
	private final int instruction;
	private int combined;
	
	protected OperateInstruction(String name, int instruction) {
		super(name);
		this.instruction = instruction;
	}
	
	/**
	 * This function is called with the character source positioned just
	 * after the mnemonic.  For this instruction class, no operand is
	 * expected, but multiple operate instructions may be combined via
	 * an inclusive OR function, e.g.  CLA\STL or CLA|STL.
	 * 
	 * @param parser current Parser instance
	 * @param cs current character source
	 * @return true if mnemonic parsing completes successfully, with
	 * the character source advanced to the end of the statement
	 */
	public boolean parse(Parser parser, CharSource cs) {
		boolean ok = true;
		
		// Initialize combined instruction value
		combined = instruction;
		
		// Don't allow multiple mnemonics for HLT
		if (instruction != 0) {
			while (true) {
				char ch = cs.peekChar();
				if ((ch == '\\') || (ch == '|')) {
					cs.advance(1);
					if (!parser.parseMnemonic(cs)) {
						parser.error("Missing mnemonic after '" + ch + "'");
						break;						
					}
					Mnemonic m = parser.getLastMnemonic();
					if (!(m instanceof OperateInstruction)) {
						parser.error("Cannot combine " + m + " with " + this);
						break;
					}
					OperateInstruction oi = (OperateInstruction) m;
					if (oi.instruction == 0) {
						parser.error("Combining HLT is not supported");
						break;
					}
					combined |= oi.instruction;
				} else {
					ok = true;
					break;
				}
			}
		}
		parser.generateWord(combined);
		
		return ok;
	}
	
	public int getInstruction() {
		return combined;
	}
}
