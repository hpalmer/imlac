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
 * This class represent Imlac assembler pseudo-operations.
 * 
 * @author Howard Palmer
 * @version $Id$
 * @see Mnemonic
 */
public class PseudoOperation extends Mnemonic {

	private static int BLOCK = 0;
	private static int BSS = 1;
	private static int CONSTANTS = 2;
	private static int DADR = 3;
	private static int DATA = 4;
	private static int DLV = 5;
	private static int DUP = 6;
	private static int END = 7;
	private static int EQU = 8;
	private static int IF1 = 9;
	private static int IFE = 10;
	private static int IFN = 11;
	private static int INC = 12;
	private static int INS = 13;
	private static int LTORG = 14;
	private static int LOC = 15;
	private static int LVI = 16;
	private static int MCC = 17;
	private static int MCD = 18;
	private static int MCE = 19;
	private static int OCT = 20;
	private static int ORG = 21;
	private static int REL = 22;
	private static int REP = 23;
	private static int REPEAT = 24;
	private static int SVORG = 25;
	private static int TITLE = 26;
	private static int ZRO = 27;
	private static int ZZZ = 28;
	
	/**
	 * While <code>psops</code> itself is never used, its elements are added to
	 * a hashtable in <code>Mnemonic</code> as they are created.
	 */
	// These need not be in alphabetical order.
	@SuppressWarnings("unused")
	private static final PseudoOperation[] psops = {
		new PseudoOperation("BLOCK", BLOCK),		// Same as BSS
		new PseudoOperation("BSS", BSS),			// Reserve a block of memory
		new PseudoOperation("CONSTANTS", CONSTANTS),	// Same as LTORG
		new PseudoOperation("DADR", DADR),			// DSTB .ADDR. ?
		new PseudoOperation("DATA", DATA),			// Data
		new PseudoOperation("DLV", DLV),			// Generate Long Vector
		new PseudoOperation("DUP", DUP),			// Imlac Duplicate Macro Operation
		new PseudoOperation("END", END),			// End of Program
		new PseudoOperation("EQU", EQU),			// Equate Symbol
		new PseudoOperation("IF1", IF1),			// Conditional: If First Pass
		new PseudoOperation("IFE", IFE),			// Conditional: If Equal Zero
		new PseudoOperation("IFN", IFN),			// Conditional: If Not Equal Zero
		new PseudoOperation("INC", INC),			// Generate Incremental Mode Bytes
		new PseudoOperation("INS", INS),			// Imlac Insert Instruction in Macro 
		new PseudoOperation("LTORG", LTORG),		// Literal Origin
		new PseudoOperation("LOC", LOC),			// Same as ORG
		new PseudoOperation("LVI", LVI),			// Generate Long Vector
		new PseudoOperation("MCC", MCC),			// Imlac Macro Call
		new PseudoOperation("MCD", MCD),			// Imlac Macro Definition Start
		new PseudoOperation("MCE", MCE),			// Imlac Macro Definition End
		new PseudoOperation("OCT", OCT),
		new PseudoOperation("ORG", ORG),			// Specify Starting Location
		new PseudoOperation("REL", REL),
		new PseudoOperation("REP", REP),			// Repeat Previous Instruction 
		new PseudoOperation("REPEAT", REPEAT),		// Repeat a Statement
		new PseudoOperation("SVORG", SVORG),		// Save Origin
		new PseudoOperation("TITLE", TITLE),		// Specify Title for Listing
		new PseudoOperation("ZRO", ZRO),			// Like DATA
		new PseudoOperation("ZZZ", 0),				// Like DATA 0
		
	};
	
	private static final Pattern midasIncPat =
		Pattern.compile("(([ENRFPTX])|(([BD])(M?)([0-3])(M?)([0-3])))");
		
	private final int which;
	
	/**
	 * @param name
	 */
	protected PseudoOperation(String name, int which) {
		super(name);
		this.which = which;
	}

	private Integer parseIncMidas(Parser parser, CharSource cs) {
		Integer result = new Integer(0);
		byte[] val = new byte[2];
		val[0] = 0;
		val[1] = 0;
		
		for (int i = 0; i < 2; ++i) {
			if (i != 0) {
				cs.skipBlanks();
				char ch = cs.peekChar();
				if (ch != ',') {
					parser.error("Missing second byte of INC");
					break;
				}
				cs.advance(1);
			}

			if (Expression.parseNumber(parser.getStyle(), cs)) {
				int byteVal = Expression.getLastNumber();
				if (byteVal > 0377) {
					parser.error("Warning: value too large for INC byte");
				}
				val[i] = (byte) byteVal;
			} else {
				Matcher matcher = midasIncPat.matcher(cs);
				if (matcher.lookingAt()) {
					String s = matcher.group(2);
					if (s != null) {
						char ch = s.charAt(0);
						switch (ch) {
							case 'E' :
								if (i != 0) {
									parser.error(
										"'E' is not allowed in the second operand of INC");
								} else {
									val[0] = 060;
								}
								break;
							case 'N' :
								val[i] = 0111;
								break;
							case 'R' :
								val[i] = 0151;
								break;
							case 'F' :
								val[i] = 0171;
								break;
							case 'P' :
								val[i] = (byte) 0200;
								break;
							case 'X':
								val[i] = 0140;   // TODO: is this right?
								break;
							case 'T':
								val[i] = 0100;	// TODO: is this right?
								break;
						}
					} else {
						val[i] = (byte) 0200;
						s = matcher.group(4);
						if (s.charAt(0) == 'B') {
							val[i] |= 0100;
						}
						s = matcher.group(5);
						if ((s != null) && (s.length() != 0)) {
							val[i] |= 040;
						}
						s = matcher.group(6);
						int dx = Integer.parseInt(s);
						val[i] |= (dx << 3);
						s = matcher.group(7);
						if ((s != null) && (s.length() != 0)) {
							val[i] |= 04;
						}
						s = matcher.group(8);
						int dy = Integer.parseInt(s);
						val[i] |= dy;
					}
					cs.advance(matcher.end(1));
				}
			}
		}
		int word = ((int) val[0] << 8) | ((int) val[1] & 0377);
		result = new Integer(word);
		
		return result;
	}
	
	private Integer parseInc(Parser parser, CharSource cs) {
		Integer result = new Integer(0);
		if (!cs.skipBlanks()) {
			parser.error("Missing operands for INC");
			parser.generateWord(0);
			return null;
		}
		
		Asm asm = parser.getAsm();
		
		if (asm.isStyleMidas()) {
			return parseIncMidas(parser, cs);
		}
		
		parser.error("INC is not implemented for this style yet");
		return result;
	}
	
	private boolean parseLvMidas(Parser parser, CharSource cs) {
		boolean result = false;
	
		if (cs.skipBlanks()) {
			char ch = cs.peekChar();
			int word0 = 0040000;
			int word1 = 0;
			int word2 = 0;
			int xVal, yVal;
			
			if (ch == 'B') {
				// Beam on
				word1 |= 0020000;
			} else if (ch == 'X') {
				// Dotted
				word1 |= 0040000;
			} else if (ch != 'D'){
				parser.error("Beam indicator '" + ch + "' is invalid in " + this);
			}
			cs.advance(1);
			cs.skipBlanks();
			ch = cs.peekChar();
			if (ch != ',') {
				parser.error("Found '" + ch + "' where comma was expected");
				return false;
			}
			cs.advance(1);
			cs.skipBlanks();
			
			xVal = 0;
			Integer ibox = null;
			Expression exp = Expression.parse(parser, cs);
			if (exp != null) {
				if (!exp.hasSymRef()) {
					ibox = exp.evaluate(parser);
					if (ibox != null) {
						xVal = ibox.intValue();
					}
				}
			}
			if (ibox == null) {
				parser.error("Unable to parse X movement in " + this);
			}
			
			ch = cs.peekChar();
			if (ch != ',') {
				parser.error("Found '" + ch + "' where comma was expected");
				return false;
			}
			cs.advance(1);
			cs.skipBlanks();
			
			yVal = 0;
			ibox = null;
			exp = Expression.parse(parser, cs);
			if (exp != null) {
				if (!exp.hasSymRef()) {
					ibox = exp.evaluate(parser);
					if (ibox != null) {
						yVal = ibox.intValue();
					}
				}
			}
			if (ibox == null) {
				parser.error("Unable to parse Y movement in " + this);
			}
			
			if (xVal < 0) {
				// Negative X
				word2 |= 0040000;
				xVal = -xVal;
			}
			if (yVal < 0) {
				// Negative Y
				word2 |= 0020000;
				yVal = -yVal;
			}
			int diff = yVal - xVal;
			if (yVal > xVal) {
				word2 |= 0010000;
				diff = -diff;
				int tmp = xVal;
				xVal = yVal;
				yVal = tmp;
			}
			word0 |= (diff & 07777);
			word1 |= (xVal & 07777);
			word2 |= (yVal & 07777);
			parser.generateWord(word0);
			parser.generateWord(word1);
			parser.generateWord(word2);
			result = true;
		} else {
			parser.error("Missing operands for " + this);
		}
		return result;
	}
	
	public boolean parse(Parser parser, CharSource cs) {
		boolean ok = false;
		String s = this.toString();
		
		if (s.equals("INC")) {
			Integer inc = parseInc(parser, cs);
			if (inc != null) {
				parser.generateWord(inc.intValue());
			}
			return true;
		} else if (s.equals("DLV")) {
			ok = parseLvMidas(parser, cs);
			if (!ok) {
				// If the operand doesn't parse, generate three zero words,
				// just to put the location counter where it would have been.
				parser.generateWord(0);
				parser.generateWord(0);
				parser.generateWord(0);
			}
		} else if (s.equals("DATA") || s.equals("ZRO")) {
			if (!cs.skipBlanks()) {
				parser.error("Failed to find operand for " + this);
				parser.generateWord(0);
			} else {
				Expression exp = Expression.parse(parser, cs);
				if (exp != null) {
					parser.generateWord(exp);
				} else {
					parser.error("Syntax error in operand of " + this);
					parser.generateWord(0);
				}
			}
			ok = true;
		} else if (s.equals("ZZZ")) {
			cs.skipBlanks();
			if (cs.peekChar() == '*') {
				cs.advance(1);
				if (cs.peekChar() == '*') {
					cs.advance(1);
				}
			}
			parser.generateWord(0);
			ok = true;
		} else if (s.equals("BLOCK") || s.equals("BSS") || s.equals("LOC") ||
			s.equals("ORG") || s.equals("REL")) {
			cs.skipBlanks();
			Expression exp = Expression.parse(parser, cs);
			if (exp != null) {
				Integer ibox = exp.evaluate(parser);
				if (ibox != null) {
					int value = ibox.intValue();
					
					if (s.charAt(0) == 'B') {
						value += parser.getCurrentLocation();
					}
					parser.setCurrentLocation(value);
				} else {
					parser.error(s + " ignored");
				}
			}
			ok = true;
		} else if (s.equals("CONSTANTS") || s.equals("LTORG")) {
			parser.generateLiterals();
			ok = true;
		} else if (s.equals("END")) {
			// TODO:
			// Get starting address from END operand
			Expression exp = null;
			if (cs.skipBlanks()) {
				exp = Expression.parse(parser, cs);
			}
			parser.end(exp);
			ok = true;
		} else {
			parser.error("Unimplemented pseudo-op " + s);
		}
		
		return ok;
	}
}
