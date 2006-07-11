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
 * This class represents an Imlac main processor IOT instruction.  These instructions
 * require no operand, and are typically used for I/O.  Some of these instructions
 * are combinations of others, e.g. RRC is RRB|RCF, but the assembler currently
 * allows only IOT mnemonic per instruction.
 * 
 * @author Howard Palmer
 * @version $Id$
 * @see Mnemonic
 */
public class IOTInstruction extends Mnemonic {

	/**
	 * While <code>iots</code> itself is never used, its elements are added to
	 * a hashtable in <code>Mnemonic</code> as they are created.
	 */
	@SuppressWarnings("unused")
	private static final IOTInstruction[] iots = {
		new IOTInstruction("DLZ",  0001001), // 0=>C(DPC)
//		new IOTInstruction(" - ", 0001002), // C(AC) OR C(DPC)=>C(DPC)
		new IOTInstruction("DLA", 0001003), // C(AC)=>AC(DPC)
		new IOTInstruction("CTB", 0001011), // CLear TTY Break
		new IOTInstruction("DOF", 0001012), // Turn Display Processor Off
		new IOTInstruction("KRB", 0001021), // Keyboard Read
		new IOTInstruction("KCF", 0001022), // Keyboard clear flag
		new IOTInstruction("KRC", 0001023), // KRC & KCF
		new IOTInstruction("RRB", 0001031), // TTY read
		new IOTInstruction("RCF", 0001032), // Clear input TTY
		new IOTInstruction("RRC", 0001033), // RRB & RCF
		new IOTInstruction("TPR", 0001041), // TTY transmit
		new IOTInstruction("TCF", 0001042), // Clear TTY Output Status
		new IOTInstruction("TPC", 0001043), // TTY print and clear flag
		new IOTInstruction("HRB", 0001051), // Read PTR
		new IOTInstruction("HOF", 0001052), // Stop PTR
		new IOTInstruction("HON", 0001061), // Start PTR
		new IOTInstruction("STB", 0001062), // Set TTY Break
		new IOTInstruction("SCF", 0001071), // Clear 40 Sync.
		new IOTInstruction("IOS", 0001072), // IOT Sync.
		
//		new IOTInstruction("IOT\t101", 0001101), // Read Interrupt Status into Accumulator
//		new IOTInstruction("IOT\t111", 0001111), // Protect Code As Per Accumulator
//		new IOTInstruction("IOT\t131", 0001131), // Read Light Pen Register
//		new IOTInstruction("IOT\t132", 0001132), // Clear Light Pen Status
//		new IOTInstruction("IOT\t134", 0001134), // Skip if Light Pen Status = 1
//		new IOTInstruction("IOT\t141", 0001141), // Arm and Disarm Devices

		new IOTInstruction("IOF", 0001161), // Disable Interrupt (optional)
		new IOTInstruction("ION", 0001162), // Enable Interrupt (optional)
		new IOTInstruction("PPC", 0001271), // Punch AC and Clear punch flag (optional)
		new IOTInstruction("PSF", 0001274), // Skip if Punch Ready (optional)
		new IOTInstruction("BEL", 0001321), // Control Audible Tone per AC bit 15 (BEL-1)
		new IOTInstruction("MSW", 0001331), // Read Mouse Switches and Keyset (GMI-1)
		
		// DON acts like an IOT, even though it's coded like shift instructions
		new IOTInstruction("DON", 0003100), // Turn Display Processor ON
		
	};
	
	private final int instruction;
	
	/**
	 * @param name the mnemonic string
	 */
	protected IOTInstruction(String name, int instruction) {
		super(name);
		this.instruction = instruction;
	}

	public boolean parse(Parser parser, CharSource cs) {
		boolean ok = false;
		String s = parser.getLeadingIndirectTag();
		if (s != null) {
			parser.error("Indirect is not compatible with " + this);
		}
		parser.generateWord(instruction);
		ok = true;
		
		return ok;
	}
	
	public int getInstruction() {
		return instruction;
	}
}
