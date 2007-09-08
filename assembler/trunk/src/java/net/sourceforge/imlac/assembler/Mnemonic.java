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

import java.util.HashMap;

/**
 * This class represents a mnemonic code in Imlac assembly language.  The
 * mnemonic may specify a machine opcode or a pseudo-operation, which is
 * a directive to the assembler.  It includes pseudo-ops from several
 * different dialects of Imlac assembly language.
 * 
 * This class includes class functions to access the mnemonic symbol table.
 * 
 * @author Howard Palmer
 * @version $Id$
 */
public abstract class Mnemonic {
	
	private static final HashMap<String, Mnemonic> table = new HashMap<String, Mnemonic>(1023);
	
	private final String name;
	
	public static void init() {
		@SuppressWarnings("unused") Class<?> c;
		// Reference these subclasses so that they build their mnemonics
		c = DisplayOperateInstruction.class;
		c = DisplayOrder.class;
		c = IOTInstruction.class;
		c = OperateInstruction.class;
		c = ProcessorOrder.class;
		c = PseudoOperation.class;
		c = ShiftInstruction.class;
		c = SkipInstruction.class;
		System.out.println("Hashed " + table.size() + " mnemonics");
	}
	
	public static Mnemonic lookup(String name) {
		Mnemonic m = table.get(name);
		return m;
	}
	
	protected Mnemonic(String name) {
		this.name = name;
		if (table.put(name, this) != null) {
			throw new RuntimeException("Duplicate Mnemonic");
		} 
	}
	
	// This should be overridden by subclasses other than PseudoOperation
	public int getInstruction() {
		return -1;
	}
	
	public boolean equals(Object obj) {
		boolean result = false;
		if (obj.getClass() == Mnemonic.class) {
			Mnemonic m = (Mnemonic) obj;
			result = (name.compareTo(m.name) == 0);
		}
		return result;
	}
	
	public int hashCode() {
		return name.hashCode();
	}
		
	public String toString() {
		return name;
	}
	
	public abstract boolean parse(Parser parser, CharSource cs);
}
