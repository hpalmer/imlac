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
 * This class represents Imlac skip instructions.  These instructions check
 * for various conditions and skip the next instruction when one of the
 * conditions are met.  They may be combined to skip on several conditions,
 * provided the conditions are "compatible" and the high order bits of the
 * instructions are the same.
 * 
 * @author Howard Palmer
 * @version $Id$
 * @see Mnemonic
 */
public class SkipInstruction extends Mnemonic {

	/**
	 * While <code>skips</code> itself is never used, its elements are added to
	 * a hashtable in <code>Mnemonic</code> as they are created.
	 */
	@SuppressWarnings("unused")
	private static final SkipInstruction[] skips = {
		new SkipInstruction("ASZ", 0002001), // C(AC)=0
		new SkipInstruction("ASP", 0002002), // C(AC)+
		new SkipInstruction("LSZ", 0002004), // C(L)=0
		new SkipInstruction("DSF", 0002010), // Display is on
		new SkipInstruction("KSF", 0002020), // Keyboard on flag.
		new SkipInstruction("RSF", 0002040), // TTY has input data.
		new SkipInstruction("TSF", 0002100), // TTY done sending.
		new SkipInstruction("SSF", 0002200), // 40 ~ sync. on.
		new SkipInstruction("HSF", 0002400), // PTR has data
		new SkipInstruction("ASN", 0102001), // C(AC)!=0
		new SkipInstruction("ASM", 0102002), // C(AC)-
		new SkipInstruction("LSN", 0102004), // C(L)=1
		new SkipInstruction("DSN", 0102010), // Display is off
		new SkipInstruction("KSN", 0102020), // Keyboard on no flag.
		new SkipInstruction("RSN", 0102040), // TTY has no input.
		new SkipInstruction("TSN", 0102100), // TTY not done sending.
		new SkipInstruction("SSN", 0102200), // 40 ~ sync. off.
		new SkipInstruction("HSN", 0102400), // PTR has no data.
		
	};
	
	private final int instruction;
	private int combined;
	
	/**
	 * @param name
	 */
	protected SkipInstruction(String name, int instruction) {
		super(name);
		this.instruction = instruction;
	}

	public boolean parse(Parser parser, CharSource cs) {
		boolean ok = false;
		combined = instruction;
		// TODO:
		// Support combining these
		parser.generateWord(combined);
		ok = true;
		return ok;
	}
	
	public int getInstruction() {
		return combined;
	}
}
