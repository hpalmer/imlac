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
package net.sourceforge.imlac.misc;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;

/**
 * This is an ad hoc program that was used to extract a vector font from an Imlac
 * binary file.
 * 
 * @author Howard Palmer
 * @version $Id$
 */
public class FourBit {

	public static HashMap<Integer, String> labels = new HashMap<Integer, String>(513);
	public static PrintStream asmOut;
	public static int chCode = 040;
	public static boolean inIncMode = false;
	
	/**
	 * 
	 */
	public FourBit() {
		super();
	}

	public static String newLabel(int location) {
		Integer loc = new Integer(location);
		String name = "C" + Integer.toOctalString(chCode);
		labels.put(loc, name);
		++chCode;
		return name;
	}
	
	public static String decodeIncByte(int value, boolean lowByte) {
		String result = null;
		value &= 0377;
		if (!inIncMode) {
			if (value == 060) {
				result = "E";
				inIncMode = true;
			}
		}
		if (result == null) {
			if (inIncMode || lowByte) {
				if ((value & 0200) == 0) {
					switch (value) {
						case 0111:
							result = "N";
							break;
						case 0151:
							result = "R";
							break;
						case 0171:
							result = "F";
							break;
						case 0200:
							result = "P";
							break;
						case 0140:
							result = "X";
							break;
						case 0100:
							result = "T";
							break;
						default:
							result = Integer.toOctalString(value) + "'";
							break;
					}
					if ((value & 0100) != 0) {
						inIncMode = false;
					}
				} else {
					StringBuffer tag = new StringBuffer(8);
					tag.append(((value & 0100) == 0) ? 'D' : 'B');
					if ((value & 040) != 0) {
						tag.append('M');
					}
					tag.append(Integer.toOctalString((value >> 3) & 3));
					if ((value & 04) != 0) {
						tag.append('M');
					}
					tag.append(Integer.toOctalString(value & 3));
					result = tag.toString();
				}
			} else {
				result = "Byte " + Integer.toOctalString(value);
			}
		}
		return result;
	}
	
	public static void decodeINC(int location, int word) {
		if (location == 03146) {
			asmOut.println("\tLOC\t" + Integer.toOctalString(location) + "'");
		}
		String name = "";
		String comment = "";
		Integer loc = new Integer(location);
		if (labels.containsKey(loc)) {
			name = labels.get(loc) + ":";
			char ch = (char) Integer.parseInt(name.substring(1, name.length()-1), 8);
			comment = "\t\t; '" + ch + "'";
		}
		asmOut.println(
			name
				+ "\tINC\t"
				+ decodeIncByte(word >> 8, false)
				+ ","
				+ decodeIncByte(word, true)
				+ comment);
	}
	public static void decodeDJMS(int location, int word) {
		if (location == 02410) {
			asmOut.println("\tLOC\t" + Integer.toOctalString(location) + "'");
		}
		if ((word ^ 050000) < 010000) {
			asmOut.println("\tDJMS\t" + newLabel(word & 07777));
		} else {
			asmOut.println("\tDATA\t" + Integer.toOctalString(word) + "'");
		}
	}
	
	public static void main(String[] args) {
		if (args.length <= 0) {
			System.exit(-1);
		}
		String filename = args[0];
		try {
			FileInputStream in4 = new FileInputStream(filename);
			FileOutputStream out = new FileOutputStream("ssv.asm");
			asmOut = new PrintStream(out);
			byte[] buf = new byte[64*1024];
			int offset = 0;
			int len;
			do {
				len = in4.read(buf, offset, buf.length - offset);
				if (len > 0) {
					offset += len;
				}
			} while (len > 0);
			System.out.println("Read " + offset + " bytes.");
			int j = 0;
			int word = 0;
			int blockLen = 0101;
			int loadAddr = 017700;
			int cksm = 0;
			boolean getLoadAddr = false;
			boolean getChecksum = false;
			boolean firstBlock = true;
			for (int i = 0; i < offset; ++i) {
				if (((int)buf[i] & 0160) == 0100) {
					word = (word << 4) | (buf[i] & 017);
					++j;
					if ((j == 2) && (blockLen == 0)) {
						System.out.println("New block length=" + word);
						blockLen = word;
						getLoadAddr = true;
						j = 0;
						word = 0;
					}
					if (j == 4) {
						if (getLoadAddr) {
							loadAddr = word;
							System.out.println("Load address=" + Integer.toOctalString(word));
							getLoadAddr = false;
						} else if (getChecksum) {
							if (((cksm - word) & 0177777) == 0) {
								System.out.println("Checksum ok");
							} else {
								System.out.println("Checksum bad.");
							}
							getChecksum = false;
							cksm = 0;
							blockLen = 0;
						} else {
							cksm += word;
							if ((cksm & 0200000) != 0) ++cksm;
							cksm &= 0177777;
							System.out.println(
								Integer.toOctalString(loadAddr)
									+ "  "
									+ Integer.toOctalString(word));
							if ((loadAddr >= 02410) && (loadAddr <= 02547)) {
								decodeDJMS(loadAddr, word);
							} else if ((loadAddr >= 03146) && (loadAddr <= 04632)) {
								decodeINC(loadAddr, word);
							}
							++loadAddr;
							if (--blockLen == 0) {
								blockLen = 0;
								getChecksum = !firstBlock;
								if (getChecksum) ++blockLen;
								firstBlock = false;
							}
						}
						j = 0;
						word = 0;
					}
				}
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex.toString());
		}
	}
}
